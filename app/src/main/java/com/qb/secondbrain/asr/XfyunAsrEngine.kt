package com.qb.secondbrain.asr

import android.content.Context
import android.util.Log
import com.iflytek.aikit.core.AiAudio
import com.iflytek.aikit.core.AiHandle
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiStatus
import com.qb.secondbrain.BuildConfig
import com.qb.secondbrain.SecondBrainApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class XfyunAsrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : AsrEngine {

    companion object {
        private const val TAG = "XfyunAsrEngine"
        private const val ABILITY_ID = "e75f07b62"
        private const val BUFFER_SIZE = 1280
    }

    @Volatile
    private var engineInit = false
    private var listenerRegistered = false
    private val engineLock = Any()

    private val pendingResults = ConcurrentLinkedQueue<SessionResult>()
    private var currentHandleId: Int = -1

    private class SessionResult(
        val fullText: StringBuilder = StringBuilder(),
        val finished: AtomicBoolean = AtomicBoolean(false),
        val latch: CountDownLatch = CountDownLatch(1)
    )

    private val globalListener = object : AiListener {
        override fun onResult(
            handleID: Int,
            outputData: MutableList<AiResponse>?,
            usrContext: Any?
        ) {
            if (outputData == null) return
            val result = pendingResults.peek() ?: return

            for (response in outputData) {
                val key = response.key
                val bytes = response.value
                try {
                    val text = String(bytes, charset("GBK"))
                    Log.d(TAG, "$key: $text")
                    if (key.contains("plain")) {
                        result.fullText.append(text.trim())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse result error", e)
                }
            }

            if (outputData.isNotEmpty() && outputData[0].status == 2) {
                result.finished.set(true)
                result.latch.countDown()
            }
        }

        override fun onEvent(
            handleID: Int,
            event: Int,
            outputData: MutableList<AiResponse>?,
            usrContext: Any?
        ) {
            Log.d(TAG, "onEvent: handleID=$handleID, event=$event")
        }

        override fun onError(
            handleID: Int,
            errCode: Int,
            errMsg: String?,
            usrContext: Any?
        ) {
            Log.e(TAG, "onError: $errMsg ($errCode)")
            val result = pendingResults.peek()
            result?.finished?.set(true)
            result?.latch?.countDown()
        }
    }

    private fun ensureReady() {
        if (!SecondBrainApp.awaitSdkInit()) {
            throw IllegalStateException("讯飞 SDK 初始化失败或超时")
        }
        synchronized(engineLock) {
            if (!listenerRegistered) {
                AiHelper.getInst().registerListener(ABILITY_ID, globalListener)
                listenerRegistered = true
            }
            if (!engineInit) {
                val builder = AiRequest.builder()
                builder.param("decNetType", "fsa")
                builder.param("punishCoefficient", 0.0)
                builder.param("wfst_addType", 0)
                val ret = AiHelper.getInst().engineInit(ABILITY_ID, builder.build())
                if (ret != 0) {
                    throw IllegalStateException("ESR engineInit 失败: $ret")
                }
                engineInit = true
            }
        }
    }

    override suspend fun recognize(audioFile: File): Result<String> {
        if (!audioFile.exists()) {
            return Result.failure(IllegalArgumentException("Audio file not found: ${audioFile.absolutePath}"))
        }
        if (BuildConfig.XFYUN_APP_ID.isBlank()) {
            return Result.failure(IllegalStateException("请先在 local.properties 中配置讯飞 SDK 凭据"))
        }

        return try {
            val text = doRecognize(audioFile)
            if (text.isBlank()) {
                Result.failure(IllegalStateException("语音识别结果为空"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun doRecognize(audioFile: File): String =
        suspendCancellableCoroutine { continuation ->
            ensureReady()

            val sessionResult = SessionResult()
            pendingResults.add(sessionResult)

            val paramBuilder = AiRequest.builder()
            paramBuilder.param("languageType", 0)
            paramBuilder.param("vadEndGap", 60)
            paramBuilder.param("vadOn", true)
            paramBuilder.param("beamThreshold", 20)
            paramBuilder.param("hisGramThreshold", 3000)
            paramBuilder.param("vadLinkOn", false)
            paramBuilder.param("vadSpeechEnd", 80)
            paramBuilder.param("vadResponsetime", 1000)
            paramBuilder.param("postprocOn", false)

            val handle: AiHandle
            synchronized(engineLock) {
                handle = AiHelper.getInst().start(ABILITY_ID, paramBuilder.build(), null)
            }
            if (handle.code != 0) {
                pendingResults.poll()
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        IllegalStateException("无法启动识别会话: ${handle.code}")
                    )
                }
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                AiHelper.getInst().end(handle)
                pendingResults.poll()
            }

            Thread {
                try {
                    FileInputStream(audioFile).use { fis ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var isFirst = true

                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            if (sessionResult.finished.get()) break
                            val data = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
                            val status = when {
                                isFirst -> { isFirst = false; AiStatus.BEGIN }
                                else -> AiStatus.CONTINUE
                            }

                            val dataBuilder = AiRequest.builder()
                            val aiAudio = AiAudio.get("audio").data(data).status(status).valid()
                            dataBuilder.payload(aiAudio)
                            AiHelper.getInst().write(dataBuilder.build(), handle)
                            AiHelper.getInst().read(ABILITY_ID, handle)
                            Thread.sleep(40)
                        }

                        if (!sessionResult.finished.get()) {
                            val endBuilder = AiRequest.builder()
                            val endAudio = AiAudio.get("audio").data(ByteArray(0)).status(AiStatus.END).valid()
                            endBuilder.payload(endAudio)
                            AiHelper.getInst().write(endBuilder.build(), handle)
                            AiHelper.getInst().read(ABILITY_ID, handle)
                        }
                    }

                    sessionResult.latch.await()
                    AiHelper.getInst().end(handle)
                    pendingResults.poll()

                    if (continuation.isActive) {
                        continuation.resume(sessionResult.fullText.toString())
                    }
                } catch (e: Exception) {
                    AiHelper.getInst().end(handle)
                    pendingResults.poll()
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }.start()
        }
}
