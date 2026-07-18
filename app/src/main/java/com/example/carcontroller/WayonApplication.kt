package com.example.carcontroller

import android.app.Application

class WayonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WayonImpactNotifications.ensureChannel(this)
        WayonPushRegistrar.registerCurrentToken(this)
    }
}
