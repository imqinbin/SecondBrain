package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class XfyunAsrEngine @Inject constructor() : AsrEngine {
    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置科大讯飞 SDK"))
    }
}
