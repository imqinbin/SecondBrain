package com.qb.secondbrain.service

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.qb.secondbrain.asr.AsrEngine
import com.qb.secondbrain.context.LocationProvider
import com.qb.secondbrain.context.ScreenCaptureProvider
import com.qb.secondbrain.data.model.ImagePath
import com.qb.secondbrain.data.model.ImageSource
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import com.qb.secondbrain.llm.LlmClient
import com.qb.secondbrain.llm.RuleBasedFallback
import com.qb.secondbrain.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VoiceMemoService : Service() {

    @Inject lateinit var asrEngine: AsrEngine
    @Inject lateinit var llmClient: LlmClient
    @Inject lateinit var ruleBasedFallback: RuleBasedFallback
    @Inject lateinit var memoRepository: MemoRepository
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var screenCaptureProvider: ScreenCaptureProvider
    @Inject lateinit var notificationHelper: NotificationHelper

    private val audioRecorder by lazy {
        AudioRecorder(cacheDir = cacheDir)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentState: VoiceMemoState = VoiceMemoState.Idle

    companion object {
        const val ACTION_TOGGLE_RECORDING = "com.qb.secondbrain.ACTION_TOGGLE_RECORDING"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_RECORDING -> {
                if (currentState is VoiceMemoState.Recording) {
                    handleStopRecording()
                } else {
                    handleStartRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.release()
        serviceScope.cancel()
    }

    private fun handleStartRecording() {
        if (currentState is VoiceMemoState.Recording) return

        // Check RECORD_AUDIO permission before starting FGS with microphone type
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            handleError("请先授予录音权限")
            Toast.makeText(this, "请先在设置中授予录音权限", Toast.LENGTH_LONG).show()
            return
        }

        currentState = VoiceMemoState.Recording
        val notification = notificationHelper.recordingNotification()
        startForeground(NOTIFICATION_ID, notification)

        val fileName = "voice_memo_${System.currentTimeMillis()}.pcm"
        audioRecorder.startRecording(fileName)
    }

    private fun handleStopRecording() {
        if (currentState !is VoiceMemoState.Recording) return

        currentState = VoiceMemoState.Processing("停止录音")
        serviceScope.launch {
            try {
                val audioFile = audioRecorder.stopRecording()
                if (audioFile != null) {
                    processAudio(audioFile)
                } else {
                    handleError("录音文件获取失败")
                }
            } catch (e: Exception) {
                handleError(e.message ?: "停止录音失败")
            }
        }
    }

    private suspend fun processAudio(file: File) {
        // Step 1: ASR - Speech to text
        currentState = VoiceMemoState.Processing("语音识别中")
        val asrResult = asrEngine.recognize(file)
        val rawText = asrResult.getOrNull()
        if (rawText.isNullOrBlank()) {
            val errorMsg = asrResult.exceptionOrNull()?.message ?: "语音识别失败"
            handleError(errorMsg)
            return
        }

        // Step 2: LLM - Parse intent with 10s timeout, fallback to rule-based
        currentState = VoiceMemoState.Processing("理解意图中")
        val llmResult = withTimeoutOrNull(10_000L) {
            llmClient.parseIntent(rawText, null)
        }

        var parsedIntent = if (llmResult != null) {
            llmResult.getOrNull()
        } else {
            null
        } ?: run {
            ruleBasedFallback.parseIntent(rawText)
        }

        // Step 3: Gather context if needed
        val contextParts = mutableListOf<String>()

        if (parsedIntent.needContext.screenshot) {
            currentState = VoiceMemoState.Processing("截取屏幕中")
            val screenshotPath = screenCaptureProvider.captureScreen()
            if (screenshotPath != null) {
                contextParts.add("屏幕内容: $screenshotPath")
            }
        }

        if (parsedIntent.needContext.location) {
            currentState = VoiceMemoState.Processing("获取位置中")
            val location = locationProvider.getCurrentLocation()
            if (location != null) {
                val address = locationProvider.getAddress(location.latitude, location.longitude)
                contextParts.add("位置: ${address ?: "${location.latitude},${location.longitude}"}")
            }
        }

        // Step 4: Re-call LLM with context if context was obtained
        if (contextParts.isNotEmpty()) {
            val contextInfo = contextParts.joinToString("\n")
            val refinedResult = withTimeoutOrNull(10_000L) {
                llmClient.parseIntent(rawText, contextInfo)
            }
            if (refinedResult != null) {
                refinedResult.getOrNull()?.let { parsedIntent = it }
            }
        }

        // Step 5: Execute intent
        currentState = VoiceMemoState.Processing("执行操作中")
        executeIntent(parsedIntent, rawText, contextParts)

        currentState = VoiceMemoState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun executeIntent(
        intent: com.qb.secondbrain.data.model.LlmIntent,
        rawText: String,
        contextParts: List<String>
    ) {
        when (intent.intent) {
            "add" -> {
                val location = if (contextParts.any { it.startsWith("位置:") }) {
                    val locationInfo = locationProvider.getCurrentLocation()
                    locationInfo?.let {
                        val addr = locationProvider.getAddress(it.latitude, it.longitude)
                        Triple(it.latitude, it.longitude, addr)
                    }
                } else {
                    null
                }

                val screenshotPaths = contextParts
                    .filter { it.startsWith("屏幕内容:") }
                    .map { it.removePrefix("屏幕内容: ") }
                    .map { ImagePath(it, ImageSource.VOICE_SCREENSHOT) }

                val reminderTime = intent.reminderTime?.let {
                    try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .parse(it)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

                val memo = Memo(
                    content = intent.content.ifBlank { rawText },
                    rawText = rawText,
                    tags = intent.tags,
                    imagePaths = screenshotPaths,
                    latitude = location?.first,
                    longitude = location?.second,
                    address = location?.third,
                    reminderTime = reminderTime
                )
                val memoId = memoRepository.addMemo(memo)
                currentState = VoiceMemoState.Notifying("已添加")
                val notification = notificationHelper.addResultNotification(
                    memoId,
                    intent.content.ifBlank { rawText }
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(
                    notificationHelper.getNotificationId(NotificationHelper.NotificationType.ADD_RESULT),
                    notification
                )
            }

            "query" -> {
                val keywords = intent.queryKeywords.ifEmpty {
                    intent.content.split(Regex("\\s+")).filter { it.isNotBlank() }
                }
                val results = memoRepository.searchByKeywords(keywords).first()
                val resultTexts = results.map { it.content }
                currentState = VoiceMemoState.Notifying(
                    if (results.isEmpty()) "未找到相关备忘" else "查询完成"
                )
                val notification = notificationHelper.queryResultNotification(
                    keywords,
                    resultTexts.ifEmpty { listOf("未找到相关备忘") }
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(
                    notificationHelper.getNotificationId(NotificationHelper.NotificationType.QUERY_RESULT),
                    notification
                )
            }

            "update" -> {
                val keywords = intent.queryKeywords.ifEmpty {
                    intent.content.split(Regex("\\s+")).filter { it.isNotBlank() }
                }
                val results = memoRepository.searchByKeywords(keywords).first()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (results.isNotEmpty()) {
                    val memoToUpdate = results.first()
                    val updatedMemo = memoToUpdate.copy(
                        content = intent.content,
                        tags = if (intent.tags.isNotEmpty()) intent.tags else memoToUpdate.tags,
                        updatedAt = System.currentTimeMillis()
                    )
                    memoRepository.updateMemo(updatedMemo)
                    currentState = VoiceMemoState.Notifying("已更新")
                    val notification = notificationHelper.updateResultNotification(
                        updatedMemo.id,
                        updatedMemo.content
                    )
                    notificationManager.notify(
                        notificationHelper.getNotificationId(NotificationHelper.NotificationType.UPDATE_RESULT),
                        notification
                    )
                } else {
                    currentState = VoiceMemoState.Notifying("未找到要更新的备忘")
                    val notification = notificationHelper.errorNotification("未找到要更新的备忘")
                    notificationManager.notify(
                        notificationHelper.getNotificationId(NotificationHelper.NotificationType.ERROR),
                        notification
                    )
                }
            }

            "delete" -> {
                val keywords = intent.queryKeywords.ifEmpty {
                    intent.content.split(Regex("\\s+")).filter { it.isNotBlank() }
                }
                val results = memoRepository.searchByKeywords(keywords).first()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (results.isNotEmpty()) {
                    val memoToDelete = results.first()
                    memoRepository.deleteMemo(memoToDelete.id)
                    currentState = VoiceMemoState.Notifying("已删除")
                    val notification = notificationHelper.deleteResultNotification(memoToDelete.content)
                    notificationManager.notify(
                        notificationHelper.getNotificationId(NotificationHelper.NotificationType.DELETE_RESULT),
                        notification
                    )
                } else {
                    currentState = VoiceMemoState.Notifying("未找到要删除的备忘")
                    val notification = notificationHelper.errorNotification("未找到要删除的备忘")
                    notificationManager.notify(
                        notificationHelper.getNotificationId(NotificationHelper.NotificationType.ERROR),
                        notification
                    )
                }
            }

            else -> {
                handleError("未知意图: ${intent.intent}")
            }
        }
    }

    private fun handleError(message: String) {
        currentState = VoiceMemoState.Error(message)
        val notification = notificationHelper.errorNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            notificationHelper.getNotificationId(NotificationHelper.NotificationType.ERROR),
            notification
        )
        stopSelf()
    }
}
