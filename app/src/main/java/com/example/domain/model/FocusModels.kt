package com.example.domain.model

enum class NetworkAccess {
    ALLOW,
    BLOCK,
    WIFI_ONLY,
    MOBILE_ONLY
}

data class FirewallRule(
    val packageName: String,
    val access: NetworkAccess = NetworkAccess.BLOCK
)

enum class CallBehavior(val displayName: String, val description: String) {
    ALLOW_ALL("Allow All Calls", "Standard call handling with no screening"),
    SILENCE_UNKNOWN("Silence Unknown Callers", "Silence numbers not in your contacts"),
    ALLOW_FAVORITES_ONLY("Favorites & Family Only", "Allow starred family and emergency contacts"),
    SILENCE_WORK_NUMBERS("Silence Work Numbers", "Filter contacts designated as work"),
    EMERGENCY_ONLY("Emergency Contacts Only", "Silence all except designated emergency contacts")
}

enum class StrictModeType(val title: String, val description: String) {
    CONFIRMATION_DELAY_10S("Mindful 10s Countdown", "Wait 10 seconds with a calming breathing prompt"),
    PHRASE_MATCH("Type Intention Phrase", "Type 'I choose to protect my peace' before ending"),
    REFLECTION_PROMPT("Reflection Prompt", "Write down why you need to interrupt your boundary")
}

enum class AppCategory(val title: String) {
    ALL("All"),
    BLOCKED("Blocked"),
    ALLOWED("Allowed"),
    SOCIAL("Social"),
    MESSAGING("Messaging"),
    ENTERTAINMENT("Entertainment"),
    WORK("Work"),
    GAMES("Games"),
    UTILITIES("Utilities"),
    MUSIC("Music"),
    OTHER("Other")
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory = AppCategory.OTHER,
    val isSystemApp: Boolean = false,
    val isDistractionRecommended: Boolean = false,
    val isBlocked: Boolean = false
)

data class ScheduleRule(
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5), // Calendar.MONDAY to FRIDAY
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val isEnabled: Boolean = true
) {
    fun formatSchedule(): String {
        val days = if (daysOfWeek.size == 7) "Every day"
        else if (daysOfWeek == setOf(1, 7)) "Weekends"
        else if (daysOfWeek == setOf(2, 3, 4, 5, 6)) "Mon–Fri"
        else "${daysOfWeek.size} days/week"

        val startTime = String.format("%02d:%02d", startHour, startMinute)
        val endTime = String.format("%02d:%02d", endHour, endMinute)
        return "$days, $startTime – $endTime"
    }
}

data class FocusMode(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String,
    val colorHex: String,
    val blockedApps: Set<String> = emptySet(),
    val allowedApps: Set<String> = emptySet(),
    val callBehavior: CallBehavior = CallBehavior.ALLOW_ALL,
    val isStrictMode: Boolean = false,
    val strictModeType: StrictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
    val durationMinutes: Int = 120, // 0 means indefinitely until manual stop
    val schedule: ScheduleRule? = null,
    val isPredefined: Boolean = false
)
