package com.example.ui.navigation

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.OptayApplication
import com.example.analytics.AnalyticsTracker
import com.example.core.firewall.ActiveSessionState
import com.example.domain.model.FocusMode
import com.example.ui.screens.ai.AIAssistantDialog
import com.example.ui.screens.apps.AppSelectionSheet
import com.example.ui.screens.apps.AppsPickerScreen
import com.example.ui.screens.billing.PaywallSheet
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.modes.CreateModeScreen
import com.example.ui.screens.modes.ModesScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.strict.EmergencyOverrideDialog
import com.example.ui.screens.strict.StrictModeEndDialog
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class OptayTab(val title: String) {
    FOCUS("Focus"),
    MODES("Modes"),
    APPS("Apps"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun OptayApp() {
    val app = OptayApplication.instance
    val context = LocalContext.current

    val isOnboardingCompleted by app.preferencesRepository.isOnboardingCompleted.collectAsState()
    val selectedModeId by app.preferencesRepository.selectedModeId.collectAsState()

    var currentTab by remember { mutableStateOf(OptayTab.FOCUS) }
    var isCreateModeOpen by remember { mutableStateOf(false) }
    var editingFocusMode by remember { mutableStateOf<FocusMode?>(null) }
    var isPaywallOpen by remember { mutableStateOf(false) }
    var isAIAssistantOpen by remember { mutableStateOf(false) }

    var strictSessionToEnd by remember { mutableStateOf<ActiveSessionState?>(null) }
    var isEmergencyOverrideOpen by remember { mutableStateOf(false) }
    var appSelectionTargetMode by remember { mutableStateOf<FocusMode?>(null) }

    // Pending mode start waiting for system VPN permission dialog
    var pendingModeToStart by remember { mutableStateOf<Pair<FocusMode, Int>?>(null) }

    val callRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Call screening role enabled.", Toast.LENGTH_SHORT).show()
        }
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Contacts permission helps identify work and emergency callers.", Toast.LENGTH_SHORT).show()
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingModeToStart?.let { (mode, duration) ->
                val started = app.firewallManager.startFocusMode(context, mode, duration)
                if (started) {
                    app.analyticsTracker.logEvent(AnalyticsTracker.EVENT_MODE_STARTED, mapOf("mode" to mode.name))
                    Toast.makeText(context, "${mode.name} boundary activated.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Optay needs VPN permission to restrict network access for selected apps.", Toast.LENGTH_LONG).show()
        }
        pendingModeToStart = null
    }

    val requestStartMode: (FocusMode, Int) -> Unit = { mode, duration ->
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            pendingModeToStart = mode to duration
            vpnLauncher.launch(vpnIntent)
        } else {
            val started = app.firewallManager.startFocusMode(context, mode, duration)
            if (started) {
                app.analyticsTracker.logEvent(AnalyticsTracker.EVENT_MODE_STARTED, mapOf("mode" to mode.name))
                Toast.makeText(context, "${mode.name} boundary activated.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // If onboarding not completed, show calm Onboarding sequence
    if (!isOnboardingCompleted) {
        OnboardingScreen(
            appDiscoveryRepository = app.appDiscoveryRepository,
            onCompleteOnboarding = { selectedPackages ->
                app.preferencesRepository.setOnboardingCompleted(true)
                app.analyticsTracker.logEvent(AnalyticsTracker.EVENT_ONBOARDING_COMPLETED)

                CoroutineScope(Dispatchers.IO).launch {
                    val offDay = app.focusModeRepository.getModeById("mode_off_day")
                    if (offDay != null) {
                        app.focusModeRepository.saveMode(offDay.copy(blockedApps = selectedPackages.toSet()))
                    }
                }
            },
            onRequestVpnPermission = {
                val vpnIntent = VpnService.prepare(context)
                if (vpnIntent != null) {
                    vpnLauncher.launch(vpnIntent)
                } else {
                    Toast.makeText(context, "VPN Permission already granted.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    // Main App Scaffold
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isCreateModeOpen && editingFocusMode == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    OptayTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        val icon = when (tab) {
                            OptayTab.FOCUS -> Icons.Default.Shield
                            OptayTab.MODES -> Icons.Default.Tune
                            OptayTab.APPS -> Icons.Default.GridView
                            OptayTab.INSIGHTS -> Icons.Default.BarChart
                            OptayTab.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isCreateModeOpen || editingFocusMode != null) {
                CreateModeScreen(
                    focusModeRepository = app.focusModeRepository,
                    appDiscoveryRepository = app.appDiscoveryRepository,
                    existingMode = editingFocusMode,
                    onNavigateBack = {
                        isCreateModeOpen = false
                        editingFocusMode = null
                    }
                )
            } else {
                when (currentTab) {
                    OptayTab.FOCUS -> DashboardScreen(
                        firewallManager = app.firewallManager,
                        focusModeRepository = app.focusModeRepository,
                        preferencesRepository = app.preferencesRepository,
                        revenueCatManager = app.revenueCatManager,
                        featureGate = app.featureGate,
                        onNavigateToApps = { currentTab = OptayTab.APPS },
                        onNavigateToModes = { currentTab = OptayTab.MODES },
                        onOpenAppSelection = { mode -> appSelectionTargetMode = mode },
                        onOpenAIAssistant = { isAIAssistantOpen = true },
                        onOpenPaywall = { isPaywallOpen = true },
                        onShowStrictModeDialog = { strictSession ->
                            strictSessionToEnd = strictSession
                        },
                        onShowEmergencyOverrideDialog = {
                            isEmergencyOverrideOpen = true
                        },
                        onTriggerStartFocus = requestStartMode
                    )
                    OptayTab.MODES -> ModesScreen(
                        focusModeRepository = app.focusModeRepository,
                        firewallManager = app.firewallManager,
                        preferencesRepository = app.preferencesRepository,
                        featureGate = app.featureGate,
                        onNavigateToCreate = { isCreateModeOpen = true },
                        onNavigateToAppsPicker = { modeId ->
                            app.preferencesRepository.setSelectedModeId(modeId)
                            currentTab = OptayTab.APPS
                        },
                        onOpenAppSelection = { mode -> appSelectionTargetMode = mode },
                        onEditMode = { mode -> editingFocusMode = mode },
                        onShowPaywall = { isPaywallOpen = true },
                        onActivateMode = { mode ->
                            requestStartMode(mode, mode.durationMinutes)
                        }
                    )
                    OptayTab.APPS -> AppsPickerScreen(
                        appDiscoveryRepository = app.appDiscoveryRepository,
                        focusModeRepository = app.focusModeRepository,
                        selectedModeId = selectedModeId
                    )
                    OptayTab.INSIGHTS -> InsightsScreen(
                        sessionHistoryDao = app.database.sessionHistoryDao()
                    )
                    OptayTab.SETTINGS -> SettingsScreen(
                        preferencesRepository = app.preferencesRepository,
                        revenueCatManager = app.revenueCatManager,
                        firebaseManager = app.firebaseManager,
                        focusModeRepository = app.focusModeRepository,
                        onShowPaywall = { isPaywallOpen = true }
                    )
                }
            }
        }

        // Modals & Dialogs
        if (isPaywallOpen) {
            PaywallSheet(
                revenueCatManager = app.revenueCatManager,
                onDismiss = { isPaywallOpen = false }
            )
        }

        if (isAIAssistantOpen) {
            AIAssistantDialog(
                aiProvider = app.aiProvider,
                onDismiss = { isAIAssistantOpen = false },
                onApplyAndStart = { parsedMode ->
                    isAIAssistantOpen = false
                    requestStartMode(parsedMode, parsedMode.durationMinutes)
                }
            )
        }

        if (strictSessionToEnd != null) {
            StrictModeEndDialog(
                strictModeType = strictSessionToEnd!!.strictModeType,
                onConfirmEnd = {
                    app.firewallManager.stopFocusMode(context, completedNaturally = false)
                    strictSessionToEnd = null
                },
                onDismiss = {
                    strictSessionToEnd = null
                }
            )
        }

        if (isEmergencyOverrideOpen) {
            EmergencyOverrideDialog(
                onSelectMinutes = { min ->
                    app.firewallManager.activateEmergencyOverride(context, min)
                    isEmergencyOverrideOpen = false
                    Toast.makeText(context, "Temporary access granted for $min minutes.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    isEmergencyOverrideOpen = false
                }
            )
        }

        appSelectionTargetMode?.let { targetMode ->
            AppSelectionSheet(
                targetMode = targetMode,
                appDiscoveryRepository = app.appDiscoveryRepository,
                focusModeRepository = app.focusModeRepository,
                onDismiss = { appSelectionTargetMode = null }
            )
        }
    }
}
