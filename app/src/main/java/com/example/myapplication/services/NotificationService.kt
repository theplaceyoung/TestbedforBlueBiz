package com.example.myapplication.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.R
import okhttp3.internal.http2.Http2Reader

class NotificationService : Service() {

    private val notificationHandler = Handler()
    private lateinit var notificationRunnable: Runnable

    // 알림 간격을 2시간으로 설정
    private val notificationInterval: Long = 2 * 60 * 60 * 1000 // 2시간마다 알림

    override fun onCreate() {
        super.onCreate()

        notificationRunnable = Runnable {
            showNotification()
            notificationHandler.postDelayed(notificationRunnable, notificationInterval)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationHandler.post(notificationRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationHandler.removeCallbacks(notificationRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun showNotification() {
        val notificationId = 1
        val channelId = "default_channel_id"
        val notificationTitle = "알림 제목"
        val notificationText = "2시간마다 앱을 사용해보세요!"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ic_notification
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

