package com.qb.secondbrain.asr

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AsrEngineFactoryTest {

    private lateinit var factory: AsrEngineFactory

    @Before
    fun setUp() {
        val xfyunProvider = javax.inject.Provider<XfyunAsrEngine> { XfyunAsrEngine() }
        val baiduProvider = javax.inject.Provider<BaiduAsrEngine> { BaiduAsrEngine() }
        val whisperProvider = javax.inject.Provider<WhisperAsrEngine> { WhisperAsrEngine() }
        factory = AsrEngineFactory(xfyunProvider, baiduProvider, whisperProvider)
    }

    @Test
    fun `getEngine returns XfyunAsrEngine for xfyun`() {
        val engine = factory.getEngine("xfyun")
        assertTrue(engine is XfyunAsrEngine)
    }

    @Test
    fun `getEngine returns BaiduAsrEngine for baidu`() {
        val engine = factory.getEngine("baidu")
        assertTrue(engine is BaiduAsrEngine)
    }

    @Test
    fun `getEngine returns WhisperAsrEngine for whisper`() {
        val engine = factory.getEngine("whisper")
        assertTrue(engine is WhisperAsrEngine)
    }

    @Test
    fun `getEngine returns WhisperAsrEngine for unknown name`() {
        val engine = factory.getEngine("unknown_engine")
        assertTrue(engine is WhisperAsrEngine)
    }

    @Test
    fun `getEngine is case insensitive`() {
        assertTrue(factory.getEngine("XFYUN") is XfyunAsrEngine)
        assertTrue(factory.getEngine("Baidu") is BaiduAsrEngine)
        assertTrue(factory.getEngine("WHISPER") is WhisperAsrEngine)
        assertTrue(factory.getEngine("Xfyun") is XfyunAsrEngine)
    }
}
