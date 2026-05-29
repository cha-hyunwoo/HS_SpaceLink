package com.example.hs_spacelink

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class RoomTrackingService : Service() {

    private var scheduler: ScheduledExecutorService? = null
    private val channelId = "SPACE_LINK_CHANNEL"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startTime = intent?.getStringExtra("START") ?: "13:00"
        val endTime = intent?.getStringExtra("END") ?: "15:00"
        val roomName = intent?.getStringExtra("ROOM_NAME") ?: "선택한 스터디룸"

        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler?.schedule({

            sendNotification(roomName, startTime, endTime)
            stopSelf()

        }, 5, TimeUnit.SECONDS)

        return START_REDELIVER_INTENT
    }

    private fun sendNotification(roomName: String, start: String, end: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔓 [HS SpaceLink] 취소 자리 매칭 성공!")
            .setContentText("대박! 선점되어 있던 $roomName [$start ~ $end] 자리가 방금 취소되었습니다! 지금 바로 선점하세요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1002, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId, "SpaceLink Tracking Channel", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduler?.shutdownNow()
    }
}