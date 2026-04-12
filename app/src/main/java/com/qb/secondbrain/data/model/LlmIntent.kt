package com.qb.secondbrain.data.model

data class LlmIntent(
    val intent: String,
    val needContext: ContextNeed = ContextNeed(),
    val content: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: String? = null,
    val queryKeywords: List<String> = emptyList()
)

data class ContextNeed(
    val screenshot: Boolean = false,
    val location: Boolean = false
)