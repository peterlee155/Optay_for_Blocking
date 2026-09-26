package com.example.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.OptayApplication
import com.example.core.schedule.ScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OptayBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        Log.i(TAG, "Device reboot or package update received ($action). Commencing recovery...")
        val app = context.applicationContext as? OptayApplication ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Session state recovery
                val isRestoreEnabled = app.preferencesRepository.isRestoreOnBootEnabled.value
                if (isRestoreEnabled) {
                    Log.i(TAG, "Restoring active focus session if valid...")
                    app.firewallManager.restorePersistedSession(context)
                }

                // 2. Re-arm all active schedules
                val allModes = app.focusModeRepository.focusModes.first()
                for (mode in allModes) {
                    if (mode.schedule?.isEnabled == true) {
                        Log.i(TAG, "Re-arming schedule alarm for: ${mode.name}")
                        ScheduleManager.scheduleNextForMode(context, mode)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during boot recovery sequence", e)
            }
        }
    }
}
