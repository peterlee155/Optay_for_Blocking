package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppCategory
import com.example.ui.theme.CoralBlocked
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary

@Composable
fun AppIconView(
    packageName: String,
    appName: String,
    category: AppCategory = AppCategory.OTHER,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        loadAppIconBitmap(context, packageName)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = appName,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.24f))
        )
    } else {
        // Fallback stylized brand badge
        val (bgBrush, textColor) = getAppBrandStyle(packageName, category)
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.24f))
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp
                ),
                color = textColor
            )
        }
    }
}

private fun loadAppIconBitmap(context: Context, packageName: String): Bitmap? {
    return try {
        val pm = context.packageManager
        val drawable: Drawable = pm.getApplicationIcon(packageName)
        drawableToBitmap(drawable)
    } catch (e: Exception) {
        null
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }

    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun getAppBrandStyle(packageName: String, category: AppCategory): Pair<Brush, Color> {
    return when (packageName) {
        "com.whatsapp" -> Brush.linearGradient(listOf(Color(0xFF25D366), Color(0xFF128C7E))) to Color.White
        "com.facebook.katana" -> Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF0D65D9))) to Color.White
        "com.facebook.orca" -> Brush.linearGradient(listOf(Color(0xFF0084FF), Color(0xFFA033FF))) to Color.White
        "com.instagram.android" -> Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))) to Color.White
        "com.zhiliaoapp.musically" -> Brush.linearGradient(listOf(Color(0xFF010101), Color(0xFF25F4EE), Color(0xFFFE2C55))) to Color.White
        "com.twitter.android" -> Brush.linearGradient(listOf(Color(0xFF14171A), Color(0xFF1DA1F2))) to Color.White
        "org.telegram.messenger" -> Brush.linearGradient(listOf(Color(0xFF2AABEE), Color(0xFF229ED9))) to Color.White
        "com.discord" -> Brush.linearGradient(listOf(Color(0xFF5865F2), Color(0xFF4752C4))) to Color.White
        "com.google.android.youtube" -> Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFFCC0000))) to Color.White
        "com.spotify.music" -> Brush.linearGradient(listOf(Color(0xFF1DB954), Color(0xFF191414))) to Color.White
        "com.google.android.apps.maps" -> Brush.linearGradient(listOf(Color(0xFF34A853), Color(0xFF4285F4))) to Color.White
        "com.reddit.frontpage" -> Brush.linearGradient(listOf(Color(0xFFFF4500), Color(0xFFCC3700))) to Color.White
        "com.Slack" -> Brush.linearGradient(listOf(Color(0xFF4A154B), Color(0xFF611f69))) to Color.White
        "com.microsoft.teams" -> Brush.linearGradient(listOf(Color(0xFF464EB8), Color(0xFF5B64DF))) to Color.White
        else -> when (category) {
            AppCategory.SOCIAL -> Brush.linearGradient(listOf(IndigoSecondary, Color(0xFF8B5CF6))) to Color.White
            AppCategory.MESSAGING -> Brush.linearGradient(listOf(CyanPrimary, Color(0xFF0284C7))) to Color.White
            AppCategory.ENTERTAINMENT -> Brush.linearGradient(listOf(CoralBlocked, Color(0xFFBE123C))) to Color.White
            AppCategory.GAMES -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))) to Color.White
            AppCategory.WORK -> Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))) to Color.White
            AppCategory.MUSIC -> Brush.linearGradient(listOf(EmeraldSuccess, Color(0xFF047857))) to Color.White
            else -> Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))) to Color.White
        }
    }
}
