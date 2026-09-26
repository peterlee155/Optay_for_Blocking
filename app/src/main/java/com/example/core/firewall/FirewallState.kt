package com.example.core.firewall

import com.example.domain.model.CallBehavior
import com.example.domain.model.StrictModeType

enum class FirewallStatus {
    INACTIVE,
    PREPARING,
    STARTING,
    ACTIVE,
    STOPPING,
    COMPLETED,
    ERROR,
    OVERRIDE_ACTIVE
}

data class ActiveSessionState(
    val sessionId: String = "session_${System.currentTimeMillis()}",
    val modeId: String,
    val modeName: String,
    val iconName: String = "shield",
    val colorHex: String = "#06B6D4",
    val blockedApps: Set<String>,
    val allowedApps: Set<String>,
    val callBehavior: CallBehavior,
    val startTimeMillis: Long,
    val endTargetTimeMillis: Long, // 0 for infinite/manual
    val isStrictMode: Boolean,
    val strictModeType: StrictModeType,
    val isEmergencyOverrideActive: Boolean = false,
    val emergencyOverrideEndTimeMillis: Long = 0L
) {
    fun isExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (endTargetTimeMillis <= 0) return false
        return currentTimeMillis >= endTargetTimeMillis
    }

    fun remainingSeconds(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (endTargetTimeMillis <= 0) return -1
        val diff = (endTargetTimeMillis - currentTimeMillis) / 1000
        return if (diff < 0) 0 else diff
    }

    fun formatRemainingTime(currentTimeMillis: Long = System.currentTimeMillis()): String {
        val sec = remainingSeconds(currentTimeMillis)
        if (sec < 0) return "Indefinite"
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
