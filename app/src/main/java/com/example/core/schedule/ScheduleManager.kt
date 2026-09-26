package com.example.core.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.domain.model.FocusMode
import com.example.domain.model.ScheduleRule
import java.util.Calendar

object ScheduleManager {

    private const val TAG = "OptayScheduleManager"
    private const val REQUEST_CODE_START_OFFSET = 10000
    private const val REQUEST_CODE_END_OFFSET = 20000

    fun scheduleNextForMode(context: Context, mode: FocusMode) {
        val schedule = mode.schedule
        if (schedule == null || !schedule.isEnabled) {
            cancelAlarmsForMode(context, mode.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = Calendar.getInstance()

        // 1. Next Start Time
        val nextStartCal = calculateNextOccurrence(now, schedule.daysOfWeek, schedule.startHour, schedule.startMinute)
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_START
            putExtra(ScheduleReceiver.EXTRA_MODE_ID, mode.id)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            mode.id.hashCode() + REQUEST_CODE_START_OFFSET,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmCompat(alarmManager, nextStartCal.timeInMillis, startPending)
        Log.i(TAG, "Scheduled start for ${mode.name} at ${nextStartCal.time}")

        // 2. Next End Time
        val nextEndCal = calculateNextEndOccurrence(nextStartCal, schedule)
        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_END
            putExtra(ScheduleReceiver.EXTRA_MODE_ID, mode.id)
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            mode.id.hashCode() + REQUEST_CODE_END_OFFSET,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmCompat(alarmManager, nextEndCal.timeInMillis, endPending)
        Log.i(TAG, "Scheduled end for ${mode.name} at ${nextEndCal.time}")
    }

    fun cancelAlarmsForMode(context: Context, modeId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_START
            putExtra(ScheduleReceiver.EXTRA_MODE_ID, modeId)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            modeId.hashCode() + REQUEST_CODE_START_OFFSET,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPending != null) {
            alarmManager.cancel(startPending)
            startPending.cancel()
        }

        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_END
            putExtra(ScheduleReceiver.EXTRA_MODE_ID, modeId)
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            modeId.hashCode() + REQUEST_CODE_END_OFFSET,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPending != null) {
            alarmManager.cancel(endPending)
            endPending.cancel()
        }
    }

    private fun scheduleAlarmCompat(alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to inexact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun calculateNextOccurrence(
        now: Calendar,
        daysOfWeek: Set<Int>,
        targetHour: Int,
        targetMinute: Int
    ): Calendar {
        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                if (candidate.after(now)) {
                    return candidate
                }
            }
        }

        // Fallback: tomorrow at same time
        return Calendar.getInstance().apply {
            timeInMillis = target.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun calculateNextEndOccurrence(startCal: Calendar, schedule: ScheduleRule): Calendar {
        val endCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, schedule.endHour)
            set(Calendar.MINUTE, schedule.endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If end time is before or equal to start time, this is an overnight schedule (ends next day)
        if (!endCal.after(startCal)) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return endCal
    }
}
