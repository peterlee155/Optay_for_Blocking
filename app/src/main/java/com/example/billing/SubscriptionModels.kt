package com.example.billing

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package

enum class SubscriptionTier {
    FREE,
    PRO,
    EXPIRED,
    UNKNOWN
}

sealed class SubscriptionState {
    object Loading : SubscriptionState()
    data class Free(val customerInfo: CustomerInfo? = null) : SubscriptionState()
    data class Pro(
        val expirationDateMillis: Long? = null,
        val willRenew: Boolean = true,
        val customerInfo: CustomerInfo? = null
    ) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

enum class Feature(val displayName: String, val proOnly: Boolean) {
    UNLIMITED_MODES("Unlimited Focus Modes", true),
    RECURRING_SCHEDULES("Automated Recurring Schedules", true),
    AI_COMMANDS("AI Intent Assistant", true),
    STRICT_MODE("Strict Mode Friction Locks", true),
    CALL_SCREENING("Advanced Call Screening", true),
    ADVANCED_ANALYTICS("Deep Privacy Insights", true),
    CLOUD_SYNC("Cross-Device Sync & Backup", true),
    CUSTOM_THEMES("Custom Themes & Icons", true)
}

data class ProductPlan(
    val id: String,
    val title: String,
    val price: String,
    val billingPeriod: String,
    val badge: String? = null,
    val trialPeriod: String? = null,
    val rcPackage: Package? = null
)

val DEFAULT_PLANS = listOf(
    ProductPlan(
        id = "optay_pro_yearly",
        title = "Annual Plan",
        price = "$39.99/year",
        billingPeriod = "Billed annually ($3.33/mo)",
        badge = "SAVE 45% • BEST VALUE",
        trialPeriod = "7-day free trial"
    ),
    ProductPlan(
        id = "optay_pro_monthly",
        title = "Monthly Plan",
        price = "$5.99/month",
        billingPeriod = "Billed monthly. Cancel anytime.",
        badge = null,
        trialPeriod = "3-day free trial"
    )
)
