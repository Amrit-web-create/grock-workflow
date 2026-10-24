package com.customapplock.studio.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.customapplock.studio.AppLockApp
import com.customapplock.studio.R
import com.customapplock.studio.ui.MainActivity

/** Minimal low-priority foreground service that keeps the process alive. No polling, no wake locks. */
class GuardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, AppLockApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("App Lock is active")
            .setContentText("Your selected apps are protected")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pi)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, n)
            }
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    companion object {
        fun start(ctx: Context) {
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, GuardService::class.java)) }
        }
        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, GuardService::class.java)) }
        }
    }
}
