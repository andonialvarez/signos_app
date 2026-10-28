package com.andoni.signosdaily

import android.app.Application
import com.andoni.signosdaily.notify.Notifications

class SignosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
    }
}
