package com.qb.secondbrain.data.local

import com.qb.secondbrain.data.model.ImagePath
import com.qb.secondbrain.data.model.ImageSource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun `fromStringList converts empty list to empty JSON array`() {
        val result = converters.fromStringList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromStringList and toStringList round-trip preserves values`() {
        val original = listOf("tag1", "tag2", "tag3")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `toStringList handles null JSON gracefully`() {
        val result = converters.toStringList("null")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `fromImagePathList and toImagePathList round-trip preserves values`() {
        val original = listOf(
            ImagePath("/data/cache/screenshot_123.png", ImageSource.VOICE_SCREENSHOT),
            ImagePath("/data/cache/photo_456.jpg", ImageSource.CAMERA),
            ImagePath("/data/cache/gallery_789.webp", ImageSource.GALLERY)
        )
        val json = converters.fromImagePathList(original)
        val restored = converters.toImagePathList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `toImagePathList handles null JSON gracefully`() {
        val result = converters.toImagePathList("null")
        assertEquals(emptyList<ImagePath>(), result)
    }

    @Test
    fun `fromStringList preserves strings with special characters`() {
        val original = listOf("hello world", "特殊字符", "path/to/file")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }
}