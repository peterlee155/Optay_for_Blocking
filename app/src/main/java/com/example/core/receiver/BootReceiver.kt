package com.example.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.OptayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("OptayBootReceiver", "Boot completed received. Checking profile restoration...")
            val app = context.applicationContext as? OptayApplication ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val isRestoreEnabled = app.preferencesRepository.isRestoreOnBootEnabled.value
                if (isRestoreEnabled) {
                    val lastModeId = app.preferencesRepository.selectedModeId.value
                    val mode = app.focusModeRepository.getModeById(lastModeId)
                    if (mode != null && mode.schedule?.isEnabled == true) {
                        Log.i("OptayBootReceiver", "Restoring scheduled profile: ${mode.name}")
                        // Active schedule verification
                    }
                }
            }
        }
    }
}
