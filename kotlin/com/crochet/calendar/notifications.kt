package com.crochet.calendar

import android.Manifest
import android.app.*
import android.content.*
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar as JC
import com.crochet.calendar.data.Event
import com.crochet.calendar.data.Pattern
class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getStringExtra("event_name") ?: "Event Reminder"
        val eventId   = intent.getIntExtra("event_id", 0)
        
        Log.d("CrochetNotifications", "Receiver triggered for: $eventName")

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("CrochetNotifications", "Permission not granted, cannot show notification")
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.app_icon)
            .setContentTitle("🧶 Crochet Calendar")
            .setContentText(eventName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(eventId, notification)
            Log.d("CrochetNotifications", "Notification sent successfully")
        } catch (e: SecurityException) {
            Log.e("CrochetNotifications", "SecurityException: ${e.message}")
        }
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
    fun eventReminder(event: Event) {
        if (!event.reminder) return

        // Build the time to fire
        val cal = JC.getInstance().apply {
            set(JC.YEAR,         event.year)
            set(JC.MONTH,        event.month)
            set(JC.DAY_OF_MONTH, event.day)

            val timeParts = event.time?.split(":")
            if (timeParts?.size == 2) {
                val hour = timeParts[0].trim().toIntOrNull() ?: 9
                val min  = timeParts[1].trim().toIntOrNull() ?: 0
                set(JC.HOUR_OF_DAY, hour)
                set(JC.MINUTE,      min)
            } else {
                set(JC.HOUR_OF_DAY, 9)
                set(JC.MINUTE, 0)
            }
            set(JC.SECOND, 0)
            set(JC.MILLISECOND, 0)
        }

        // If time is in the past, don't schedule
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            Log.w("CrochetNotifications", "event already passed")
            return
        }

        Log.d("CrochetNotifications", "Scheduling notification for ${event.name} at ${cal.time}")

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
    
    fun holidayNotification(holiday: holiday, year: Int) {
        // Build the time to fire
        val cal = JC.getInstance().apply {
            set(JC.YEAR,         year)
            set(JC.MONTH,        holiday.month - 1)
            set(JC.DAY_OF_MONTH, holiday.day)
            set(JC.HOUR_OF_DAY,  0)
            set(JC.MINUTE,       0)
            set(JC.SECOND,       0)
            set(JC.MILLISECOND,  0)
        }

        // If time is already passed, don't schedule
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            Log.w("CrochetNotifications", "holiday ${holiday.name} already passed")
            return
        }

        Log.d("CrochetNotifications", "Scheduling holiday notification for ${holiday.name} at ${cal.time}")

        val holidayMessage = "Enjoy a ${holiday.prefix} ${holiday.name} ${holiday.emoji}"
        val holidayId = holiday.name.hashCode()

        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("event_name", holidayMessage)
            putExtra("event_id",   holidayId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            holidayId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()) {
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