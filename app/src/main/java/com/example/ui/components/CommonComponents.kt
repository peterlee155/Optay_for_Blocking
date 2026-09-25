package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IndigoSecondary

@Composable
fun OptayBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CyanPrimary,
    backgroundColor: Color = color.copy(alpha = 0.12f)
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = backgroundColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ModeIcon(
    iconName: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val vector = when (iconName) {
        "beach_access" -> Icons.Default.BeachAccess
        "work" -> Icons.Default.Work
        "school" -> Icons.Default.School
        "bedtime" -> Icons.Default.Bedtime
        "psychology" -> Icons.Default.Psychology
        "self_improvement" -> Icons.Default.SelfImprovement
        "spa" -> Icons.Default.Spa
        "sports_esports" -> Icons.Default.SportsEsports
        else -> Icons.Default.Shield
    }
    Icon(
        imageVector = vector,
        contentDescription = iconName,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun ProTag(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(listOf(CyanPrimary, IndigoSecondary)),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("pro_badge")
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "PRO",
            tint = Color.Black,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "PRO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
