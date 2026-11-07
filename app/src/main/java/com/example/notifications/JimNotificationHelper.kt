package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object JimNotificationHelper {
    const val CHANNEL_ID = "jim_academic_channel"
    const val CHANNEL_NAME = "تنبيهات المهام والاختبارات الجامعية"
    const val NOTIF_ID_DAILY = 1001
    const val NOTIF_ID_ASSIGNMENT = 1002
    const val NOTIF_ID_EXAM = 1003
    const val NOTIF_ID_DAILY_TASKS = -1004

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "إشعارات تذكيرية بالمهام الدراسية والاختبارات وموجز اليوم"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jim_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (error: SecurityException) {
            Log.e("JimNotification", "Notification permission is unavailable", error)
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId)
    }
}
