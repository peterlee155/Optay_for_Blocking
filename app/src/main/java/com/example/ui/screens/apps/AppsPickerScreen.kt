package com.example.ui.screens.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.FocusModeRepository
import com.example.domain.model.AppCategory
import com.example.domain.model.InstalledApp
import com.example.ui.components.AppIconView
import com.example.ui.components.ModeIcon
import com.example.ui.components.OptayBadge
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsPickerScreen(
    appDiscoveryRepository: AppDiscoveryRepository,
    focusModeRepository: FocusModeRepository,
    selectedModeId: String,
    onNavigateBack: (() -> Unit)? = null
) {
    val modes by focusModeRepository.focusModes.collectAsState(initial = emptyList())
    var currentTargetModeId by remember(selectedModeId) { mutableStateOf(selectedModeId) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var showSystemApps by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var blockedPackageSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    val currentMode = modes.find { it.id == currentTargetModeId } ?: modes.firstOrNull()

    LaunchedEffect(showSystemApps) {
        isLoading = true
        val apps = appDiscoveryRepository.getInstalledApps(showSystemApps)
        installedApps = apps
        isLoading = false
    }

    LaunchedEffect(currentTargetModeId, modes) {
        currentMode?.let {
            blockedPackageSet = it.blockedApps
        }
    }

    // Filter logic
    val filteredApps = remember(installedApps, searchQuery, selectedCategory, blockedPackageSet) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val isBlocked = blockedPackageSet.contains(app.packageName)
            val matchesCategory = when (selectedCategory) {
                AppCategory.ALL -> true
                AppCategory.BLOCKED -> isBlocked
                AppCategory.ALLOWED -> !isBlocked
                else -> app.category == selectedCategory
            }

            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and Mode Target
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Boundaries",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Choose which installed apps to disconnect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OptayBadge(
                text = "${blockedPackageSet.size} BLOCKED",
                color = CoralBlocked
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Switcher Pills: Allows user to choose apps for any mode!
        Text(
            text = "Configuring Mode:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(modes) { mode ->
                val isSelected = mode.id == currentTargetModeId
                Surface(
                    onClick = { currentTargetModeId = mode.id },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeIcon(iconName = mode.iconName, tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${mode.blockedApps.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps (WhatsApp, Instagram, Maps...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Bulk Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val distractionPkgs = installedApps.filter { it.isDistractionRecommended }.map { it.packageName }.toSet()
                    val newSet = blockedPackageSet + distractionPkgs
                    blockedPackageSet = newSet
                    currentMode?.let {
                        scope.launch {
                            focusModeRepository.saveMode(it.copy(blockedApps = newSet))
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralBlocked.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("select_all_distractions_button")
            ) {
                Icon(Icons.Default.Block, contentDescription = null, tint = CoralBlocked, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Distractions", color = CoralBlocked, style = MaterialTheme.typography.labelMedium)
            }

            Row {
                TextButton(
                    onClick = {
                        val allPkgs = installedApps.map { it.packageName }.toSet()
                        blockedPackageSet = allPkgs
                        currentMode?.let {
                            scope.launch {
                                focusModeRepository.saveMode(it.copy(blockedApps = allPkgs))
                            }
                        }
                    }
                ) {
                    Text("Select All", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                }

                TextButton(
                    onClick = {
                        blockedPackageSet = emptySet()
                        currentMode?.let {
                            scope.launch {
                                focusModeRepository.saveMode(it.copy(blockedApps = emptySet()))
                            }
                        }
                    }
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        val categories = listOf(
            AppCategory.ALL,
            AppCategory.BLOCKED,
            AppCategory.ALLOWED,
            AppCategory.SOCIAL,
            AppCategory.MESSAGING,
            AppCategory.ENTERTAINMENT,
            AppCategory.WORK,
            AppCategory.GAMES
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("chip_category_${cat.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // App List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isBlocked = blockedPackageSet.contains(app.packageName)
                    AppSelectRowCard(
                        app = app,
                        isBlocked = isBlocked,
                        onToggle = { newBlockedState ->
                            val newSet = if (newBlockedState) {
                                blockedPackageSet + app.packageName
                            } else {
                                blockedPackageSet - app.packageName
                            }
                            blockedPackageSet = newSet
                            currentMode?.let {
                                scope.launch {
                                    focusModeRepository.saveMode(it.copy(blockedApps = newSet))
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show System Applications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = showSystemApps,
                            onCheckedChange = { showSystemApps = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun AppSelectRowCard(
    app: InstalledApp,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isBlocked) }
            .testTag("app_row_${app.packageName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) CoralBlocked.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            if (isBlocked) CoralBlocked.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real App Icon / Brand Emblem
            AppIconView(
                packageName = app.packageName,
                appName = app.appName,
                category = app.category,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (app.isDistractionRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OptayBadge(text = "Distraction", color = CoralBlocked)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${app.category.title} • ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Interactive Toggle Button with status
            Surface(
                onClick = { onToggle(!isBlocked) },
                shape = RoundedCornerShape(10.dp),
                color = if (isBlocked) CoralBlocked else EmeraldSuccess.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (isBlocked) CoralBlocked else EmeraldSuccess.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isBlocked) Color.White else EmeraldSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBlocked) "BLOCKED" else "ALLOWED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isBlocked) Color.White else EmeraldSuccess
                    )
                }
            }
        }
    }
}
