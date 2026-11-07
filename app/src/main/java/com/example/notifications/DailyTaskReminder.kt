package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.JimDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object DailyTaskReminderScheduler {
    private const val TAG = "DailyTaskReminder"
    private const val PREFERENCES = "daily_task_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TIME = "time"
    private const val DEFAULT_TIME = "08:00"
    private const val REQUEST_CODE = 1004

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, false)

    fun getTime(context: Context): String =
        preferences(context).getString(KEY_TIME, DEFAULT_TIME) ?: DEFAULT_TIME

    fun configure(context: Context, enabled: Boolean, time: String) {
        require(DailyReminderTime.nextTriggerAtMillis(time, System.currentTimeMillis()) != null) {
            "Reminder time must use HH:mm format"
        }
        preferences(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TIME, time)
            .apply()

        if (enabled) {
            scheduleNext(context, time)
        } else {
            cancel(context)
        }
    }

    fun rescheduleFromPreferences(context: Context) {
        if (isEnabled(context)) {
            scheduleNext(context, getTime(context))
        } else {
            cancel(context)
        }
    }

    fun scheduleNext(context: Context, time: String, nowMillis: Long = System.currentTimeMillis()) {
        val triggerAtMillis = DailyReminderTime.nextTriggerAtMillis(time, nowMillis) ?: run {
            Log.e(TAG, "Invalid saved daily reminder time: $time")
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            reminderPendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context))
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyTaskReminderReceiver::class.java)
            .setAction(DailyTaskReminderReceiver.ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyTaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DailyTaskReminderScheduler.rescheduleFromPreferences(context)
        if (intent?.action != ACTION_REMIND) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = JimDatabase.getDatabase(context).jimDao()
                val assignments = dao.getAllAssignmentsFlow().first()
                    .filter { it.status != "COMPLETED" }
                if (assignments.isEmpty()) return@launch

                val courseNames = dao.getAllCourses().associate { it.id to it.name }
                val lines = assignments.take(MAX_TASKS_IN_NOTIFICATION).map { assignment ->
                    val courseName = courseNames[assignment.courseId]
                    val dueDate = assignment.dueDate.takeIf(String::isNotBlank)
                        ?.let { " - التسليم: $it" }
                        .orEmpty()
                    "• ${assignment.title}" +
                        (courseName?.let { " ($it)" } ?: "") +
                        dueDate
                }
                val remainingCount = assignments.size - lines.size
                val message = buildString {
                    append(lines.joinToString("\n"))
                    if (remainingCount > 0) {
                        append("\nو ")
                        append(remainingCount)
                        append(" مهام أخرى")
                    }
                }
                JimNotificationHelper.showTaskReminder(
                    context,
                    JimNotificationHelper.NOTIF_ID_DAILY_TASKS,
                    "مهامك غير المنجزة (${assignments.size})",
                    message
                )
            } catch (error: Exception) {
                Log.e(TAG, "Failed to create the daily tasks notification", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.example.notifications.DAILY_TASK_REMINDER"
        private const val TAG = "DailyTaskReminder"
        private const val MAX_TASKS_IN_NOTIFICATION = 5
    }
}

class DailyTaskReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> DailyTaskReminderScheduler.rescheduleFromPreferences(context)
        }
    }
}
