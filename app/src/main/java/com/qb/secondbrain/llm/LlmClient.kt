package com.qb.secondbrain.llm

import com.qb.secondbrain.data.model.LlmIntent

interface LlmClient {
    suspend fun parseIntent(text: String, context: String? = null): Result<LlmIntent>
}
