package com.aetherdown.app

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        updateYoutubeDl()
    }

    private fun initYoutubeDl() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this)
            Timber.d("youtubedl-android, ffmpeg and aria2c initialized successfully")
        } catch (e: Throwable) {
            Timber.e(e, "Failed to initialize youtubedl-android components")
        }
    }

    private fun updateYoutubeDl() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(this@AetherApp, YoutubeDL.UpdateChannel.STABLE)
                Timber.d("yt-dlp update status: $status")
            } catch (e: Throwable) {
                Timber.e(e, "Failed to update yt-dlp")
            }
        }
    }

    companion object {
        lateinit var instance: AetherApp
            private set
    }
}
