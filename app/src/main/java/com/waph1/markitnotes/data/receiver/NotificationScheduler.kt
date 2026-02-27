package com.waph1.markitnotes.data.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.waph1.markitnotes.data.model.Note

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(note: Note) {
        val reminderTime = note.reminder ?: return
        if (reminderTime < System.currentTimeMillis()) return 

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, note.id) 
            putExtra(ReminderReceiver.EXTRA_TITLE, note.title)
            putExtra(ReminderReceiver.EXTRA_CONTENT, note.content.take(100))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    android.util.Log.w("NotificationScheduler", "Cannot schedule exact alarm: permission denied")
                    return
                }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            android.util.Log.e("NotificationScheduler", "SecurityException scheduling alarm", e)
        }
    }

    fun cancel(note: Note) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
