package com.aqpseller.lulaapp

import android.app.Application
import com.aqpseller.lulaapp.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LulaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.crearCanales(this)
    }
}
