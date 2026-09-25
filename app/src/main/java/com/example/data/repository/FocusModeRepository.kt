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
                val initialModes = DEFAULT_MODES.map { mode ->
                    if (appDiscoveryRepository != null) {
                        val installed = mode.blockedApps.filter { appDiscoveryRepository.isPackageInstalled(it) }.toSet()
                        mode.copy(blockedApps = installed)
                    } else {
                        mode
                    }
                }
                dao.insertAll(initialModes.map { it.toEntity() })
            }
        }
    }

    val focusModes: Flow<List<FocusMode>> = dao.getAllFocusModes().map { entities ->
        entities.map { entity ->
            val mode = entity.toDomain()
            if (appDiscoveryRepository != null) {
                mode.copy(blockedApps = mode.blockedApps.filter { appDiscoveryRepository.isPackageInstalled(it) }.toSet())
            } else {
                mode
            }
        }
    }

    suspend fun getModeById(id: String): FocusMode? {
        val mode = dao.getFocusModeById(id)?.toDomain() ?: return null
        return if (appDiscoveryRepository != null) {
            mode.copy(blockedApps = mode.blockedApps.filter { appDiscoveryRepository.isPackageInstalled(it) }.toSet())
        } else {
            mode
        }
    }

    suspend fun saveMode(mode: FocusMode) {
        val finalMode = if (appDiscoveryRepository != null) {
            mode.copy(blockedApps = mode.blockedApps.filter { appDiscoveryRepository.isPackageInstalled(it) }.toSet())
        } else {
            mode
        }
        dao.insertOrUpdate(finalMode.toEntity())
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
                    "com.android.camera"
                ),
                callBehavior = CallBehavior.SILENCE_WORK_NUMBERS,
                isStrictMode = false,
                durationMinutes = 720, // 12 hours / until morning
                schedule = ScheduleRule(
                    daysOfWeek = setOf(1, 7), // Sun & Sat
                    startHour = 22,
                    startMinute = 0,
                    endHour = 10,
                    endMinute = 30
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_work",
                name = "Work",
                iconName = "work",
                description = "Stay productive: silence games, video feeds, and leisure chats.",
                colorHex = "#6366F1", // Indigo
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.facebook.katana",
                    "com.google.android.youtube",
                    "com.reddit.frontpage"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.SILENCE_UNKNOWN,
                isStrictMode = false,
                durationMinutes = 240, // 4 hours
                schedule = ScheduleRule(
                    daysOfWeek = setOf(2, 3, 4, 5, 6), // Mon-Fri
                    startHour = 9,
                    startMinute = 0,
                    endHour = 17,
                    endMinute = 0
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_study",
                name = "Study",
                iconName = "school",
                description = "Focus on reading, writing, and research without interruptions.",
                colorHex = "#3B82F6",
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.facebook.katana",
                    "com.twitter.android",
                    "com.discord",
                    "com.google.android.youtube"
                ),
                allowedApps = setOf("com.spotify.music"),
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                isStrictMode = true,
                strictModeType = StrictModeType.CONFIRMATION_DELAY_10S,
                durationMinutes = 120, // 2 hours
                isPredefined = true
            ),
            FocusMode(
                id = "mode_sleep",
                name = "Sleep",
                iconName = "bedtime",
                description = "Quiet the digital world for restorative rest. Family only.",
                colorHex = "#8B5CF6", // Violet
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.facebook.katana",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "org.telegram.messenger",
                    "com.discord"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.ALLOW_FAVORITES_ONLY,
                isStrictMode = false,
                durationMinutes = 480, // 8 hours
                schedule = ScheduleRule(
                    daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                    startHour = 23,
                    startMinute = 0,
                    endHour = 7,
                    endMinute = 0
                ),
                isPredefined = true
            ),
            FocusMode(
                id = "mode_deep_focus",
                name = "Deep Focus",
                iconName = "psychology",
                description = "Strict isolation. High friction to stop. Silence everything non-vital.",
                colorHex = "#EC4899", // Pink
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.facebook.katana",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.twitter.android",
                    "org.telegram.messenger",
                    "com.discord",
                    "com.reddit.frontpage",
                    "com.google.android.youtube"
                ),
                allowedApps = setOf("com.google.android.apps.maps", "com.spotify.music"),
                callBehavior = CallBehavior.EMERGENCY_ONLY,
                isStrictMode = true,
                strictModeType = StrictModeType.PHRASE_MATCH,
                durationMinutes = 60,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_personal_time",
                name = "Personal Time",
                iconName = "self_improvement",
                description = "Disconnect from work tools (Slack, Teams, Email) and enjoy personal peace.",
                colorHex = "#10B981", // Emerald
                blockedApps = setOf(
                    "com.Slack",
                    "com.microsoft.teams",
                    "com.google.android.gm"
                ),
                allowedApps = setOf("com.spotify.music", "com.google.android.apps.maps"),
                callBehavior = CallBehavior.SILENCE_WORK_NUMBERS,
                isStrictMode = false,
                durationMinutes = 180,
                isPredefined = true
            ),
            FocusMode(
                id = "mode_social_detox",
                name = "Social Detox",
                iconName = "spa",
                description = "Targeted block on infinite-scroll social feeds and short videos.",
                colorHex = "#F59E0B", // Amber
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.facebook.katana",
                    "com.twitter.android",
                    "com.reddit.frontpage",
                    "com.google.android.youtube"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.ALLOW_ALL,
                isStrictMode = false,
                durationMinutes = 1440, // 24 hours
                isPredefined = true
            ),
            FocusMode(
                id = "mode_gaming",
                name = "Gaming",
                iconName = "sports_esports",
                description = "Mute messaging alerts and work calls while gaming.",
                colorHex = "#06B6D4",
                blockedApps = setOf(
                    "com.whatsapp",
                    "com.facebook.orca",
                    "com.Slack"
                ),
                allowedApps = emptySet(),
                callBehavior = CallBehavior.SILENCE_UNKNOWN,
                isStrictMode = false,
                durationMinutes = 120,
                isPredefined = true
            )
        )
    }
}

// Entity <-> Domain Mappers
fun FocusMode.toEntity(): FocusModeEntity {
    return FocusModeEntity(
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
        scheduleStartHour = schedule?.startHour ?: 0,
        scheduleStartMinute = schedule?.startMinute ?: 0,
        scheduleEndHour = schedule?.endHour ?: 0,
        scheduleEndMinute = schedule?.endMinute ?: 0,
        scheduleEnabled = schedule?.isEnabled ?: false,
        isPredefined = isPredefined
    )
}

fun FocusModeEntity.toDomain(): FocusMode {
    val blocked = if (blockedAppsJson.isBlank()) emptySet() else blockedAppsJson.split(",").map { it.trim() }.toSet()
    val allowed = if (allowedAppsJson.isBlank()) emptySet() else allowedAppsJson.split(",").map { it.trim() }.toSet()

    val sched = if (scheduleDays.isNotBlank()) {
        val days = scheduleDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        ScheduleRule(
            daysOfWeek = days,
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
        blockedApps = blocked,
        allowedApps = allowed,
        callBehavior = try { CallBehavior.valueOf(callBehavior) } catch (e: Exception) { CallBehavior.ALLOW_ALL },
        isStrictMode = isStrictMode,
        strictModeType = try { StrictModeType.valueOf(strictModeType) } catch (e: Exception) { StrictModeType.CONFIRMATION_DELAY_10S },
        durationMinutes = durationMinutes,
        schedule = sched,
        isPredefined = isPredefined
    )
}
