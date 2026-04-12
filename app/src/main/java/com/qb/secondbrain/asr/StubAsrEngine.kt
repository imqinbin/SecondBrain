package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubAsrEngine @Inject constructor() : AsrEngine {

    override suspend fun recognize(audioFile: File): Result<String> {
        // Stub implementation - will be replaced by real ASR (e.g., Baidu, Google)
        return Result.success("")
    }
}
