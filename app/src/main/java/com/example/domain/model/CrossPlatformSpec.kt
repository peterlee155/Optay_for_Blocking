package com.example.domain.model

/**
 * Shared Multiplatform Architecture Specification for Optay.
 *
 * This represents the cross-platform contract across Android and iOS:
 *
 * Android Native Implementation:
 * - Network Restriction: android.net.VpnService creates a local TUN virtual interface.
 *   Only designated blocked packages are routed to the local drop interface via addAllowedApplication().
 *   All allowed and system apps completely bypass the VPN interface and maintain normal high-speed internet.
 *   No remote servers, no traffic interception, no TLS decryption.
 * - Call Filtering: android.telecom.CallScreeningService intercepts incoming calls and silences or rejects
 *   unwanted callers according to the active profile rules.
 * - Persistence: Room + DataStore (local-first).
 *
 * iOS Native Implementation:
 * - App Restrictions: FamilyControls (AuthorizationCenter) + ManagedSettings (ManagedSettingsStore.shield.applications).
 *   Shields selected applications with a custom Optay shield view via ManagedSettingsUI.
 * - Scheduling: DeviceActivity (DeviceActivitySchedule & DeviceActivityMonitorExtension) for background schedule triggers.
 * - Call Filtering: CallKit (CXCallDirectoryExtension) for identification and blocking where iOS allows.
 * - Persistence: SwiftData / UserDefaults (App Group shared with extensions).
 *
 * Shared Backend (Supabase):
 * - Tables: profiles, focus_modes, mode_apps, schedules, user_preferences, subscription_metadata.
 * - Local-First Guarantee: Optay never requires cloud access to activate, maintain, or end restrictions.
 */
object CrossPlatformSpec {
    const val PLATFORM_ANDROID = "android"
    const val PLATFORM_IOS = "ios"

    const val TABLE_PROFILES = "profiles"
    const val TABLE_FOCUS_MODES = "focus_modes"
    const val TABLE_MODE_APPS = "mode_apps"
    const val TABLE_SCHEDULES = "schedules"
    const val TABLE_DEVICES = "devices"

    const val REVENUECAT_ENTITLEMENT_PRO = "pro"
    const val PRODUCT_PRO_MONTHLY = "optay_pro_monthly"
    const val PRODUCT_PRO_YEARLY = "optay_pro_yearly"
}
