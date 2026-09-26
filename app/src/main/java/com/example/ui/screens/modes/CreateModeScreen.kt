package com.example.ui.screens.modes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.FocusModeRepository
import com.example.domain.model.AppCategory
import com.example.domain.model.CallBehavior
import com.example.domain.model.FocusMode
import com.example.domain.model.InstalledApp
import com.example.domain.model.StrictModeType
import com.example.ui.components.ModeIcon
import com.example.ui.components.OptayBadge
import com.example.ui.screens.apps.AppSelectRowCard
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.launch

data class ProfileTemplate(
    val name: String,
    val iconName: String,
    val colorHex: String,
    val description: String,
    val durationMinutes: Int,
    val callBehavior: CallBehavior,
    val targetCategories: Set<AppCategory>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateModeScreen(
    focusModeRepository: FocusModeRepository,
    appDiscoveryRepository: AppDiscoveryRepository,
    existingMode: FocusMode? = null,
    onNavigateBack: () -> Unit
) {
    val isEditing = existingMode != null
    val context = LocalContext.current

    var modeName by remember(existingMode) { mutableStateOf(existingMode?.name ?: "") }
    var modeDescription by remember(existingMode) { mutableStateOf(existingMode?.description ?: "") }
    var selectedIcon by remember(existingMode) { mutableStateOf(existingMode?.iconName ?: "shield") }
    var selectedColorHex by remember(existingMode) { mutableStateOf(existingMode?.colorHex ?: "#06B6D4") }
    var selectedDurationMinutes by remember(existingMode) { mutableIntStateOf(existingMode?.durationMinutes ?: 120) }
    var isCustomDurationDialogOpen by remember { mutableStateOf(false) }
    var selectedCallBehavior by remember(existingMode) { mutableStateOf(existingMode?.callBehavior ?: CallBehavior.ALLOW_ALL) }
    var isStrictMode by remember(existingMode) { mutableStateOf(existingMode?.isStrictMode ?: false) }
    var strictModeType by remember(existingMode) { mutableStateOf(existingMode?.strictModeType ?: StrictModeType.CONFIRMATION_DELAY_10S) }

    // App selection state
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var blockedApps by remember(existingMode) { mutableStateOf<Set<String>>(existingMode?.blockedApps ?: emptySet()) }
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedAppCategory by remember { mutableStateOf(AppCategory.ALL) }
    var isLoadingApps by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // Pre-made profile templates (e.g., 'Work', 'Deep Sleep', 'Study')
    val profileTemplates = remember {
        listOf(
            ProfileTemplate(
                name = "Work",
                iconName = "work",
                colorHex = "#6366F1",
                description = "Stay productive: silence games, video feeds, and social distractions.",
                durationMinutes = 240,
                callBehavior = CallBehavior.SILENCE_UNKNOWN,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.ENTERTAINMENT, AppCategory.GAMES)
            ),
            ProfileTemplate(
                name = "Deep Sleep",
                iconName = "bedtime",
                colorHex = "#8B5CF6",
                description = "Quiet the digital world for restorative rest. Family only.",
                durationMinutes = 480,
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.MESSAGING, AppCategory.ENTERTAINMENT, AppCategory.WORK, AppCategory.GAMES)
            ),
            ProfileTemplate(
                name = "Study",
                iconName = "school",
                colorHex = "#3B82F6",
                description = "Focus on reading, writing, and research without notifications.",
                durationMinutes = 120,
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.GAMES, AppCategory.ENTERTAINMENT)
            ),
            ProfileTemplate(
                name = "Gym & Fitness",
                iconName = "self_improvement",
                colorHex = "#10B981",
                description = "Train with total focus. Music and fitness tools remain online.",
                durationMinutes = 90,
                callBehavior = CallBehavior.ALLOW_ALL,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.WORK)
            ),
            ProfileTemplate(
                name = "Digital Detox",
                iconName = "spa",
                colorHex = "#F59E0B",
                description = "Targeted block on infinite-scroll feeds and algorithmic dopamine loops.",
                durationMinutes = 720,
                callBehavior = CallBehavior.ALLOW_ALL,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.ENTERTAINMENT, AppCategory.GAMES)
            ),
            ProfileTemplate(
                name = "Deep Focus",
                iconName = "psychology",
                colorHex = "#EC4899",
                description = "Strict distraction isolation with high friction to stop early.",
                durationMinutes = 60,
                callBehavior = CallBehavior.EMERGENCY_ONLY,
                targetCategories = setOf(AppCategory.SOCIAL, AppCategory.MESSAGING, AppCategory.ENTERTAINMENT, AppCategory.WORK, AppCategory.GAMES)
            )
        )
    }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        val apps = appDiscoveryRepository.getInstalledApps(showSystemApps = false)
        installedApps = apps

        if (!isEditing && blockedApps.isEmpty()) {
            // Default initial selection for a new mode: select distraction apps
            blockedApps = apps.filter { it.isDistractionRecommended }.map { it.packageName }.toSet()
        }
        isLoadingApps = false
    }

    val filteredApps = remember(installedApps, appSearchQuery, selectedAppCategory, blockedApps) {
        installedApps.filter { app ->
            val matchSearch = appSearchQuery.isBlank() ||
                app.appName.contains(appSearchQuery, ignoreCase = true) ||
                app.packageName.contains(appSearchQuery, ignoreCase = true)

            val isBlocked = blockedApps.contains(app.packageName)
            val matchCat = when (selectedAppCategory) {
                AppCategory.ALL -> true
                AppCategory.BLOCKED -> isBlocked
                AppCategory.ALLOWED -> !isBlocked
                else -> app.category == selectedAppCategory
            }

            matchSearch && matchCat
        }
    }

    val availableIcons = listOf(
        "shield", "work", "bedtime", "school", "beach_access", "psychology", "self_improvement", "spa", "sports_esports"
    )
    val availableColors = listOf(
        "#06B6D4", "#6366F1", "#8B5CF6", "#3B82F6", "#10B981", "#F59E0B", "#EC4899"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Focus Profile" else "Create Focus Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Templates Bar (shown for new profiles or when renaming)
            if (!isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Profile Templates:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profileTemplates) { template ->
                        val isSelected = modeName == template.name
                        Surface(
                            onClick = {
                                modeName = template.name
                                modeDescription = template.description
                                selectedIcon = template.iconName
                                selectedColorHex = template.colorHex
                                selectedDurationMinutes = template.durationMinutes
                                selectedCallBehavior = template.callBehavior

                                // Configure suggested blocked app set for this specific template
                                val matchingApps = installedApps.filter { app ->
                                    app.isDistractionRecommended || template.targetCategories.contains(app.category)
                                }.map { it.packageName }.toSet()
                                if (matchingApps.isNotEmpty()) {
                                    blockedApps = matchingApps
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModeIcon(
                                    iconName = template.iconName,
                                    tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Step 1: Name and Description
            Text(
                text = "1. Profile Name & Identity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = modeName,
                onValueChange = { modeName = it },
                label = { Text("Profile Name") },
                placeholder = { Text("e.g. Work, Deep Sleep, Study, Family Time") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = modeDescription,
                onValueChange = { modeDescription = it },
                label = { Text("Description") },
                placeholder = { Text("What boundary does this profile maintain?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Picker
            Text("Icon", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableIcons) { iconName ->
                    val isSelected = selectedIcon == iconName
                    Surface(
                        onClick = { selectedIcon = iconName },
                        shape = CircleShape,
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ModeIcon(
                                iconName = iconName,
                                tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 2: BLOCKED APP CONFIGURATION FOR THIS PROFILE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Blocked App Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                )
                OptayBadge(text = "${blockedApps.size} BLOCKED", color = CoralBlocked)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Each focus profile saves its own unique blocklist. Selected apps will disconnect whenever $modeName is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = appSearchQuery,
                onValueChange = { appSearchQuery = it },
                placeholder = { Text("Search installed applications...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (appSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { appSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Selection Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val distractionPkgs = installedApps.filter { it.isDistractionRecommended }.map { it.packageName }.toSet()
                        blockedApps = blockedApps + distractionPkgs
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralBlocked.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = CoralBlocked,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Distractions", color = CoralBlocked, fontSize = 12.sp)
                }

                Row {
                    TextButton(
                        onClick = {
                            blockedApps = installedApps.map { it.packageName }.toSet()
                        }
                    ) {
                        Text("Select All", fontSize = 12.sp, color = CyanPrimary)
                    }

                    TextButton(
                        onClick = {
                            blockedApps = emptySet()
                        }
                    ) {
                        Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Chips
            val cats = listOf(
                AppCategory.ALL,
                AppCategory.BLOCKED,
                AppCategory.ALLOWED,
                AppCategory.SOCIAL,
                AppCategory.MESSAGING,
                AppCategory.ENTERTAINMENT,
                AppCategory.WORK,
                AppCategory.GAMES
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cats) { cat ->
                    val isSel = selectedAppCategory == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedAppCategory = cat },
                        label = { Text(cat.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive list of installed apps
            if (isLoadingApps) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredApps.forEach { app ->
                        val isBlocked = blockedApps.contains(app.packageName)
                        AppSelectRowCard(
                            app = app,
                            isBlocked = isBlocked,
                            onToggle = { newBlocked ->
                                blockedApps = if (newBlocked) blockedApps + app.packageName else blockedApps - app.packageName
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 3: DEFAULT DURATION
            if (isCustomDurationDialogOpen) {
                com.example.ui.components.CustomDurationDialog(
                    initialMinutes = selectedDurationMinutes,
                    onDismiss = { isCustomDurationDialogOpen = false },
                    onConfirm = { customMinutes ->
                        selectedDurationMinutes = customMinutes
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. Default Duration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                )
                TextButton(
                    onClick = { isCustomDurationDialogOpen = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
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
            Spacer(modifier = Modifier.height(8.dp))
            val durations = listOf(
                30 to "30m",
                60 to "1h",
                120 to "2h",
                240 to "4h (Work)",
                480 to "8h (Sleep)",
                720 to "12h (Off Day)",
                0 to "Until manual stop"
            )

            val isCustomModeDuration = durations.none { it.first == selectedDurationMinutes }
            val customLabel = if (selectedDurationMinutes <= 0) "Indefinite"
            else if (selectedDurationMinutes % 60 == 0) "${selectedDurationMinutes / 60}h"
            else if (selectedDurationMinutes > 60) "${selectedDurationMinutes / 60}h ${selectedDurationMinutes % 60}m"
            else "${selectedDurationMinutes}m"

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCustomModeDuration) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { isCustomDurationDialogOpen = true },
                            label = { Text("Custom ($customLabel)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = CyanPrimary
                            )
                        )
                    }
                }

                items(durations) { (minutes, label) ->
                    val isSelected = selectedDurationMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDurationMinutes = minutes },
                        label = { Text(label) }
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = { isCustomDurationDialogOpen = true },
                        label = { Text("+ Custom...") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 4: CALL SCREENING
            Text(
                text = "4. Call Screening Rules",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CallBehavior.values().forEach { behavior ->
                    val isSelected = selectedCallBehavior == behavior
                    Surface(
                        onClick = { selectedCallBehavior = behavior },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = behavior.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = behavior.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 5: STRICT MODE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Mode Friction", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Requires mindful 10-second delay before stopping early", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isStrictMode,
                            onCheckedChange = { isStrictMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                        )
                    }

                    if (isStrictMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        StrictModeType.values().forEach { type ->
                            FilterChip(
                                selected = strictModeType == type,
                                onClick = { strictModeType = type },
                                label = { Text(type.title) },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save / Update Button
            Button(
                onClick = {
                    if (modeName.isNotBlank()) {
                        val targetId = existingMode?.id ?: ("custom_" + System.currentTimeMillis())
                        val isPredef = existingMode?.isPredefined ?: false

                        val modeToSave = FocusMode(
                            id = targetId,
                            name = modeName.trim(),
                            iconName = selectedIcon,
                            description = if (modeDescription.isNotBlank()) modeDescription.trim() else "Custom focus boundary",
                            colorHex = selectedColorHex,
                            blockedApps = blockedApps,
                            allowedApps = existingMode?.allowedApps ?: emptySet(),
                            callBehavior = selectedCallBehavior,
                            isStrictMode = isStrictMode,
                            strictModeType = strictModeType,
                            durationMinutes = selectedDurationMinutes,
                            schedule = existingMode?.schedule,
                            isPredefined = isPredef
                        )
                        scope.launch {
                            focusModeRepository.saveMode(modeToSave)
                            com.example.core.schedule.ScheduleManager.scheduleNextForMode(context, modeToSave)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_custom_mode_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = modeName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = if (isEditing) "Update Profile (${blockedApps.size} Apps Blocked)" else "Save Profile (${blockedApps.size} Apps Blocked)",
                    color = Color(0xFF042F2E),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
