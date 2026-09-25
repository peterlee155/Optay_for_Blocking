package com.example.ai

import com.example.domain.model.CallBehavior
import com.example.domain.model.FocusMode
import java.util.Calendar

data class AICommandResult(
    val suggestedName: String,
    val description: String,
    val durationMinutes: Int,
    val blockedAppKeywords: List<String>,
    val allowedAppKeywords: List<String>,
    val callBehavior: CallBehavior,
    val confidence: Float,
    val explanation: String
)

interface AIProvider {
    suspend fun parseCommand(input: String): AICommandResult
    suspend fun recommendMode(timeContext: String, userHabits: String): FocusModeRecommendation
    suspend fun generateReflection(reason: String): String
}

data class FocusModeRecommendation(
    val recommendedModeId: String,
    val modeName: String,
    val reason: String
)

class OptayAIProvider : AIProvider {

    override suspend fun parseCommand(input: String): AICommandResult {
        val lower = input.lowercase()

        // 1. Detect target apps to block
        val blocked = mutableListOf<String>()
        val allowed = mutableListOf<String>()

        val candidateApps = mapOf(
            "whatsapp" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "messenger" to "com.facebook.orca",
            "instagram" to "com.instagram.android",
            "insta" to "com.instagram.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "telegram" to "org.telegram.messenger",
            "discord" to "com.discord",
            "youtube" to "com.google.android.youtube",
            "reddit" to "com.reddit.frontpage",
            "slack" to "com.Slack",
            "teams" to "com.microsoft.teams",
            "maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "games" to "games",
            "social" to "social"
        )

        // Identify allowed phrases e.g. "keep maps", "allow spotify", "except spotify"
        for ((name, pkg) in candidateApps) {
            val isAllowed = lower.contains("keep $name") ||
                lower.contains("allow $name") ||
                lower.contains("except $name") ||
                lower.contains("leave $name")
            val isBlocked = lower.contains("block $name") ||
                lower.contains("disconnect $name") ||
                lower.contains("silence $name") ||
                lower.contains("restrict $name") ||
                (!isAllowed && lower.contains(name) && !lower.contains("keep"))

            if (isAllowed) {
                allowed.add(pkg)
            } else if (isBlocked) {
                blocked.add(pkg)
            }
        }

        // If generic terms used
        if (lower.contains("social") && blocked.isEmpty()) {
            blocked.addAll(listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.facebook.katana", "com.twitter.android"))
        }

        // 2. Detect duration
        var duration = 120 // Default 2h
        when {
            lower.contains("15 min") -> duration = 15
            lower.contains("30 min") -> duration = 30
            lower.contains("1 hour") || lower.contains("one hour") || lower.contains("1h") -> duration = 60
            lower.contains("2 hour") || lower.contains("two hour") || lower.contains("2h") -> duration = 120
            lower.contains("3 hour") || lower.contains("3h") -> duration = 180
            lower.contains("4 hour") || lower.contains("4h") -> duration = 240
            lower.contains("8 hour") || lower.contains("8h") -> duration = 480
            lower.contains("tomorrow") -> duration = 720
            lower.contains("off day") || lower.contains("off today") || lower.contains("day off") -> duration = 720
            lower.contains("tonight") -> duration = 360
        }

        // 3. Detect call behavior
        val callBehavior = when {
            lower.contains("work") && (lower.contains("silence") || lower.contains("block")) -> CallBehavior.SILENCE_WORK_NUMBERS
            lower.contains("favorite") || lower.contains("family only") -> CallBehavior.ALLOW_FAVORITES_ONLY
            lower.contains("emergency only") -> CallBehavior.EMERGENCY_ONLY
            lower.contains("unknown") -> CallBehavior.SILENCE_UNKNOWN
            else -> CallBehavior.ALLOW_ALL
        }

        // 4. Suggest mode name
        val modeName = when {
            lower.contains("off day") || lower.contains("day off") || lower.contains("vacation") -> "Off Day"
            lower.contains("study") || lower.contains("read") || lower.contains("exam") -> "Study"
            lower.contains("sleep") || lower.contains("night") -> "Sleep"
            lower.contains("work") || lower.contains("focus") -> "Work"
            lower.contains("detox") -> "Social Detox"
            else -> "Custom Focus"
        }

        return AICommandResult(
            suggestedName = modeName,
            description = "AI boundary intent: disconnect $blocked while allowing $allowed",
            durationMinutes = duration,
            blockedAppKeywords = blocked,
            allowedAppKeywords = allowed,
            callBehavior = callBehavior,
            confidence = 0.95f,
            explanation = "Extracted rule configuration safely without external packet inspection or arbitrary script execution."
        )
    }

    override suspend fun recommendMode(timeContext: String, userHabits: String): FocusModeRecommendation {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when {
            hour in 22..23 || hour in 0..6 -> FocusModeRecommendation(
                "mode_sleep",
                "Sleep Mode",
                "It's late. Protect your sleep boundary by muting social chatter and only allowing family."
            )
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY -> FocusModeRecommendation(
                "mode_off_day",
                "Off Day Mode",
                "It's the weekend! Silence work-related apps and messaging while keeping music and navigation ready."
            )
            hour in 9..17 -> FocusModeRecommendation(
                "mode_work",
                "Work Mode",
                "Deep focus hours. Keep your concentration sharp by disconnecting entertainment feeds."
            )
            else -> FocusModeRecommendation(
                "mode_personal_time",
                "Personal Time",
                "Unwind without endless work notifications."
            )
        }
    }

    override suspend fun generateReflection(reason: String): String {
        return "Taking back your attention requires intentional choices. You've protected your peace and reclaimed valuable moments for what truly matters."
    }
}
