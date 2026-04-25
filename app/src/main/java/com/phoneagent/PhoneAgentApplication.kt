package com.phoneagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build


@HiltAndroidApp
class PhoneAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val captureChannel = NotificationChannel(
                "screen_capture_channel",
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps PhoneAgent ready to capture your screen"
            }
            
            val agentChannel = NotificationChannel(
                "agent_running_channel",
                "Agent Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when agent is performing tasks"
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(captureChannel)
            manager.createNotificationChannel(agentChannel)
        }

    }
}
