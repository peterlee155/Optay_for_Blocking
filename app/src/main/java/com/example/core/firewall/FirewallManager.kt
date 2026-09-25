package com.example.core.firewall

import android.content.Context
import android.net.VpnService
import com.example.data.db.SessionHistoryDao
import com.example.data.db.SessionHistoryEntity
import com.example.domain.model.FocusMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FirewallManager(
    private val sessionHistoryDao: SessionHistoryDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _firewallStatus = MutableStateFlow(FirewallStatus.INACTIVE)
    val firewallStatus: StateFlow<FirewallStatus> = _firewallStatus.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSessionState?>(null)
    val activeSession: StateFlow<ActiveSessionState?> = _activeSession.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(-1L)
    val countdownSeconds: StateFlow<Long> = _countdownSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun isVpnPermissionRequired(context: Context): Boolean {
        return VpnService.prepare(context) != null
    }

    fun startFocusMode(context: Context, mode: FocusMode, durationMinutes: Int = mode.durationMinutes): Boolean {
        // If system requires VPN permission first, caller should check isVpnPermissionRequired
        if (isVpnPermissionRequired(context)) {
            return false
        }

        val startTime = System.currentTimeMillis()
        val endTime = if (durationMinutes > 0) startTime + (durationMinutes * 60 * 1000L) else 0L

        val session = ActiveSessionState(
            modeId = mode.id,
            modeName = mode.name,
            iconName = mode.iconName,
            colorHex = mode.colorHex,
            blockedApps = mode.blockedApps,
            allowedApps = mode.allowedApps,
            callBehavior = mode.callBehavior,
            startTimeMillis = startTime,
            endTargetTimeMillis = endTime,
            isStrictMode = mode.isStrictMode,
            strictModeType = mode.strictModeType
        )

        _activeSession.value = session
        _firewallStatus.value = FirewallStatus.ACTIVE

        // Start local VPN Service
        OptayVpnService.start(
            context = context,
            modeId = mode.id,
            modeName = mode.name,
            blockedPackages = ArrayList(mode.blockedApps),
            allowedPackages = ArrayList(mode.allowedApps),
            endTimeMillis = endTime
        )

        startTimer(context, session)
        return true
    }

    fun stopFocusMode(context: Context, completedNaturally: Boolean = false) {
        val session = _activeSession.value
        timerJob?.cancel()
        timerJob = null

        OptayVpnService.stop(context)
        _firewallStatus.value = FirewallStatus.INACTIVE
        _countdownSeconds.value = -1L
        _activeSession.value = null

        if (session != null) {
            val endTime = System.currentTimeMillis()
            val durationMinutes = ((endTime - session.startTimeMillis) / 60000).coerceAtLeast(1).toInt()
            scope.launch(Dispatchers.IO) {
                sessionHistoryDao.insert(
                    SessionHistoryEntity(
                        modeName = session.modeName,
                        startTimeMillis = session.startTimeMillis,
                        endTimeMillis = endTime,
                        durationMinutes = durationMinutes,
                        blockedAppsCount = session.blockedApps.size,
                        completedNaturally = completedNaturally
                    )
                )
            }
        }
    }

    fun activateEmergencyOverride(context: Context, overrideMinutes: Int) {
        val current = _activeSession.value ?: return
        val overrideEnd = System.currentTimeMillis() + (overrideMinutes * 60 * 1000L)
        _activeSession.value = current.copy(
            isEmergencyOverrideActive = true,
            emergencyOverrideEndTimeMillis = overrideEnd
        )
        // Temporarily relieve firewall restriction
        OptayVpnService.stop(context)
    }

    fun endEmergencyOverride(context: Context) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            isEmergencyOverrideActive = false,
            emergencyOverrideEndTimeMillis = 0L
        )
        // Re-enable firewall restriction
        OptayVpnService.start(
            context = context,
            modeId = current.modeId,
            modeName = current.modeName,
            blockedPackages = ArrayList(current.blockedApps),
            allowedPackages = ArrayList(current.allowedApps),
            endTimeMillis = current.endTargetTimeMillis
        )
    }

    private fun startTimer(context: Context, session: ActiveSessionState) {
        timerJob?.cancel()
        if (session.endTargetTimeMillis <= 0) {
            _countdownSeconds.value = -1L
            return
        }

        timerJob = scope.launch {
            while (isActive && _firewallStatus.value == FirewallStatus.ACTIVE) {
                val now = System.currentTimeMillis()
                val currentSession = _activeSession.value ?: break

                // Check emergency override expiration
                if (currentSession.isEmergencyOverrideActive) {
                    if (now >= currentSession.emergencyOverrideEndTimeMillis) {
                        endEmergencyOverride(context)
                    }
                }

                if (session.isExpired(now)) {
                    stopFocusMode(context, completedNaturally = true)
                    break
                } else {
                    _countdownSeconds.value = session.remainingSeconds(now)
                }
                delay(1000)
            }
        }
    }
}
