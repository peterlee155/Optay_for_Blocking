package com.example.ui.screens.modes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.Feature
import com.example.billing.FeatureGate
import com.example.core.firewall.FirewallManager
import com.example.data.repository.FocusModeRepository
import com.example.data.repository.PreferencesRepository
import com.example.domain.model.FocusMode
import com.example.ui.components.ModeIcon
import com.example.ui.components.OptayBadge
import com.example.ui.components.ProTag
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary
import kotlinx.coroutines.launch

@Composable
fun ModesScreen(
    focusModeRepository: FocusModeRepository,
    firewallManager: FirewallManager,
    preferencesRepository: PreferencesRepository,
    featureGate: FeatureGate,
    onNavigateToCreate: () -> Unit,
    onNavigateToAppsPicker: (String) -> Unit,
    onShowPaywall: () -> Unit,
    onActivateMode: (FocusMode) -> Unit,
    onOpenAppSelection: ((FocusMode) -> Unit)? = null,
    onEditMode: ((FocusMode) -> Unit)? = null
) {
    val modes by focusModeRepository.focusModes.collectAsState(initial = emptyList())
    val activeSession by firewallManager.activeSession.collectAsState()
    val selectedModeId by preferencesRepository.selectedModeId.collectAsState()
    val scope = rememberCoroutineScope()

    val customModesCount = modes.count { !it.isPredefined }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Focus Profiles",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${modes.size} modes available (${customModesCount} custom)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(modes, key = { it.id }) { mode ->
                    val isActive = activeSession?.modeId == mode.id
                    val isCurrentFavorite = selectedModeId == mode.id

                    ModeCard(
                        mode = mode,
                        isActive = isActive,
                        isCurrentFavorite = isCurrentFavorite,
                        onActivate = {
                            preferencesRepository.setSelectedModeId(mode.id)
                            onActivateMode(mode)
                        },
                        onConfigureApps = {
                            preferencesRepository.setSelectedModeId(mode.id)
                            if (onOpenAppSelection != null) {
                                onOpenAppSelection(mode)
                            } else {
                                onNavigateToAppsPicker(mode.id)
                            }
                        },
                        onEdit = {
                            onEditMode?.invoke(mode)
                        },
                        onDuplicate = {
                            if (featureGate.canUse(Feature.UNLIMITED_MODES, customModesCount)) {
                                scope.launch {
                                    focusModeRepository.duplicateMode(mode.id, "${mode.name} (Copy)")
                                }
                            } else {
                                onShowPaywall()
                            }
                        },
                        onDelete = {
                            if (!mode.isPredefined) {
                                scope.launch {
                                    focusModeRepository.deleteMode(mode.id)
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating action button for creating new custom mode
        FloatingActionButton(
            onClick = {
                if (featureGate.canUse(Feature.UNLIMITED_MODES, customModesCount)) {
                    onNavigateToCreate()
                } else {
                    onShowPaywall()
                }
            },
            containerColor = CyanPrimary,
            contentColor = Color(0xFF042F2E),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_mode_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Custom Mode")
        }
    }
}

@Composable
private fun ModeCard(
    mode: FocusMode,
    isActive: Boolean,
    isCurrentFavorite: Boolean,
    onActivate: () -> Unit,
    onConfigureApps: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(mode.colorHex))
    } catch (e: Exception) {
        CyanPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mode_card_${mode.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            if (isActive) 2.dp else 1.dp,
            if (isActive) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ModeIcon(iconName = mode.iconName, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mode.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrentFavorite && !isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                OptayBadge(text = "Primary", color = CyanPrimary)
                            }
                        }
                        if (mode.isStrictMode) {
                            Text(
                                text = "Strict Mode Enabled",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = accentColor
                            )
                        }
                    }
                }

                if (isActive) {
                    OptayBadge(text = "ACTIVE", color = EmeraldSuccess)
                } else {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        if (!mode.isPredefined) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata summary chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptayBadge(
                    text = "${mode.blockedApps.size} apps blocked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundColor = MaterialTheme.colorScheme.surface
                )
                OptayBadge(
                    text = mode.callBehavior.displayName.take(16),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundColor = MaterialTheme.colorScheme.surface
                )
                if (mode.schedule != null && mode.schedule.isEnabled) {
                    OptayBadge(
                        text = "Scheduled",
                        color = IndigoSecondary,
                        backgroundColor = IndigoSecondary.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onConfigureApps,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Edit Apps", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onActivate,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else CyanPrimary)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Shield else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onSurface else Color(0xFF042F2E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Managing" else "Start Mode",
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else Color(0xFF042F2E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
