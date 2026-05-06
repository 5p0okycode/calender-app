package com.crochet.calendar

import android.Manifest
import android.app.*
import android.content.*
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar as JC

class EventReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getStringExtra("event_name") ?: return
        val eventId   = intent.getIntExtra("event_id", 0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.app_icon)  // swap for your own icon
            .setContentTitle("🧶 Crochet Calendar")
            .setContentText(eventName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)   // dismisses when tapped
            .build()

        NotificationManagerCompat.from(context).notify(eventId, notification)
    }

    companion object {
        const val CHANNEL_ID = "crochet_reminders"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. NotificationHelper — schedules and cancels reminders
//    Call scheduleReminder() when the user saves an event with reminder = true
// ─────────────────────────────────────────────────────────────────────────────

class NotificationHelper(private val context: Context) {

    // Create the notification channel — call this once in Application.onCreate()
    // or MainActivity.onCreate(). Safe to call multiple times.
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EventReminderReceiver.CHANNEL_ID,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your crochet calendar events"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Schedule a notification for a given Event
    fun scheduleReminder(event: Event) {
        if (!event.reminder) return

        // Build the time to fire — 9am on the event day if no time set,
        // or 15 minutes before if a time is provided
        val cal = JC.getInstance().apply {
            set(JC.YEAR,         event.year)
            set(JC.MONTH,        event.month)  // already 0-based
            set(JC.DAY_OF_MONTH, event.day)

            val timeParts = event.time?.split(":")
            if (timeParts?.size == 2) {
                set(JC.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 9)
                set(JC.MINUTE,      (timeParts[1].toIntOrNull() ?: 0) - 15)
            } else {
                set(JC.HOUR_OF_DAY, 9)
                set(JC.MINUTE, 0)
            }
            set(JC.SECOND, 0)
        }

        // Don't schedule if the time has already passed
        if (cal.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("event_name", event.name)
            putExtra("event_id",   event.id)
        }

        // Each event needs a unique PendingIntent — use event.id as request code
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // RTC_WAKEUP wakes the device even if it's asleep
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()) {
            // Fallback to inexact if exact alarm permission not granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }

    // Cancel a reminder when an event is deleted
    fun cancelReminder(eventId: Int) {
        val intent       = Intent(context, EventReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent)
    }
}