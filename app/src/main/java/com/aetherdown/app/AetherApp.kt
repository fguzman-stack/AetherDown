package com.aetherdown.app

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AetherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        initYoutubeDl()
    }

    private fun initYoutubeDl() {
        try {
            YoutubeDL.getInstance().init(this)
            Timber.d("youtubedl-android initialized successfully")
        } catch (e: YoutubeDLException) {
            Timber.e(e, "Failed to initialize youtubedl-android")
        }
    }

    companion object {
        lateinit var instance: AetherApp
            private set
    }
}
