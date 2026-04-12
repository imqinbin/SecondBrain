package com.qb.secondbrain.asr

import javax.inject.Inject
import javax.inject.Provider

class AsrEngineFactory @Inject constructor(
    private val xfyunProvider: Provider<XfyunAsrEngine>,
    private val baiduProvider: Provider<BaiduAsrEngine>,
    private val whisperProvider: Provider<WhisperAsrEngine>
) {
    fun getEngine(name: String): AsrEngine {
        return when (name.lowercase()) {
            "xfyun" -> xfyunProvider.get()
            "baidu" -> baiduProvider.get()
            "whisper" -> whisperProvider.get()
            else -> whisperProvider.get()
        }
    }
}
