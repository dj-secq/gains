package com.example.repsgrams

import android.app.Application
import com.example.repsgrams.reminder.ReminderNotifications

class RepsGramsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannels(this)
        container = DefaultAppContainer(this)
    }
}
