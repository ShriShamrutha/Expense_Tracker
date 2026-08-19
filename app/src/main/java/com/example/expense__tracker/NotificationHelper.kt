package com.example.expense__tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID      = "expense_budget_channel"
    private const val CHANNEL_NAME    = "Budget Alerts"
    private const val NOTIF_ID_WARN   = 1001
    private const val NOTIF_ID_EXCEED = 1002

    // Call this once when app starts — safe to call multiple times
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you are approaching or exceeding your monthly budget"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotify(context: Context, totalSpent: Double, budget: Double) {
        if (budget <= 0) return

        val percent = (totalSpent / budget * 100).toInt()

        when {
            totalSpent > budget -> sendNotification(
                context  = context,
                id       = NOTIF_ID_EXCEED,
                title    = "🚨 Budget Exceeded!",
                message  = "You have spent ${formatAmount(totalSpent)} — " +
                        "${formatAmount(totalSpent - budget)} over your " +
                        "${formatAmount(budget)} budget.",
                priority = NotificationCompat.PRIORITY_MAX
            )
            percent >= 80 -> sendNotification(
                context  = context,
                id       = NOTIF_ID_WARN,
                title    = "⚠️ Approaching Budget Limit",
                message  = "$percent% of your ${formatAmount(budget)} budget used. " +
                        "Only ${formatAmount(budget - totalSpent)} remaining.",
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }
    }

    private fun sendNotification(
        context:  Context,
        id:       Int,
        title:    String,
        message:  String,
        priority: Int
    ) {
        // On Android 13+ check permission at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
        }

        // Tap notification → opens app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}