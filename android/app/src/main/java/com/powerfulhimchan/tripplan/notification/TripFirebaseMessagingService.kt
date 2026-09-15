package com.powerfulhimchan.tripplan.notification

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.powerfulhimchan.tripplan.MainActivity

class TripFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val channelId = "trip_schedule"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channelId, "여행 일정", NotificationManager.IMPORTANCE_HIGH))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                putExtra("tripId", message.data["tripId"])
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: "여행 일정 알림")
            .setContentText(message.notification?.body ?: "예정된 일정을 확인하세요.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(message.data["itemId"]?.hashCode() ?: 1, notification)
    }
}

