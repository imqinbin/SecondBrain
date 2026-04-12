package com.qb.secondbrain.llm

import com.qb.secondbrain.data.model.ContextNeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedFallbackTest {

    private lateinit var fallback: RuleBasedFallback

    @Before
    fun setUp() {
        fallback = RuleBasedFallback()
    }

    // --- Delete patterns ---

    @Test
    fun `delete last memo - 删除最后一条`() {
        val result = fallback.parseIntent("删除最后一条")
        assertEquals("delete", result.intent)
        assertEquals(listOf("最近"), result.queryKeywords)
    }

    @Test
    fun `delete last memo - 删除上一条`() {
        val result = fallback.parseIntent("删除上一条备忘录")
        assertEquals("delete", result.intent)
        assertEquals(listOf("最近"), result.queryKeywords)
    }

    // --- Query patterns ---

    @Test
    fun `query today - 今天的备忘录`() {
        val result = fallback.parseIntent("今天的备忘录")
        assertEquals("query", result.intent)
        assertEquals(listOf("今天"), result.queryKeywords)
    }

    @Test
    fun `query today - 今日`() {
        val result = fallback.parseIntent("今日记录")
        assertEquals("query", result.intent)
        assertEquals(listOf("今天"), result.queryKeywords)
    }

    @Test
    fun `query recent - 最近`() {
        val result = fallback.parseIntent("最近的备忘录")
        assertEquals("query", result.intent)
        assertEquals(listOf("最近"), result.queryKeywords)
    }

    @Test
    fun `query by keyword - 搜索`() {
        val result = fallback.parseIntent("搜索会议记录")
        assertEquals("query", result.intent)
        assertEquals(listOf("会议记录"), result.queryKeywords)
    }

    @Test
    fun `query by keyword - 查找`() {
        val result = fallback.parseIntent("查找购物清单")
        assertEquals("query", result.intent)
        assertEquals(listOf("购物清单"), result.queryKeywords)
    }

    @Test
    fun `query by keyword - 找一下`() {
        val result = fallback.parseIntent("找一下项目进度")
        assertEquals("query", result.intent)
        assertEquals(listOf("项目进度"), result.queryKeywords)
    }

    // --- Update patterns ---

    @Test
    fun `update - 修改XXX为YYY`() {
        val result = fallback.parseIntent("修改会议时间为下午三点")
        assertEquals("update", result.intent)
        assertTrue(result.content.contains("修改"))
        assertTrue(result.content.contains("会议时间"))
        assertTrue(result.content.contains("下午三点"))
    }

    // --- Reminder patterns ---

    @Test
    fun `reminder - 提醒我N点`() {
        val result = fallback.parseIntent("提醒我3点开会")
        assertEquals("add", result.intent)
        assertNotNull(result.reminderTime)
        assertTrue(result.content.contains("开会") || result.content.contains("提醒"))
    }

    @Test
    fun `reminder - 提醒我N时`() {
        val result = fallback.parseIntent("提醒我14时吃药")
        assertEquals("add", result.intent)
        assertNotNull(result.reminderTime)
    }

    // --- Screenshot context ---

    @Test
    fun `screenshot context - 记住这个`() {
        val result = fallback.parseIntent("记住这个")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.screenshot)
        assertFalse(result.needContext.location)
    }

    @Test
    fun `screenshot context - 截图`() {
        val result = fallback.parseIntent("截图保存")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.screenshot)
    }

    // --- Location context ---

    @Test
    fun `location context - 这里`() {
        val result = fallback.parseIntent("这里")
        assertEquals("add", result.intent)
        assertFalse(result.needContext.screenshot)
        assertTrue(result.needContext.location)
    }

    @Test
    fun `location context - 记住这里`() {
        val result = fallback.parseIntent("记住这里")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.location)
    }

    @Test
    fun `location context - 在这个位置`() {
        val result = fallback.parseIntent("在这个位置记录")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.location)
    }

    // --- Default add ---

    @Test
    fun `default - add with original text`() {
        val text = "明天要去超市买牛奶"
        val result = fallback.parseIntent(text)
        assertEquals("add", result.intent)
        assertEquals(text, result.content)
    }

    @Test
    fun `default - add plain text`() {
        val text = "这是一个普通的备忘录"
        val result = fallback.parseIntent(text)
        assertEquals("add", result.intent)
        assertEquals(text, result.content)
    }

    @Test
    fun `default - no context needed`() {
        val result = fallback.parseIntent("随便说点什么")
        assertEquals("add", result.intent)
        assertFalse(result.needContext.screenshot)
        assertFalse(result.needContext.location)
    }
}
