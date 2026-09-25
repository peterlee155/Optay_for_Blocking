package com.example.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.RevenueCatManager
import com.example.billing.SubscriptionTier
import com.example.data.repository.PreferencesRepository
import com.example.ui.components.OptayBadge
import com.example.ui.components.ProTag
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary

@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    revenueCatManager: RevenueCatManager,
    onShowPaywall: () -> Unit
) {
    val isRestoreOnBoot by preferencesRepository.isRestoreOnBootEnabled.collectAsState()
    val isProActive by revenueCatManager.isProActive.collectAsState()
    var restoreFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings & Privacy",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Optay local firewall configuration and platform controls",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Subscription Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProActive) CyanPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, if (isProActive) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isProActive) "Optay Pro Active" else "Optay Free Plan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (isProActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ProTag()
                                }
                            }
                            Text(
                                text = if (isProActive) "Unlimited modes, AI assistant, and call filtering" else "1 custom mode • Manual blocking • Basic timer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isProActive) {
                        Button(
                            onClick = onShowPaywall,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Upgrade to Pro", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                revenueCatManager.setProStatus(false)
                                restoreFeedback = "Switched to Free tier preview."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Switch to Free Preview")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            revenueCatManager.restorePurchases { success, msg ->
                                restoreFeedback = msg
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restore Purchases")
                    }
                }

                if (restoreFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = restoreFeedback ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System & Firewall Section
        Text(
            text = "Firewall & System",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Restore Active Mode on Boot", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Resume scheduled focus profile after device restarts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isRestoreOnBoot,
                        onCheckedChange = { preferencesRepository.setRestoreOnBootEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Battery Optimization Warning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Battery Optimization Guidance", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To ensure Android doesn't kill Optay's local firewall service during deep sleep, allow 'Unrestricted' battery access in App Info > Battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Manifesto
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optay Privacy Manifesto", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Optay never routes your network traffic through remote servers.\n" +
                        "• Optay never decrypts HTTPS / TLS data.\n" +
                        "• Optay never reads chat contents, browsing history, or message payloads.\n" +
                        "• All allow/block boundaries are enforced locally on your Android device.\n" +
                        "• Emergency phone calls are never blocked or screened.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cross-platform Parity Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cross-Platform Architecture Parity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Android: Local VpnService virtual interface + Telecom CallScreeningService.\n\n" +
                        "iOS: Screen Time FamilyControls + ManagedSettings shield UI + DeviceActivity scheduling + CallKit Call Directory.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
