package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class BaiduAsrEngine @Inject constructor() : AsrEngine {
    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置百度语音 SDK"))
    }
}
