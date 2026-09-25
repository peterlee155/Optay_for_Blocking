package com.example.ui.screens.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.FocusModeRepository
import com.example.domain.model.AppCategory
import com.example.domain.model.FocusMode
import com.example.domain.model.InstalledApp
import com.example.ui.components.OptayBadge
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionSheet(
    targetMode: FocusMode,
    appDiscoveryRepository: AppDiscoveryRepository,
    focusModeRepository: FocusModeRepository,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var blockedSet by remember { mutableStateOf(targetMode.blockedApps) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        installedApps = appDiscoveryRepository.getInstalledApps(showSystemApps = false)
        isLoading = false
    }

    val filtered = remember(installedApps, searchQuery, selectedCategory, blockedSet) {
        installedApps.filter { app ->
            val matchSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val isBlocked = blockedSet.contains(app.packageName)
            val matchCat = when (selectedCategory) {
                AppCategory.ALL -> true
                AppCategory.BLOCKED -> isBlocked
                AppCategory.ALLOWED -> !isBlocked
                else -> app.category == selectedCategory
            }

            matchSearch && matchCat
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("app_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Choose Apps to Restrict",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "For mode: ${targetMode.name} • ${blockedSet.size} apps offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralBlocked
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed applications...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bulk actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val distractionPkgs = installedApps.filter { it.isDistractionRecommended }.map { it.packageName }.toSet()
                        blockedSet = blockedSet + distractionPkgs
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralBlocked.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = CoralBlocked, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Distractions", color = CoralBlocked, fontSize = 12.sp)
                }

                Row {
                    TextButton(
                        onClick = {
                            blockedSet = installedApps.map { it.packageName }.toSet()
                        }
                    ) {
                        Text("Select All", fontSize = 12.sp, color = CyanPrimary)
                    }

                    TextButton(
                        onClick = {
                            blockedSet = emptySet()
                        }
                    ) {
                        Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Categories
            val cats = listOf(AppCategory.ALL, AppCategory.BLOCKED, AppCategory.ALLOWED, AppCategory.SOCIAL, AppCategory.MESSAGING, AppCategory.ENTERTAINMENT, AppCategory.WORK, AppCategory.GAMES)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cats) { cat ->
                    val isSel = selectedCategory == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // List of Installed Apps
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isBlocked = blockedSet.contains(app.packageName)
                        AppSelectRowCard(
                            app = app,
                            isBlocked = isBlocked,
                            onToggle = { newBlocked ->
                                blockedSet = if (newBlocked) blockedSet + app.packageName else blockedSet - app.packageName
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save and Apply Button
            Button(
                onClick = {
                    scope.launch {
                        focusModeRepository.saveMode(targetMode.copy(blockedApps = blockedSet))
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_selected_apps_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = "Save App Boundaries (${blockedSet.size} Blocked)",
                    color = Color(0xFF042F2E),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
