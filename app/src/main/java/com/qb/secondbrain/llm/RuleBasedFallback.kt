package com.qb.secondbrain.llm

import com.qb.secondbrain.data.model.ContextNeed
import com.qb.secondbrain.data.model.LlmIntent
import javax.inject.Inject

class RuleBasedFallback @Inject constructor() {

    fun parseIntent(text: String): LlmIntent {
        val trimmed = text.trim()

        // "删除最后一条"
        if (trimmed.contains("删除") && (trimmed.contains("最后") || trimmed.contains("上一"))) {
            return LlmIntent(
                intent = "delete",
                content = trimmed,
                queryKeywords = listOf("最近")
            )
        }

        // "今天的备忘录" / "最近" / "昨天的"
        if (trimmed.contains("今天") || trimmed.contains("今日")) {
            return LlmIntent(
                intent = "query",
                content = trimmed,
                queryKeywords = listOf("今天")
            )
        }

        if (trimmed.contains("最近")) {
            return LlmIntent(
                intent = "query",
                content = trimmed,
                queryKeywords = listOf("最近")
            )
        }

        // "搜索XXX" / "查找XXX" / "找一下XXX"
        val searchPatterns = listOf("搜索", "查找", "搜一下", "找一下", "查一下", "查询")
        for (pattern in searchPatterns) {
            if (trimmed.contains(pattern)) {
                val keyword = trimmed.substringAfter(pattern).trim()
                if (keyword.isNotEmpty()) {
                    return LlmIntent(
                        intent = "query",
                        content = keyword,
                        queryKeywords = listOf(keyword)
                    )
                }
            }
        }

        // "修改XXX为YYY"
        if (trimmed.contains("修改") && trimmed.contains("为")) {
            val content = trimmed.substringAfter("修改").trim()
            val parts = content.split("为", limit = 2)
            if (parts.size == 2) {
                return LlmIntent(
                    intent = "update",
                    content = "将「${parts[0]}」修改为「${parts[1]}」",
                    queryKeywords = listOf(parts[0].trim())
                )
            }
            return LlmIntent(
                intent = "update",
                content = trimmed
            )
        }

        // "提醒我N点XXX"
        val reminderRegex = """提醒我?(\d{1,2})[点时:：](.+)""".toRegex()
        val reminderMatch = reminderRegex.find(trimmed)
        if (reminderMatch != null) {
            val hour = reminderMatch.groupValues[1]
            val minute = if (reminderMatch.groupValues[2].length >= 2 && reminderMatch.groupValues[2].substring(0, 2).toIntOrNull() != null) {
                val min = reminderMatch.groupValues[2].substring(0, 2)
                val rest = reminderMatch.groupValues[2].substring(2)
                reminderMatch.groupValues[1] + ":" + min
            } else {
                reminderMatch.groupValues[1] + ":00"
            }
            val content = reminderMatch.groupValues[2].trim()
            return LlmIntent(
                intent = "add",
                content = content,
                reminderTime = "$hour:00"
            )
        }

        // "记住这个" -> screenshot context
        if (trimmed.contains("记住这个") || trimmed.contains("记住屏幕") || trimmed.contains("截图")) {
            return LlmIntent(
                intent = "add",
                content = trimmed,
                needContext = ContextNeed(screenshot = true, location = false)
            )
        }

        // "这里" -> location context
        if (trimmed == "这里" || trimmed == "记住这里" || trimmed.contains("在这个位置") || trimmed.contains("当前位置")) {
            return LlmIntent(
                intent = "add",
                content = trimmed,
                needContext = ContextNeed(screenshot = false, location = true)
            )
        }

        // Default -> add with original text
        return LlmIntent(
            intent = "add",
            content = trimmed
        )
    }
}
