package com.qb.secondbrain.asr

import android.content.Context
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechRecognizer
import com.iflytek.cloud.util.ResourceUtil
import com.qb.secondbrain.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Singleton
class XfyunAsrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : AsrEngine {

    companion object {
        private const val TAG = "XfyunAsrEngine"
        private const val BUFFER_SIZE = 4800 // 16kHz * 16bit * 100ms
    }

    private val initListener = InitListener { code ->
        if (code != ErrorCode.SUCCESS) {
            android.util.Log.e(TAG, "Xfyun SDK init failed: $code")
        }
    }

    override suspend fun recognize(audioFile: File): Result<String> {
        if (!audioFile.exists()) {
            return Result.failure(IllegalArgumentException("Audio file not found: ${audioFile.absolutePath}"))
        }

        if (BuildConfig.XFYUN_APP_ID.isBlank()) {
            return Result.failure(IllegalStateException("请先在 local.properties 中配置 xfyun.appid"))
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
            val recognizer = SpeechRecognizer.createRecognizer(context, initListener)
            if (recognizer == null) {
                continuation.resumeWithException(IllegalStateException("无法创建讯飞语音识别器"))
                return@suspendCancellableCoroutine
            }

            // Configure for offline recognition
            recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL)
            recognizer.setParameter(SpeechConstant.RESULT_TYPE, "json")
            recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
            recognizer.setParameter(SpeechConstant.ACCENT, "mandarin")
            recognizer.setParameter(SpeechConstant.ASR_PTT, "1")
            recognizer.setParameter(SpeechConstant.VAD_BOS, "4000")
            recognizer.setParameter(SpeechConstant.VAD_EOS, "1000")

            // Set offline resource path
            recognizer.setParameter(SpeechConstant.ASR_RES_PATH, getResourcePath())

            // Use external audio source (we feed data ourselves)
            recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, "-1")

            val fullResult = StringBuilder()

            val listener = object : RecognizerListener {
                override fun onBeginOfSpeech() {}
                override fun onEndOfSpeech() {}

                override fun onResult(result: RecognizerResult?, isLast: Boolean) {
                    result?.let {
                        val text = parseResult(it.resultString)
                        fullResult.append(text)
                    }
                    if (isLast) {
                        recognizer.stopListening()
                        recognizer.destroy()
                        continuation.resume(fullResult.toString()) {}
                    }
                }

                override fun onError(error: SpeechError?) {
                    recognizer.destroy()
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(
                        IllegalStateException("讯飞识别错误: ${error?.errorDescription} (${error?.errorCode})")
                    )
                }

                override fun onVolumeChanged(volume: Int, data: ByteArray?) {}
                override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: android.os.Bundle?) {}
            }

            continuation.invokeOnCancellation {
                recognizer.cancel()
                recognizer.destroy()
            }

            // Start listening (external audio mode)
            recognizer.startListening(listener)

            // Feed audio data in chunks
            FileInputStream(audioFile).use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    recognizer.writeAudio(buffer, 0, bytesRead)
                    // Small delay to avoid buffer overflow
                    Thread.sleep(40)
                }
            }

            // Signal end of audio
            recognizer.stopListening()
        }

    private fun getResourcePath(): String {
        return ResourceUtil.generateResourcePath(
            context, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet"
        ) + ";" + ResourceUtil.generateResourcePath(
            context, ResourceUtil.RESOURCE_TYPE.assets, "iat/sms_16k.jet"
        )
    }

    private fun parseResult(json: String): String {
        return try {
            val jsonObject = JSONObject(json)
            val wsArray = jsonObject.optJSONArray("ws") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until wsArray.length()) {
                val ws = wsArray.getJSONObject(i)
                val cwArray = ws.getJSONArray("cw")
                for (j in 0 until cwArray.length()) {
                    val cw = cwArray.getJSONObject(j)
                    sb.append(cw.optString("w", ""))
                }
            }
            sb.toString()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Parse result error", e)
            ""
        }
    }
}
