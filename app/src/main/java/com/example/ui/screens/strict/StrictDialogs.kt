package com.example.ui.screens.strict

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.StrictModeType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.delay

@Composable
fun StrictModeEndDialog(
    strictModeType: StrictModeType,
    onConfirmEnd: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(10) }
    var typedPhrase by remember { mutableStateOf("") }
    val requiredPhrase = "I choose to protect my peace"
    var reflectionText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (strictModeType == StrictModeType.CONFIRMATION_DELAY_10S) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AmberWarning.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Strict Mode Active",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (strictModeType) {
                    StrictModeType.CONFIRMATION_DELAY_10S -> {
                        Text(
                            text = "Take a mindful pause. Distractions can wait. Are you certain you want to break your digital boundary?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (countdown > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$countdown",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                    )
                                    Text(
                                        text = "Breathe in... and breathe out...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "Ready. You can end now if truly needed.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    StrictModeType.PHRASE_MATCH -> {
                        Text(
                            text = "To end your intentional boundary, please type the intention phrase below:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$requiredPhrase\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = typedPhrase,
                            onValueChange = { typedPhrase = it },
                            placeholder = { Text("Type here...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    StrictModeType.REFLECTION_PROMPT -> {
                        Text(
                            text = "What urgent need requires interrupting your focus session right now?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { reflectionText = it },
                            placeholder = { Text("e.g. Urgent message from school / team") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canEnd = when (strictModeType) {
                StrictModeType.CONFIRMATION_DELAY_10S -> countdown == 0
                StrictModeType.PHRASE_MATCH -> typedPhrase.trim().equals(requiredPhrase, ignoreCase = true)
                StrictModeType.REFLECTION_PROMPT -> reflectionText.trim().length >= 4
            }

            Button(
                onClick = onConfirmEnd,
                enabled = canEnd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralBlocked,
                    disabledContainerColor = CoralBlocked.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("confirm_end_strict_button")
            ) {
                Text("End Mode Early", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Focus Active")
            }
        }
    )
}

@Composable
fun EmergencyOverrideDialog(
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency Access", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column {
                Text(
                    text = "Need quick temporary access to a restricted app? Optay will pause restrictions and automatically restore them when the timer expires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15).forEach { min ->
                        Surface(
                            onClick = { onSelectMinutes(min) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${min}m",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CyanPrimary
                                )
                                Text(
                                    text = "Access",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Emergency calling is never screened or restricted.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
