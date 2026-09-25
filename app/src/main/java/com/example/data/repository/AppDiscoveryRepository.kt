package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.AppCategory
import com.example.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppDiscoveryRepository(private val context: Context) {

    // Known apps metadata for category and distraction detection
    private val knownAppMeta = mapOf(
        "com.whatsapp" to AppMeta("WhatsApp", AppCategory.MESSAGING, isDistraction = true),
        "com.facebook.orca" to AppMeta("Messenger", AppCategory.MESSAGING, isDistraction = true),
        "com.facebook.katana" to AppMeta("Facebook", AppCategory.SOCIAL, isDistraction = true),
        "com.instagram.android" to AppMeta("Instagram", AppCategory.SOCIAL, isDistraction = true),
        "com.zhiliaoapp.musically" to AppMeta("TikTok", AppCategory.SOCIAL, isDistraction = true),
        "com.twitter.android" to AppMeta("X (Twitter)", AppCategory.SOCIAL, isDistraction = true),
        "org.telegram.messenger" to AppMeta("Telegram", AppCategory.MESSAGING, isDistraction = true),
        "com.discord" to AppMeta("Discord", AppCategory.MESSAGING, isDistraction = true),
        "com.google.android.youtube" to AppMeta("YouTube", AppCategory.ENTERTAINMENT, isDistraction = true),
        "com.reddit.frontpage" to AppMeta("Reddit", AppCategory.SOCIAL, isDistraction = true),
        "com.netflix.mediaclient" to AppMeta("Netflix", AppCategory.ENTERTAINMENT, isDistraction = true),
        "com.spotify.music" to AppMeta("Spotify", AppCategory.MUSIC, isDistraction = false),
        "com.google.android.apps.maps" to AppMeta("Google Maps", AppCategory.UTILITIES, isDistraction = false),
        "com.Slack" to AppMeta("Slack", AppCategory.WORK, isDistraction = true),
        "com.microsoft.teams" to AppMeta("Microsoft Teams", AppCategory.WORK, isDistraction = true),
        "com.google.android.gm" to AppMeta("Gmail", AppCategory.WORK, isDistraction = false),
        "com.supercell.clashofclans" to AppMeta("Clash of Clans", AppCategory.GAMES, isDistraction = true),
        "com.roblox.client" to AppMeta("Roblox", AppCategory.GAMES, isDistraction = true),
        "com.king.candycrushsaga" to AppMeta("Candy Crush Saga", AppCategory.GAMES, isDistraction = true)
    )

    /**
     * Strictly queries applications that are ACTUALLY INSTALLED on this device.
     * Does NOT inject any uninstalled or placeholder applications.
     */
    suspend fun getInstalledApps(showSystemApps: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedMap = mutableMapOf<String, InstalledApp>()

        // 1. Query device packages with LAUNCHER intent (user-facing applications)
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip Optay itself

            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            // Skip internal system apps if showSystemApps is false, unless it's a prominent user-facing app
            if (isSystem && !showSystemApps && !isUserFacingApp(pkg)) continue

            val label = resolveInfo.loadLabel(pm).toString()
            val category = resolveCategory(pkg, appInfo)
            val isDistraction = isDistractionApp(pkg, category)

            installedMap[pkg] = InstalledApp(
                packageName = pkg,
                appName = label,
                category = category,
                isSystemApp = isSystem,
                isDistractionRecommended = isDistraction
            )
        }

        // 2. Query all installed application packages
        try {
            val allInstalled = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in allInstalled) {
                val pkg = appInfo.packageName
                if (pkg == context.packageName || installedMap.containsKey(pkg)) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                // If non-system (user downloaded app), always include it!
                if (!isSystem || (showSystemApps || isUserFacingApp(pkg))) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val category = resolveCategory(pkg, appInfo)
                    val isDistraction = isDistractionApp(pkg, category)

                    installedMap[pkg] = InstalledApp(
                        packageName = pkg,
                        appName = label,
                        category = category,
                        isSystemApp = isSystem,
                        isDistractionRecommended = isDistraction
                    )
                }
            }
        } catch (e: Exception) {
            // Handled gracefully
        }

        installedMap.values.sortedWith(
            compareByDescending<InstalledApp> { it.isDistractionRecommended }
                .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Checks if a package is actually installed on the physical or virtual device.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Filters a collection of package names to strictly those actually installed.
     */
    fun filterInstalledPackages(packages: Set<String>): Set<String> {
        val pm = context.packageManager
        return packages.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }.toSet()
    }

    private fun isUserFacingApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("chrome") ||
            lower.contains("youtube") ||
            lower.contains("maps") ||
            lower.contains("camera") ||
            lower.contains("gallery") ||
            lower.contains("photos") ||
            lower.contains("browser") ||
            lower.contains("calculator") ||
            lower.contains("clock") ||
            lower.contains("calendar") ||
            knownAppMeta.containsKey(packageName)
    }

    private fun resolveCategory(packageName: String, appInfo: ApplicationInfo): AppCategory {
        knownAppMeta[packageName]?.let { return it.category }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.MUSIC
                ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.WORK
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.UTILITIES
            }
        }

        val lower = packageName.lowercase()
        return when {
            lower.contains("social") || lower.contains("insta") || lower.contains("twitter") || lower.contains("facebook") -> AppCategory.SOCIAL
            lower.contains("chat") || lower.contains("msg") || lower.contains("talk") || lower.contains("telegram") || lower.contains("whatsapp") -> AppCategory.MESSAGING
            lower.contains("game") -> AppCategory.GAMES
            lower.contains("music") || lower.contains("audio") || lower.contains("spotify") -> AppCategory.MUSIC
            lower.contains("work") || lower.contains("office") || lower.contains("mail") || lower.contains("docs") -> AppCategory.WORK
            lower.contains("video") || lower.contains("media") || lower.contains("tv") -> AppCategory.ENTERTAINMENT
            else -> AppCategory.OTHER
        }
    }

    private fun isDistractionApp(packageName: String, category: AppCategory): Boolean {
        knownAppMeta[packageName]?.let { return it.isDistraction }
        return category == AppCategory.SOCIAL || category == AppCategory.GAMES || category == AppCategory.ENTERTAINMENT
    }

    private data class AppMeta(val name: String, val category: AppCategory, val isDistraction: Boolean)
}
