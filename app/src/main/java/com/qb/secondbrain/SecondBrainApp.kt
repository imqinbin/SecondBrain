package com.qb.secondbrain

import android.app.Application
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecondBrainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appId = BuildConfig.XFYUN_APP_ID
        if (appId.isNotBlank()) {
            SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + appId)
        }
    }
}
