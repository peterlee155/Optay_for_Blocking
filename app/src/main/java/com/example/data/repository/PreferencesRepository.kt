package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("optay_user_prefs", Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _selectedModeId = MutableStateFlow(prefs.getString(KEY_SELECTED_MODE, "mode_off_day") ?: "mode_off_day")
    val selectedModeId: StateFlow<String> = _selectedModeId.asStateFlow()

    private val _isRestoreOnBootEnabled = MutableStateFlow(prefs.getBoolean(KEY_RESTORE_BOOT, true))
    val isRestoreOnBootEnabled: StateFlow<Boolean> = _isRestoreOnBootEnabled.asStateFlow()

    private val _showSystemApps = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false))
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setSelectedModeId(modeId: String) {
        prefs.edit().putString(KEY_SELECTED_MODE, modeId).apply()
        _selectedModeId.value = modeId
    }

    fun setRestoreOnBootEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RESTORE_BOOT, enabled).apply()
        _isRestoreOnBootEnabled.value = enabled
    }

    fun setShowSystemApps(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, show).apply()
        _showSystemApps.value = show
    }

    companion object {
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_SELECTED_MODE = "key_selected_mode"
        private const val KEY_RESTORE_BOOT = "key_restore_boot"
        private const val KEY_SHOW_SYSTEM_APPS = "key_show_system_apps"
    }
}
