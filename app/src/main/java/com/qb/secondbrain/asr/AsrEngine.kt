package com.qb.secondbrain.asr

import java.io.File

interface AsrEngine {
    suspend fun recognize(audioFile: File): Result<String>
}
