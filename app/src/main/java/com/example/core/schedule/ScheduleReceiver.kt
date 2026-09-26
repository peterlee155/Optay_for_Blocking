package com.example.core.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.OptayApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OptayScheduleReceiver"
        const val ACTION_SCHEDULE_START = "com.example.optay.action.SCHEDULE_START"
        const val ACTION_SCHEDULE_END = "com.example.optay.action.SCHEDULE_END"
        const val EXTRA_MODE_ID = "extra_mode_id"

        private const val SCHEDULE_NOTIFICATION_CHANNEL = "optay_schedule_channel"
        private const val SCHEDULE_NOTIFICATION_ID = 2002
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val modeId = intent.getStringExtra(EXTRA_MODE_ID) ?: return
        val app = context.applicationContext as? OptayApplication ?: return

        Log.i(TAG, "Schedule event received: $action for mode $modeId")

        CoroutineScope(Dispatchers.IO).launch {
            val mode = app.focusModeRepository.getModeById(modeId) ?: return@launch
            val schedule = mode.schedule ?: return@launch
            if (!schedule.isEnabled) return@launch

            when (action) {
                ACTION_SCHEDULE_START -> {
                    // Check if already active
                    val currentSession = app.firewallManager.activeSession.value
                    if (currentSession != null) {
                        Log.i(TAG, "Session already active (${currentSession.modeName}). Skipping schedule start.")
                        return@launch
                    }

                    // Check VPN consent
                    if (app.firewallManager.isVpnPermissionRequired(context)) {
                        Log.w(TAG, "VPN permission required to auto-start scheduled mode. Prompting user.")
                        notifyScheduleRequiresConsent(context, mode.name)
                        return@launch
                    }

                    // Calculate duration for this scheduled window
                    val durationMin = calculateScheduledDuration(schedule.startHour, schedule.startMinute, schedule.endHour, schedule.endMinute)
                    launch(Dispatchers.Main) {
                        app.firewallManager.startFocusMode(context, mode, durationMin)
                    }

                    // Schedule next transition
                    ScheduleManager.scheduleNextForMode(context, mode)
                }

                ACTION_SCHEDULE_END -> {
                    val currentSession = app.firewallManager.activeSession.value
                    if (currentSession != null && currentSession.modeId == modeId) {
                        launch(Dispatchers.Main) {
                            app.firewallManager.stopFocusMode(context, completedNaturally = true)
                        }
                    }
                    // Schedule next transition
                    ScheduleManager.scheduleNextForMode(context, mode)
                }
            }
        }
    }

    private fun calculateScheduledDuration(startH: Int, startM: Int, endH: Int, endM: Int): Int {
        val startTotal = startH * 60 + startM
        val endTotal = endH * 60 + endM
        return if (endTotal >= startTotal) {
            endTotal - startTotal
        } else {
            // Overnight: e.g. 23:00 (1380m) to 07:00 (420m) -> (1440 - 1380) + 420 = 480m
            (1440 - startTotal) + endTotal
        }
    }

    private fun notifyScheduleRequiresConsent(context: Context, modeName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SCHEDULE_NOTIFICATION_CHANNEL,
                "Optay Scheduled Boundaries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to activate scheduled focus boundaries"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SCHEDULE_NOTIFICATION_CHANNEL)
            .setContentTitle("Scheduled Focus: $modeName")
            .setContentText("Tap to grant local firewall permission and start boundary.")
            .setSmallIcon(R.drawable.ic_optay_shield)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(SCHEDULE_NOTIFICATION_ID, notification)
    }
}
