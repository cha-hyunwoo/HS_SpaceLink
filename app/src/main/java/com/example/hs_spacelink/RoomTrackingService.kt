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

        // ★ [디테일 동기화 1] MainActivity에서 파싱해서 넘겨준 가독성 좋은 시간대 문자열 수신
        val timeRange = intent?.getStringExtra("TIME_RANGE") ?: "$startTime~$endTime"

        // ★ [디테일 동기화 2] 볼륨 다운 키로 발동된 가짜 시연용 강제 알림 신호 수신
        val isForceCancel = intent?.getBooleanExtra("FORCE_CANCEL_SIGNAL", false) ?: false

        // [분기 1] 시연용: 볼륨 다운 키를 누르면 백그라운드 추적을 건너뛰고 "즉시" 알림 송신
        if (isForceCancel) {
            sendNotification(roomName, timeRange)
            scheduler?.shutdownNow() // 스케줄러가 돌고 있었다면 안전하게 종료
            stopSelf()
            return START_REDELIVER_INTENT
        }

        // [분기 2] 실제 기능: 버튼을 눌렀을 때, 백그라운드에서 5초마다 실시간으로 크롤링 감시 동작!
        if (scheduler == null || scheduler?.isShutdown == true) {
            scheduler = Executors.newSingleThreadScheduledExecutor()

            // 5초 주기로 무한 반복하며 네트워크 감시 (initialDelay: 0초, period: 5초)
            scheduler?.scheduleAtFixedRate({
                try {
                    // Jsoup 크롤링 함수를 호출하여 해당 방이 비었는지(취소되었는지) 체크
                    val isRoomCanceledNow = checkActualRoomCancellation(roomName, timeRange)

                    // 만약 실제로 취소된 것이 감지되었다면 푸시 알림을 보낸다.
                    if (isRoomCanceledNow) {
                        sendNotification(roomName, timeRange)
                        scheduler?.shutdown() // 감시 성공했으므로 타이머 종료
                        stopSelf()            // 서비스 종료
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 0, 5, TimeUnit.SECONDS)
        }

        return START_REDELIVER_INTENT
    }

    // ★ [추가] 실제 크롤링 검증 함수 아키텍처 (시연용 안전장치로 return false 세팅)
    private fun checkActualRoomCancellation(roomName: String, timeRange: String): Boolean {
        // 원래는 여기서 Jsoup으로 학교 서버를 긁어야 하지만, 테스트/시연 시 터지는 걸 막기 위해 false 반환
        // (만약 5초 뒤 자동 알림이 기기에서 잘 터지는지 테스트해보고 싶다면 이 부분을 true로 바꾸면 됩니다!)
        return false
    }

    // 동적 텍스트 매핑 체계를 위해 기존 매개변수 구조를 복합 시간대(timeRange) 수신형으로 깔끔하게 리팩토링
    private fun sendNotification(roomName: String, timeRange: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            // 알림을 클릭하고 진입했을 때 액티비티가 새롭게 켜지거나 갱신되도록 플래그 주입
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 장소와 시간 정보가 또렷한 상용 앱 퀄리티 문구 셋업
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("[HS SpaceLink] 빈방 매칭 성공!")
            .setContentText("${roomName} (${timeRange}) 자리가 방금 취소되었습니다! 지금 바로 선점하세요!")
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