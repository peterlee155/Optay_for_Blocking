package com.example.core.firewall

import android.content.Context
import android.net.VpnService
import android.util.Log
import com.example.data.db.ActiveSessionDao
import com.example.data.db.ActiveSessionEntity
import com.example.data.db.SessionHistoryDao
import com.example.data.db.SessionHistoryEntity
import com.example.domain.model.CallBehavior
import com.example.domain.model.FocusMode
import com.example.domain.model.StrictModeType
import java.util.concurrent.atomic.AtomicBoolean
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
    private val activeSessionDao: ActiveSessionDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    companion object {
        private const val TAG = "OptayFirewallManager"
    }

    private val _firewallStatus = MutableStateFlow(FirewallStatus.INACTIVE)
    val firewallStatus: StateFlow<FirewallStatus> = _firewallStatus.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSessionState?>(null)
    val activeSession: StateFlow<ActiveSessionState?> = _activeSession.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(-1L)
    val countdownSeconds: StateFlow<Long> = _countdownSeconds.asStateFlow()

    private val _statusErrorMessage = MutableStateFlow<String?>(null)
    val statusErrorMessage: StateFlow<String?> = _statusErrorMessage.asStateFlow()

    private var timerJob: Job? = null
    private val isFinishingSession = AtomicBoolean(false)

    init {
        // Observe VPN service real-time status and error states
        scope.launch {
            OptayVpnService.lastError.collect { errorMsg ->
                if (errorMsg != null && _firewallStatus.value == FirewallStatus.ACTIVE) {
                    _statusErrorMessage.value = errorMsg
                    _firewallStatus.value = FirewallStatus.ERROR
                }
            }
        }
    }

    fun isVpnPermissionRequired(context: Context): Boolean {
        return VpnService.prepare(context) != null
    }

    fun restorePersistedSession(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val persisted = activeSessionDao.getActiveSession() ?: return@launch
                val now = System.currentTimeMillis()

                // Check whether persisted session expired during process absence or reboot
                if (persisted.endTargetTimeMillis in 1..now) {
                    Log.i(TAG, "Persisted session expired while process was down. Finalizing cleanly.")
                    val durationMin = ((persisted.endTargetTimeMillis - persisted.startTimeMillis) / 60000)
                        .coerceAtLeast(1).toInt()
                    sessionHistoryDao.insert(
                        SessionHistoryEntity(
                            sessionId = persisted.sessionId,
                            modeId = persisted.modeId,
                            modeName = persisted.modeName,
                            startTimeMillis = persisted.startTimeMillis,
                            endTimeMillis = persisted.endTargetTimeMillis,
                            plannedDurationMinutes = durationMin,
                            durationMinutes = durationMin,
                            blockedAppsCount = persisted.blockedPackagesJson.split(",").filter { it.isNotBlank() }.size,
                            completedNaturally = true,
                            endedEarly = false,
                            emergencyOverrideUsed = persisted.emergencyOverrideActive
                        )
                    )
                    activeSessionDao.clearActiveSession()
                    return@launch
                }

                // If still within valid time range and firewall was expected active:
                if (persisted.firewallExpectedActive) {
                    val blockedSet = persisted.blockedPackagesJson.split(",").filter { it.isNotBlank() }.toSet()
                    val allowedSet = persisted.allowedPackagesJson.split(",").filter { it.isNotBlank() }.toSet()
                    val callBeh = try { CallBehavior.valueOf(persisted.callBehavior) } catch (e: Exception) { CallBehavior.ALLOW_ALL }
                    val strictType = try { StrictModeType.valueOf(persisted.strictModeType) } catch (e: Exception) { StrictModeType.CONFIRMATION_DELAY_10S }

                    val sessionState = ActiveSessionState(
                        sessionId = persisted.sessionId,
                        modeId = persisted.modeId,
                        modeName = persisted.modeName,
                        blockedApps = blockedSet,
                        allowedApps = allowedSet,
                        callBehavior = callBeh,
                        startTimeMillis = persisted.startTimeMillis,
                        endTargetTimeMillis = persisted.endTargetTimeMillis,
                        isStrictMode = persisted.isStrictMode,
                        strictModeType = strictType,
                        isEmergencyOverrideActive = persisted.emergencyOverrideActive,
                        emergencyOverrideEndTimeMillis = persisted.emergencyOverrideEndTimeMillis
                    )

                    scope.launch(Dispatchers.Main) {
                        _activeSession.value = sessionState

                        if (isVpnPermissionRequired(context)) {
                            Log.w(TAG, "VPN permission required after reboot. Awaiting user consent.")
                            _firewallStatus.value = FirewallStatus.ERROR
                            _statusErrorMessage.value = "VPN permission requires verification."
                        } else {
                            _firewallStatus.value = FirewallStatus.ACTIVE
                            if (!sessionState.isEmergencyOverrideActive) {
                                OptayVpnService.start(
                                    context = context,
                                    modeId = sessionState.modeId,
                                    modeName = sessionState.modeName,
                                    blockedPackages = ArrayList(sessionState.blockedApps),
                                    allowedPackages = ArrayList(sessionState.allowedApps),
                                    endTimeMillis = sessionState.endTargetTimeMillis
                                )
                            }
                            startTimer(context, sessionState)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring persisted active session", e)
            }
        }
    }

    fun startFocusMode(context: Context, mode: FocusMode, durationMinutes: Int = mode.durationMinutes): Boolean {
        if (isVpnPermissionRequired(context)) {
            _firewallStatus.value = FirewallStatus.INACTIVE
            _statusErrorMessage.value = null
            return false
        }

        _firewallStatus.value = FirewallStatus.STARTING
        _statusErrorMessage.value = null

        val startTime = System.currentTimeMillis()
        val endTime = if (durationMinutes > 0) startTime + (durationMinutes * 60 * 1000L) else 0L
        val sessionId = "session_${System.currentTimeMillis()}"

        val session = ActiveSessionState(
            sessionId = sessionId,
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
        isFinishingSession.set(false)

        // Persist to Room so state survives process death and device restarts
        scope.launch(Dispatchers.IO) {
            activeSessionDao.setActiveSession(
                ActiveSessionEntity(
                    sessionId = sessionId,
                    modeId = mode.id,
                    modeName = mode.name,
                    blockedPackagesJson = mode.blockedApps.joinToString(","),
                    allowedPackagesJson = mode.allowedApps.joinToString(","),
                    callBehavior = mode.callBehavior.name,
                    isStrictMode = mode.isStrictMode,
                    strictModeType = mode.strictModeType.name,
                    startTimeMillis = startTime,
                    endTargetTimeMillis = endTime,
                    emergencyOverrideActive = false,
                    emergencyOverrideEndTimeMillis = 0L,
                    firewallExpectedActive = true,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        }

        // Start local black-hole VPN service
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
        if (isFinishingSession.getAndSet(true)) {
            // Prevent duplicate closing or double history inserts
            return
        }

        val session = _activeSession.value
        _firewallStatus.value = FirewallStatus.STOPPING
        timerJob?.cancel()
        timerJob = null

        OptayVpnService.stop(context)
        _firewallStatus.value = FirewallStatus.INACTIVE
        _countdownSeconds.value = -1L
        _activeSession.value = null

        // Clear active session and record to session_history atomically
        scope.launch(Dispatchers.IO) {
            try {
                activeSessionDao.clearActiveSession()

                if (session != null) {
                    val alreadyInserted = sessionHistoryDao.hasSession(session.sessionId) > 0
                    if (!alreadyInserted) {
                        val endTime = System.currentTimeMillis()
                        val durationMinutes = ((endTime - session.startTimeMillis) / 60000).coerceAtLeast(1).toInt()
                        val plannedMin = if (session.endTargetTimeMillis > session.startTimeMillis) {
                            ((session.endTargetTimeMillis - session.startTimeMillis) / 60000).toInt()
                        } else 0

                        sessionHistoryDao.insert(
                            SessionHistoryEntity(
                                sessionId = session.sessionId,
                                modeId = session.modeId,
                                modeName = session.modeName,
                                startTimeMillis = session.startTimeMillis,
                                endTimeMillis = endTime,
                                plannedDurationMinutes = plannedMin,
                                durationMinutes = durationMinutes,
                                blockedAppsCount = session.blockedApps.size,
                                completedNaturally = completedNaturally,
                                endedEarly = !completedNaturally,
                                emergencyOverrideUsed = session.isEmergencyOverrideActive
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing session history", e)
            } finally {
                isFinishingSession.set(false)
            }
        }
    }

    fun activateEmergencyOverride(context: Context, overrideMinutes: Int) {
        val current = _activeSession.value ?: return
        val overrideEnd = System.currentTimeMillis() + (overrideMinutes * 60 * 1000L)
        val updated = current.copy(
            isEmergencyOverrideActive = true,
            emergencyOverrideEndTimeMillis = overrideEnd
        )
        _activeSession.value = updated
        _firewallStatus.value = FirewallStatus.OVERRIDE_ACTIVE

        // Update database persistence
        scope.launch(Dispatchers.IO) {
            val entity = activeSessionDao.getActiveSession()
            if (entity != null) {
                activeSessionDao.setActiveSession(
                    entity.copy(
                        emergencyOverrideActive = true,
                        emergencyOverrideEndTimeMillis = overrideEnd,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        // Temporarily relieve firewall restriction
        OptayVpnService.stop(context)
    }

    fun endEmergencyOverride(context: Context) {
        val current = _activeSession.value ?: return
        val updated = current.copy(
            isEmergencyOverrideActive = false,
            emergencyOverrideEndTimeMillis = 0L
        )
        _activeSession.value = updated
        _firewallStatus.value = FirewallStatus.ACTIVE

        scope.launch(Dispatchers.IO) {
            val entity = activeSessionDao.getActiveSession()
            if (entity != null) {
                activeSessionDao.setActiveSession(
                    entity.copy(
                        emergencyOverrideActive = false,
                        emergencyOverrideEndTimeMillis = 0L,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        // Re-enable local black-hole firewall
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
            while (isActive && (_firewallStatus.value == FirewallStatus.ACTIVE || _firewallStatus.value == FirewallStatus.OVERRIDE_ACTIVE)) {
                val now = System.currentTimeMillis()
                val currentSession = _activeSession.value ?: break

                // Check emergency override expiration
                if (currentSession.isEmergencyOverrideActive) {
                    if (now >= currentSession.emergencyOverrideEndTimeMillis) {
                        endEmergencyOverride(context)
                    }
                }

                // Wall-clock target time is the single source of truth
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
