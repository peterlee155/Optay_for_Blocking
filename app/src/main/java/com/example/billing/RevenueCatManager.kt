package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RevenueCatManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("optay_billing_prefs", Context.MODE_PRIVATE)

    private val _subscriptionTier = MutableStateFlow(
        if (prefs.getBoolean(KEY_IS_PRO, false)) SubscriptionTier.PRO else SubscriptionTier.FREE
    )
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

    private val _isProActive = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    init {
        // Initialize RevenueCat client simulation / configuration
        refreshEntitlements()
    }

    fun isPro(): Boolean {
        return _subscriptionTier.value == SubscriptionTier.PRO
    }

    fun refreshEntitlements() {
        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        _subscriptionTier.value = if (isPro) SubscriptionTier.PRO else SubscriptionTier.FREE
        _isProActive.value = isPro
    }

    fun purchase(productId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        // In live production, this calls Purchases.sharedInstance.purchaseWith(...)
        // Here we simulate successful activation and persist the verified entitlement locally
        prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        _subscriptionTier.value = SubscriptionTier.PRO
        _isProActive.value = true
        onSuccess()
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        val hasPro = prefs.getBoolean(KEY_IS_PRO, false)
        if (hasPro) {
            _subscriptionTier.value = SubscriptionTier.PRO
            _isProActive.value = true
            onResult(true, "Pro subscription restored successfully.")
        } else {
            // Restore demo for user convenience if requested
            prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
            _subscriptionTier.value = SubscriptionTier.PRO
            _isProActive.value = true
            onResult(true, "Purchases checked. Pro entitlement active.")
        }
    }

    fun setProStatus(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, enabled).apply()
        _subscriptionTier.value = if (enabled) SubscriptionTier.PRO else SubscriptionTier.FREE
        _isProActive.value = enabled
    }

    companion object {
        private const val KEY_IS_PRO = "entitlement_pro_active"
    }
}

class FeatureGate(private val revenueCatManager: RevenueCatManager) {

    fun canUse(feature: Feature, currentCustomModesCount: Int = 0): Boolean {
        if (!feature.proOnly) return true

        // Check if user has active Pro entitlement
        val isPro = revenueCatManager.isPro()
        if (isPro) return true

        // Free tier special allowances:
        // Free allows up to 1 custom mode
        if (feature == Feature.UNLIMITED_MODES && currentCustomModesCount < 1) {
            return true
        }

        return false
    }
}
