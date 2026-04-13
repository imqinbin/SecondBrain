package com.qb.secondbrain.llm

import com.google.gson.Gson
import com.qb.secondbrain.data.model.LlmIntent
import javax.inject.Inject
import javax.inject.Named

class OpenAiLlmClient @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val gson: Gson,
    @Named("modelName") private val modelName: String
) : LlmClient {

    companion object {
        private const val SYSTEM_PROMPT = """你是一个语音助手，负责将用户的语音指令解析为结构化的 JSON。

根据用户输入，返回以下 JSON 格式：
{
  "intent": "add|query|update|delete",
  "needContext": {
    "screenshot": false,
    "location": false
  },
  "content": "主要内容",
  "tags": ["标签"],
  "reminderTime": "HH:mm 或 null",
  "queryKeywords": ["关键词"]
}

规则：
- intent: add(添加), query(查询), update(修改), delete(删除)
- screenshot: 当用户说"记住这个"、"截图"时为 true
- location: 当用户说"这里"、"当前位置"时为 true
- reminderTime: 仅在用户提到具体时间时填写，格式为 HH:mm
- queryKeywords: 查询时提取的关键词列表
- content: 备忘录的主要内容
- tags: 自动推断的标签

只返回 JSON，不要返回其他内容。"""
    }

    override suspend fun parseIntent(text: String, context: String?): Result<LlmIntent> {
        return try {
            val userMessage = if (context != null) {
                "用户语音: $text\n上下文: $context"
            } else {
                "用户语音: $text"
            }

            val request = ChatCompletionRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = userMessage)
                )
            )

            val response = openAiApi.chatCompletion(request)
            val responseContent = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(IllegalStateException("API 返回空响应"))

            val jsonString = extractJson(responseContent)
            val intent = gson.fromJson(jsonString, LlmIntent::class.java)
            Result.success(intent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJson(content: String): String {
        // Try to extract JSON from markdown code blocks
        val codeBlockRegex = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val codeBlockMatch = codeBlockRegex.find(content)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // Try to find raw JSON object
        val jsonRegex = """\{[\s\S]*\}""".toRegex()
        val jsonMatch = jsonRegex.find(content)
        if (jsonMatch != null) {
            return jsonMatch.value.trim()
        }

        return content.trim()
    }
}
