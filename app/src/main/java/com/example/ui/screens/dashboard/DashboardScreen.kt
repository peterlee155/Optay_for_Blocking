package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.Feature
import com.example.billing.FeatureGate
import com.example.billing.RevenueCatManager
import com.example.core.firewall.ActiveSessionState
import com.example.core.firewall.FirewallManager
import com.example.core.firewall.FirewallStatus
import com.example.data.repository.FocusModeRepository
import com.example.data.repository.PreferencesRepository
import com.example.domain.model.FocusMode
import com.example.ui.components.AppIconView
import com.example.ui.components.ModeIcon
import com.example.ui.components.OptayBadge
import com.example.ui.components.ProTag
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    firewallManager: FirewallManager,
    focusModeRepository: FocusModeRepository,
    preferencesRepository: PreferencesRepository,
    revenueCatManager: RevenueCatManager,
    featureGate: FeatureGate,
    onNavigateToApps: () -> Unit,
    onNavigateToModes: () -> Unit,
    onOpenAppSelection: (FocusMode) -> Unit,
    onOpenAIAssistant: () -> Unit,
    onOpenPaywall: () -> Unit,
    onShowStrictModeDialog: (ActiveSessionState) -> Unit,
    onShowEmergencyOverrideDialog: () -> Unit,
    onTriggerStartFocus: (FocusMode, Int) -> Unit
) {
    val firewallStatus by firewallManager.firewallStatus.collectAsState()
    val activeSession by firewallManager.activeSession.collectAsState()
    val countdownSeconds by firewallManager.countdownSeconds.collectAsState()

    val modes by focusModeRepository.focusModes.collectAsState(initial = emptyList())
    val selectedModeId by preferencesRepository.selectedModeId.collectAsState()
    val isProActive by revenueCatManager.isProActive.collectAsState()

    // Find currently selected favorite mode (default to Off Day)
    val selectedMode = modes.find { it.id == selectedModeId } ?: modes.firstOrNull()

    // Duration options
    var selectedDurationMinutes by remember { mutableIntStateOf(120) }
    var isCustomDurationDialogOpen by remember { mutableStateOf(false) }

    val isActive = firewallStatus == FirewallStatus.ACTIVE && activeSession != null

    if (isCustomDurationDialogOpen) {
        com.example.ui.components.CustomDurationDialog(
            initialMinutes = selectedDurationMinutes,
            onDismiss = { isCustomDurationDialogOpen = false },
            onConfirm = { customMinutes ->
                selectedDurationMinutes = customMinutes
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OPTAY",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isProActive) {
                        ProTag()
                    } else {
                        OptayBadge(text = "Free Plan", color = CyanPrimary)
                    }
                }
                Text(
                    text = "Disconnect without disconnecting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // AI Assistant Trigger
                IconButton(
                    onClick = {
                        if (featureGate.canUse(Feature.AI_COMMANDS)) {
                            onOpenAIAssistant()
                        } else {
                            onOpenPaywall()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("open_ai_assistant_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!isProActive) {
                    Surface(
                        onClick = onOpenPaywall,
                        shape = RoundedCornerShape(20.dp),
                        color = IndigoSecondary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, IndigoSecondary.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("upgrade_pro_button")
                    ) {
                        Text(
                            text = "Upgrade",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = IndigoSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HERO CARD: ONE-TAP ACTIVATION & LIVE STATUS
        if (isActive && activeSession != null) {
            ActiveSessionHeroCard(
                session = activeSession!!,
                countdownSeconds = countdownSeconds,
                onEndMode = {
                    if (activeSession!!.isStrictMode) {
                        onShowStrictModeDialog(activeSession!!)
                    } else {
                        firewallManager.stopFocusMode(com.example.OptayApplication.instance)
                    }
                },
                onViewBlockedApps = {
                    if (selectedMode != null) onOpenAppSelection(selectedMode) else onNavigateToApps()
                },
                onEmergencyOverride = onShowEmergencyOverrideDialog
            )
        } else if (selectedMode != null) {
            InactiveModeHeroCard(
                mode = selectedMode,
                selectedDuration = selectedDurationMinutes,
                onDurationChange = { selectedDurationMinutes = it },
                onOpenCustomDuration = { isCustomDurationDialogOpen = true },
                onStartMode = {
                    onTriggerStartFocus(selectedMode, selectedDurationMinutes)
                },
                onConfigureApps = {
                    onOpenAppSelection(selectedMode)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK MODES SWITCHER CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Profiles",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onNavigateToModes) {
                Text("See All (${modes.size})", style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(modes.take(5)) { mode ->
                val isSelected = selectedModeId == mode.id
                QuickModeItemCard(
                    mode = mode,
                    isSelected = isSelected,
                    onClick = {
                        preferencesRepository.setSelectedModeId(mode.id)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PRIVACY & ENGINE STATUS SUMMARY
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Firewall Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Operates entirely on this device (No external VPN proxy)\n" +
                        "• Zero traffic decryption & zero packet inspection\n" +
                        "• Essential apps (Maps, Spotify, Banking) keep direct high-speed internet",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ActiveSessionHeroCard(
    session: ActiveSessionState,
    countdownSeconds: Long,
    onEndMode: () -> Unit,
    onViewBlockedApps: () -> Unit,
    onEmergencyOverride: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            2.dp,
            Brush.linearGradient(listOf(CyanPrimary, IndigoSecondary))
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulse Badge
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(CyanPrimary.copy(alpha = 0.35f), Color.Transparent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OPTAY ACTIVE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = CyanPrimary
            )

            Text(
                text = session.modeName.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Restrictions summary
            Text(
                text = "${session.blockedApps.size} apps offline • ${session.callBehavior.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display Blocked App Icons directly
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(session.blockedApps.toList().take(6)) { pkg ->
                    AppIconView(
                        packageName = pkg,
                        appName = pkg.substringAfterLast('.'),
                        size = 36.dp
                    )
                }
                if (session.blockedApps.size > 6) {
                    item {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, CoralBlocked.copy(alpha = 0.4f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+${session.blockedApps.size - 6}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CoralBlocked)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown Timer Display
            val formattedTime = if (session.endTargetTimeMillis <= 0) "Indefinite" else session.formatRemainingTime()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (session.endTargetTimeMillis > 0) "remaining until auto-restore" else "active until you end",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Emergency Override active banner
            if (session.isEmergencyOverrideActive) {
                Spacer(modifier = Modifier.height(12.dp))
                OptayBadge(
                    text = "EMERGENCY OVERRIDE PAUSE ACTIVE",
                    color = AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // End Mode button
            Button(
                onClick = onEndMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("end_mode_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralBlocked)
            ) {
                Text(
                    text = if (session.isStrictMode) "End Mode (Strict Friction)" else "End Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewBlockedApps,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("View / Edit Apps", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onEmergencyOverride,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Emergency Access", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InactiveModeHeroCard(
    mode: FocusMode,
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit,
    onOpenCustomDuration: () -> Unit,
    onStartMode: () -> Unit,
    onConfigureApps: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(mode.colorHex))
    } catch (e: Exception) {
        CyanPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inactive_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ModeIcon(iconName = mode.iconName, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = mode.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${mode.blockedApps.size} apps will go offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralBlocked
                        )
                    }
                }

                Button(
                    onClick = onConfigureApps,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary.copy(alpha = 0.15f))
                ) {
                    Text("Choose Apps", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual row of apps that will go offline
            Text(
                text = "Targeted Blocklist:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfigureApps() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mode.blockedApps.toList().take(5).forEach { pkg ->
                    AppIconView(
                        packageName = pkg,
                        appName = pkg.substringAfterLast('.'),
                        size = 36.dp
                    )
                }
                if (mode.blockedApps.size > 5) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, CoralBlocked.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+${mode.blockedApps.size - 5}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CoralBlocked)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details rows
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calls: ${mode.callBehavior.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Allowed: Google Maps, Spotify, Banking remain online",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration selector header with custom time action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Duration:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onOpenCustomDuration,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Set Custom Time",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val standardPresets = listOf(
                15 to "15m",
                60 to "1h",
                120 to "2h",
                240 to "4h",
                480 to "8h",
                720 to "Until Tomorrow",
                0 to "Indefinite"
            )

            // If user set a custom duration not in presets, display it prominently
            val isCustomDuration = standardPresets.none { it.first == selectedDuration }
            val customLabel = if (selectedDuration <= 0) "Indefinite"
            else if (selectedDuration % 60 == 0) "${selectedDuration / 60}h"
            else if (selectedDuration > 60) "${selectedDuration / 60}h ${selectedDuration % 60}m"
            else "${selectedDuration}m"

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCustomDuration) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = onOpenCustomDuration,
                            label = { Text("Custom ($customLabel)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = CyanPrimary
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanPrimary)
                            }
                        )
                    }
                }

                items(standardPresets) { (min, label) ->
                    val isSelected = selectedDuration == min
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDurationChange(min) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = onOpenCustomDuration,
                        label = { Text("+ Custom...", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = CyanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PRIMARY ONE-TAP BUTTON: START MODE
            val durationTag = when {
                selectedDuration <= 0 -> "Indefinite"
                selectedDuration % 60 == 0 -> "${selectedDuration / 60}h"
                selectedDuration > 60 -> "${selectedDuration / 60}h ${selectedDuration % 60}m"
                else -> "${selectedDuration}m"
            }

            Button(
                onClick = onStartMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_mode_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color(0xFF042F2E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START ${mode.name.uppercase()} • $durationTag",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color(0xFF042F2E)
                )
            }
        }
    }
}

@Composable
private fun QuickModeItemCard(
    mode: FocusMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(mode.colorHex))
    } catch (e: Exception) {
        CyanPrimary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(136.dp)
            .testTag("quick_mode_${mode.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                ModeIcon(iconName = mode.iconName, tint = accentColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mode.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = "${mode.blockedApps.size} apps offline",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
