package com.example.ui.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.OptayApplication
import com.example.R
import com.example.core.firewall.FirewallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class OptayQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? OptayApplication ?: return
        val firewallManager = app.firewallManager

        if (firewallManager.firewallStatus.value == FirewallStatus.ACTIVE) {
            firewallManager.stopFocusMode(this)
            updateTileState()
        } else {
            if (firewallManager.isVpnPermissionRequired(this)) {
                // Launch MainActivity so user can grant VPN permission
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivityAndCollapse(intent)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val modeId = app.preferencesRepository.selectedModeId.value
                    val mode = app.focusModeRepository.getModeById(modeId)
                    if (mode != null) {
                        firewallManager.startFocusMode(this@OptayQuickTileService, mode)
                        updateTileState()
                    }
                }
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val app = applicationContext as? OptayApplication
        val isActive = app?.firewallManager?.firewallStatus?.value == FirewallStatus.ACTIVE
        val activeSession = app?.firewallManager?.activeSession?.value

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "Optay: ${activeSession?.modeName ?: "Active"}" else "Optay Focus"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_optay_shield)
        tile.updateTile()
    }
}
