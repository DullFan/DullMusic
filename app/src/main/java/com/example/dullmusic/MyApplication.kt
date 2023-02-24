package com.example.dullmusic

import android.app.Application
import com.example.base.utils.showLog
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCenter.start(
            this,
            "7f4ffe49-58c2-4546-8467-8371f9c4e261",
            Analytics::class.java,
            Crashes::class.java
        )
    }
}