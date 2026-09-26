package com.example.data.repository

import com.example.data.db.FocusModeDao
import com.example.data.db.FocusModeEntity
import com.example.domain.model.CallBehavior
import com.example.domain.model.FocusMode
import com.example.domain.model.ScheduleRule
import com.example.domain.model.StrictModeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FocusModeRepository(
    private val dao: FocusModeDao,
    private val appDiscoveryRepository: AppDiscoveryRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    init {
        scope.launch {
            val existing = dao.getAllFocusModes().first()
            if (existing.isEmpty()) {
                dao.insertAll(DEFAULT_MODES.map { it.toEntity() })
            }
        }
    }

    val focusModes: Flow<List<FocusMode>> = dao.getAllFocusModes().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getModeById(id: String): FocusMode? {
        return dao.getFocusModeById(id)?.toDomain()
    }

    suspend fun saveMode(mode: FocusMode) {
        dao.insertOrUpdate(mode.toEntity())
    }

    suspend fun deleteMode(id: String) {
        dao.deleteById(id)
    }

    suspend fun duplicateMode(originalId: String, newName: String): FocusMode? {
        val original = getModeById(originalId) ?: return null
        val newMode = original.copy(
            id = "custom_" + System.currentTimeMillis(),
            name = newName,
            isPredefined = false
        )
        saveMode(newMode)
        return newMode
    }

    companion object {
        // Pre-configured default modes specified in the prompt
        val DEFAULT_MODES = listOf(
            FocusMode(
                id = "mode_off_day",
                name = "Off Day",
                iconName = "beach_access",
                description = "Keep phone online while disconnecting social, work and messaging.",
                colorHex = "#06B6D4", // Cyan
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca", // Messenger
                    "com.facebook.katana",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically", // TikTok
                    "com.twitter.android"
                ),
                allowedApps = setOf(
                    "com.google.android.apps.maps",
                    "com.spotify.music",
                    "com.google.android.apps.photos"
                ),
                callBehavior = CallBehavior.SILENCE_WORK_NUMBERS,
                isStrictMode = false,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 180,
                schedule = ScheduleRule(
                    daysOfWeek = setOf(1, 7), // Sun & Sat
                    startHour = 9,
                    startMinute = 0,
                    endHour = 21,
                    endMinute = 0,
                    isEnabled = false
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_work",
                name = "Work",
                iconName = "work",
                description = "Stay focused during the workday. Silence non-essential notifications.",
                colorHex = "#3B82F6", // Blue
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "com.reddit.frontpage",
                    "com.facebook.katana"
                ),
                allowedApps = setOf(
                    "com.Slack",
                    "com.microsoft.teams",
                    "com.google.android.gm"
                ),
                callBehavior = CallBehavior.SILENCE_UNKNOWN,
                isStrictMode = true,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 120,
                schedule = ScheduleRule(
                    daysOfWeek = setOf(2, 3, 4, 5, 6), // Mon-Fri
                    startHour = 9,
                    startMinute = 0,
                    endHour = 17,
                    endMinute = 0,
                    isEnabled = false
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_study",
                name = "Study",
                iconName = "menu_book",
                description = "Eliminate all digital chatter for deep academic concentration.",
                colorHex = "#8B5CF6", // Purple
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "com.discord",
                    "com.google.android.youtube",
                    "com.netflix.mediaclient"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                isStrictMode = true,
                strictModeType = StrictModeType.PHRASE_MATCH,
                durationMinutes = 60,
                schedule = null,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_sleep",
                name = "Sleep",
                iconName = "bedtime",
                description = "Restful sleep boundary. Only emergency contacts can reach you.",
                colorHex = "#6366F1", // Indigo
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.facebook.katana",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "com.google.android.youtube",
                    "com.netflix.mediaclient",
                    "com.reddit.frontpage"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.EMERGENCY_ONLY,
                isStrictMode = true,
                strictModeType = StrictModeType.REFLECTION_PROMPT,
                durationMinutes = 480, // 8 hours
                schedule = ScheduleRule(
                    daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7), // Everyday
                    startHour = 22,
                    startMinute = 30,
                    endHour = 7,
                    endMinute = 0,
                    isEnabled = false
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_deep_focus",
                name = "Deep Focus",
                iconName = "psychology",
                description = "Maximum digital isolation for flow state and creative output.",
                colorHex = "#EC4899", // Pink
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "com.google.android.youtube",
                    "com.reddit.frontpage",
                    "com.Slack",
                    "com.microsoft.teams"
                ),
                allowedApps = setOf("com.spotify.music"),
                callBehavior = CallBehavior.EMERGENCY_ONLY,
                isStrictMode = true,
                strictModeType = StrictModeType.PHRASE_MATCH,
                durationMinutes = 90,
                schedule = null,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_personal_time",
                name = "Personal Time",
                iconName = "favorite",
                description = "Disconnect from work and news. Be present with family and friends.",
                colorHex = "#10B981", // Emerald
                blockedApps = setOf(
                    "com.Slack",
                    "com.microsoft.teams",
                    "com.google.android.gm",
                    "com.twitter.android",
                    "com.reddit.frontpage"
                ),
                allowedApps = setOf(
                    "com.whatsapp",
                    "com.google.android.apps.photos"
                ),
                callBehavior = CallBehavior.SILENCE_WORK_NUMBERS,
                isStrictMode = false,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 120,
                schedule = null,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_social_detox",
                name = "Social Detox",
                iconName = "spa",
                description = "Take a healthy break from endless social media scrolling.",
                colorHex = "#F59E0B", // Amber
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "com.facebook.katana",
                    "com.reddit.frontpage",
                    "com.snapchat.android"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.ALLOW_ALL,
                isStrictMode = true,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 1440, // 24 hours
                schedule = null,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_gaming",
                name = "Gaming",
                iconName = "sports_esports",
                description = "Uninterrupted gameplay without work pings or social distractions.",
                colorHex = "#EF4444", // Red
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.Slack",
                    "com.microsoft.teams",
                    "com.google.android.gm"
                ),
                allowedApps = setOf("com.discord"),
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                isStrictMode = false,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 90,
                schedule = null,
                isPredefined = true
            )
        )
    }
}

private fun FocusMode.toEntity(): FocusModeEntity = FocusModeEntity(
    id = id,
    name = name,
    iconName = iconName,
    description = description,
    colorHex = colorHex,
    blockedAppsJson = blockedApps.joinToString(","),
    allowedAppsJson = allowedApps.joinToString(","),
    callBehavior = callBehavior.name,
    isStrictMode = isStrictMode,
    strictModeType = strictModeType.name,
    durationMinutes = durationMinutes,
    scheduleDays = schedule?.daysOfWeek?.joinToString(",") ?: "",
    scheduleStartHour = schedule?.startHour ?: 9,
    scheduleStartMinute = schedule?.startMinute ?: 0,
    scheduleEndHour = schedule?.endHour ?: 17,
    scheduleEndMinute = schedule?.endMinute ?: 0,
    scheduleEnabled = schedule?.isEnabled ?: false,
    isPredefined = isPredefined
)

private fun FocusModeEntity.toDomain(): FocusMode {
    val schedule = if (scheduleDays.isNotBlank()) {
        ScheduleRule(
            daysOfWeek = scheduleDays.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toSet(),
            startHour = scheduleStartHour,
            startMinute = scheduleStartMinute,
            endHour = scheduleEndHour,
            endMinute = scheduleEndMinute,
            isEnabled = scheduleEnabled
        )
    } else null

    return FocusMode(
        id = id,
        name = name,
        iconName = iconName,
        description = description,
        colorHex = colorHex,
        blockedApps = blockedAppsJson.split(",").filter { it.isNotBlank() }.toSet(),
        allowedApps = allowedAppsJson.split(",").filter { it.isNotBlank() }.toSet(),
        callBehavior = try { CallBehavior.valueOf(callBehavior) } catch (e: Exception) { CallBehavior.ALLOW_ALL },
        isStrictMode = isStrictMode,
        strictModeType = try { StrictModeType.valueOf(strictModeType) } catch (e: Exception) { StrictModeType.CONFIRMATION_DELAY_10S },
        durationMinutes = durationMinutes,
        schedule = schedule,
        isPredefined = isPredefined
    )
}
