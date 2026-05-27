package com.qb.secondbrain

import android.app.Application
import android.util.Log
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.BaseLibrary
import com.iflytek.aikit.core.CoreListener
import com.iflytek.aikit.core.ErrType
import com.iflytek.aikit.core.LogLvl
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SecondBrainApp : Application() {

    companion object {
        private const val TAG = "SecondBrainApp"
        private val initLatch = CountDownLatch(1)
        @Volatile
        var sdkReady = false
            private set

        fun awaitSdkInit(timeoutSeconds: Long = 30): Boolean {
            return initLatch.await(timeoutSeconds, TimeUnit.SECONDS) && sdkReady
        }
    }

    override fun onCreate() {
        super.onCreate()

        val appId = BuildConfig.XFYUN_APP_ID
        val apiKey = BuildConfig.XFYUN_API_KEY
        val apiSecret = BuildConfig.XFYUN_API_SECRET
        if (appId.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            val workDir = getExternalFilesDir(null)?.absolutePath
                ?: filesDir.absolutePath
            AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, "$workDir/aikit.log")

            AiHelper.getInst().registerListener(object : CoreListener {
                override fun onAuthStateChange(type: ErrType, code: Int) {
                    Log.i(TAG, "SDK auth: type=$type, code=$code")
                    if (type == ErrType.AUTH) {
                        sdkReady = code == 0
                        initLatch.countDown()
                    }
                }
            })

            val params = BaseLibrary.Params.builder()
                .appId(appId)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .workDir(workDir)
                .build()
            Thread { AiHelper.getInst().initEntry(this, params) }.start()
        } else {
            initLatch.countDown()
        }
    }
}
