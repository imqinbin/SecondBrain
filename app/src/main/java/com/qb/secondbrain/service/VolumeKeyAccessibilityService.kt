package com.qb.secondbrain.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var lastScreenshot: Bitmap? = null
            private set

        @Volatile
        var isRunning: Boolean = false
            private set

        private const val DOUBLE_PRESS_WINDOW_MS = 200L
    }

    private var volumeUpDownTime: Long = 0L
    private var volumeUpPressed = false
    private var volumeDownPressed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: we only use key events
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeUpPressed = true
                    checkDoublePress()
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeUpPressed = false
                }
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeDownPressed = true
                    checkDoublePress()
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeDownPressed = false
                }
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun checkDoublePress() {
        if (!volumeUpPressed || !volumeDownPressed) return

        val now = System.currentTimeMillis()
        if (now - volumeUpDownTime < DOUBLE_PRESS_WINDOW_MS) {
            // Double press detected
            volumeUpDownTime = 0L
            handleDoublePress()
        } else {
            volumeUpDownTime = now
        }
    }

    private fun handleDoublePress() {
        captureScreenshot()

        if (VoiceMemoServiceStaticHelper.isRecording) {
            VoiceMemoService.stopRecording(this)
            VoiceMemoServiceStaticHelper.isRecording = false
        } else {
            VoiceMemoService.startRecording(this)
            VoiceMemoServiceStaticHelper.isRecording = true
        }
    }

    private fun captureScreenshot() {
        try {
            val rootNode = rootInActiveWindow ?: return
            val bounds = Rect()
            rootNode.getBoundsInScreen(bounds)
            val width = bounds.width()
            val height = bounds.height()
            if (width <= 0 || height <= 0) return

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            lastScreenshot = bitmap
        } catch (e: Exception) {
            // Screenshot capture failed, continue without it
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}

/**
 * Helper object to track recording state across service and accessibility service.
 */
object VoiceMemoServiceStaticHelper {
    @Volatile
    var isRecording: Boolean = false
}
