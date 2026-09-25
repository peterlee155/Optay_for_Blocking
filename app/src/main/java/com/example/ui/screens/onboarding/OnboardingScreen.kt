package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppDiscoveryRepository
import com.example.domain.model.InstalledApp
import com.example.ui.components.AppIconView
import com.example.ui.components.OptayBadge
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary

@Composable
fun OnboardingScreen(
    appDiscoveryRepository: AppDiscoveryRepository,
    onCompleteOnboarding: (List<String>) -> Unit,
    onRequestVpnPermission: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 8

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // Selected initial app packages state
    val selectedPackages = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        val apps = appDiscoveryRepository.getInstalledApps(showSystemApps = false)
        installedApps = apps
        // If nothing selected yet, select installed distraction apps
        if (selectedPackages.isEmpty()) {
            val defaultDistractions = apps.filter { it.isDistractionRecommended }.map { it.packageName }
            if (defaultDistractions.isNotEmpty()) {
                selectedPackages.addAll(defaultDistractions)
            } else if (apps.isNotEmpty()) {
                selectedPackages.add(apps.first().packageName)
            }
        }
        isLoadingApps = false
    }

    val selectedAppsSummary = remember(selectedPackages.toList(), installedApps) {
        installedApps.filter { selectedPackages.contains(it.packageName) }.map { it.appName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                0 -> StepWelcome()
                1 -> StepDistractionControl()
                2 -> StepPhoneStaysUseful()
                3 -> StepCallBoundaries()
                4 -> StepSelectApps(
                    installedApps = installedApps,
                    selectedPackages = selectedPackages,
                    isLoading = isLoadingApps
                )
                5 -> StepFirstMode(selectedApps = selectedAppsSummary)
                6 -> StepPermissionsEducation(onRequestVpnPermission)
                7 -> StepProTrial { onCompleteOnboarding(selectedPackages.toList()) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0 && step < 7) {
                TextButton(onClick = { step-- }) {
                    Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            if (step < totalSteps - 1) {
                Button(
                    onClick = { step++ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text("Continue", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onCompleteOnboarding(selectedPackages.toList()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("onboarding_finish_button")
                ) {
                    Text("Start Using Optay", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.Shield, color = CyanPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "OPTAY",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Disconnect without disconnecting your phone.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = CyanPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Keep your smartphone online while temporarily putting the apps, distractions, and work notifications you don't want to sleep.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepDistractionControl() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.Lock, color = IndigoSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Choose What Gets Your Attention",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Block distracting social feeds, endless reels, or work-related Slack and email during your time off.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OptayBadge(text = "Target Only Distractions • Keep Rest Online", color = IndigoSecondary)
    }
}

@Composable
private fun StepPhoneStaysUseful() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.Map, color = EmeraldSuccess)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Phone Stays Useful",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Unlike airplane mode, essential apps keep running with high-speed internet: Google Maps, Spotify, Banking, and Camera remain fully accessible.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OptayBadge(text = "Maps • Spotify • Banking • Camera Allowed", color = EmeraldSuccess)
    }
}

@Composable
private fun StepCallBoundaries() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.Phone, color = CyanPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Boundaries Around Calls",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Filter work numbers and unknown spam while letting family and emergency contacts reach you anytime.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepSelectApps(
    installedApps: List<InstalledApp>,
    selectedPackages: MutableList<String>,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Select Distraction Apps",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose which installed apps to disconnect during Off Day mode:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (installedApps.isEmpty()) {
            Text(
                text = "No third-party apps found on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                installedApps.forEach { app ->
                    val isSelected = selectedPackages.contains(app.packageName)
                    Surface(
                        onClick = {
                            if (isSelected) {
                                selectedPackages.remove(app.packageName)
                            } else {
                                selectedPackages.add(app.packageName)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIconView(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    category = app.category,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = app.category.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepFirstMode(selectedApps: List<String>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.BeachAccess, color = CyanPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your First Mode: Off Day",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OFF DAY MODE", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Blocked: ${if (selectedApps.isEmpty()) "None selected" else selectedApps.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = CoralBlocked
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Allowed: Google Maps, Spotify, Banking", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Calls: Work numbers silenced • Family allowed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Duration: Until morning 10:30 AM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepPermissionsEducation(onRequestVpn: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.VpnKey, color = CyanPrimary)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Contextual Permissions",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Optay runs completely on your device. We do not use external servers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Local VPN Firewall Permission",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optay uses Android's local VPN service strictly to restrict network traffic for chosen apps. No data is sent to external VPN servers and no traffic is decrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRequestVpn,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
            Text("Grant Local VPN Permission", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepProTrial(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(icon = Icons.Default.Star, color = CyanPrimary)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Ready to Power Down Distractions",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Optay is ready. You have 1 custom mode and full manual blocking free forever, with an optional Pro upgrade anytime.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroEmblem(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.05f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(42.dp)
        )
    }
}
