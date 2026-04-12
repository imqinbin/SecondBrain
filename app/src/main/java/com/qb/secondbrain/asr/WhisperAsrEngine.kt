package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class WhisperAsrEngine @Inject constructor() : AsrEngine {
    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置 Whisper 模型"))
    }
}
