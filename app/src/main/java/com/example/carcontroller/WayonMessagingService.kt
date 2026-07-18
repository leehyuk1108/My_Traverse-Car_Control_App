package com.example.carcontroller

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale

class WayonMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        WayonPushRegistrar.registerToken(this, token, force = true)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != "wayon_impact") return
        WayonImpactNotifications.show(this, message.data)
    }
}

object WayonImpactNotifications {
    private const val CHANNEL_ID = "wayon_impact_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "주차 충격 감지",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "주차 중 차량 충격이 감지되면 알려줍니다"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 450)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, data: Map<String, String>) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val isTest = data["test"] == "true"
        val severity = when (data["severity"]) {
            "severe" -> "강한"
            "moderate" -> "중간"
            else -> "가벼운"
        }
        val peakG = data["peakDynamicG"]?.toDoubleOrNull()
        val detail = if (peakG != null) {
            String.format(Locale.KOREA, "%s 충격 · %.2f g", severity, peakG)
        } else {
            "$severity 충격이 감지됐습니다"
        }
        val title = if (isTest) "Wayon 충격 알림 테스트" else "주차 충격 감지"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("wayonImpactId", data["impactId"])
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            data["impactId"].orEmpty().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_impact)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (isTest) "$detail\nCloud와 My Traverse 알림 연결이 정상입니다." else detail,
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = data["impactId"].orEmpty().hashCode().let { if (it == 0) 9201 else it }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }
}
