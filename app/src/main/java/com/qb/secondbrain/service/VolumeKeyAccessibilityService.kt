package com.qb.secondbrain.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeKeyAccessibilityService : AccessibilityService() {

    private var volumeUpPressTime = 0L
    private var volumeDownPressTime = 0L
    private var volumeUpReleased = true
    private var volumeDownReleased = true

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        // 两个键按下时间差不超过300ms视为同时
        private const val SIMULTANEOUS_THRESHOLD = 300L
        // 按压时长不超过400ms视为短按
        private const val SHORT_PRESS_THRESHOLD = 400L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeUpPressTime = event.eventTime
                    volumeUpReleased = false
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeUpReleased = true
                    tryTriggerVoiceMemo(event.eventTime)
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeDownPressTime = event.eventTime
                    volumeDownReleased = false
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeDownReleased = true
                    tryTriggerVoiceMemo(event.eventTime)
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun tryTriggerVoiceMemo(upTime: Long) {
        // 两个键都已松开，且都曾是短按，且按下时间接近
        if (volumeUpReleased && volumeDownReleased) {
            val pressDiff = kotlin.math.abs(volumeUpPressTime - volumeDownPressTime)
            val upDuration = upTime - minOf(volumeUpPressTime, volumeDownPressTime)
            if (pressDiff <= SIMULTANEOUS_THRESHOLD && upDuration <= SHORT_PRESS_THRESHOLD) {
                val intent = Intent(this, VoiceMemoService::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = VoiceMemoService.ACTION_TOGGLE_RECORDING
                }
                startService(intent)
                // 重置，避免重复触发
                volumeUpPressTime = 0L
                volumeDownPressTime = 0L
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
