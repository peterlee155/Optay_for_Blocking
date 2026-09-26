package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RevenueCatManager(context: Context) {

    companion object {
        private const val TAG = "OptayRevenueCat"
        const val ENTITLEMENT_PRO = "pro"
        const val PACKAGE_MONTHLY = "optay_pro_monthly"
        const val PACKAGE_ANNUAL = "optay_pro_yearly"

        private var isConfigured = false

        fun initialize(context: Context) {
            if (isConfigured) return
            try {
                // Public SDK key - safe to use in client app. Never contains secret keys.
                val apiKey = try {
                    BuildConfig.REVENUECAT_PUBLIC_KEY
                } catch (e: Exception) {
                    ""
                }.ifBlank { "goog_placeholder_public_sdk_key" }

                if (apiKey.isNotBlank() && apiKey != "goog_placeholder_public_sdk_key" && apiKey.startsWith("goog_")) {
                    Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
                    Purchases.configure(
                        PurchasesConfiguration.Builder(context, apiKey).build()
                    )
                    isConfigured = true
                    Log.i(TAG, "RevenueCat Purchases SDK initialized successfully.")
                } else {
                    Log.i(TAG, "RevenueCat: Using local preview mode until production key is supplied.")
                    isConfigured = false
                }
            } catch (t: Throwable) {
                Log.w(TAG, "RevenueCat initialization note: SDK configuration deferred.", t)
                isConfigured = false
            }
        }
    }

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _isProActive = MutableStateFlow(false)
    val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    private val _availablePlans = MutableStateFlow<List<ProductPlan>>(DEFAULT_PLANS)
    val availablePlans: StateFlow<List<ProductPlan>> = _availablePlans.asStateFlow()

    init {
        initialize(context)

        if (isConfigured) {
            try {
                Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                    processCustomerInfo(customerInfo)
                }
                refreshCustomerInfo()
                fetchOfferings()
            } catch (e: Exception) {
                Log.w(TAG, "Failed attaching customer info listener: ${e.message}")
                _subscriptionState.value = SubscriptionState.Free()
            }
        } else {
            _subscriptionState.value = SubscriptionState.Free()
        }
    }

    fun isPro(): Boolean {
        return _subscriptionState.value is SubscriptionState.Pro
    }

    fun refreshCustomerInfo() {
        if (!isConfigured) {
            _subscriptionState.value = SubscriptionState.Free()
            return
        }

        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    processCustomerInfo(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Error fetching RevenueCat customer info: ${error.message}")
                    // On offline network error, keep current state or fallback to Free
                    if (_subscriptionState.value is SubscriptionState.Loading) {
                        _subscriptionState.value = SubscriptionState.Free()
                    }
                }
            })
        } catch (e: Exception) {
            _subscriptionState.value = SubscriptionState.Free()
        }
    }

    private fun processCustomerInfo(customerInfo: CustomerInfo) {
        val proEntitlement = customerInfo.entitlements[ENTITLEMENT_PRO]
        if (proEntitlement != null && proEntitlement.isActive) {
            val expMillis = proEntitlement.expirationDate?.time
            val willRenew = proEntitlement.willRenew
            _subscriptionState.value = SubscriptionState.Pro(
                expirationDateMillis = expMillis,
                willRenew = willRenew,
                customerInfo = customerInfo
            )
            _isProActive.value = true
            Log.i(TAG, "Verified active Pro entitlement.")
        } else {
            _subscriptionState.value = SubscriptionState.Free(customerInfo)
            _isProActive.value = false
            Log.i(TAG, "Pro entitlement inactive or expired.")
        }
    }

    fun fetchOfferings() {
        if (!isConfigured) return

        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    _offerings.value = offerings
                    val currentOffering = offerings.current
                    if (currentOffering != null && currentOffering.availablePackages.isNotEmpty()) {
                        val plans = currentOffering.availablePackages.map { pkg ->
                            ProductPlan(
                                id = pkg.identifier,
                                title = pkg.product.title.substringBefore("(").trim(),
                                price = pkg.product.price.formatted,
                                billingPeriod = pkg.product.description,
                                badge = if (pkg.identifier.contains("yearly") || pkg.identifier.contains("annual")) {
                                    "BEST VALUE • 7-DAY TRIAL"
                                } else null,
                                trialPeriod = if (pkg.identifier.contains("yearly")) "7-day free trial" else "3-day free trial",
                                rcPackage = pkg
                            )
                        }
                        _availablePlans.value = plans
                    }
                }

                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Unable to load live RevenueCat offerings: ${error.message}")
                    // Retain default plans display for UI stability
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Exception loading offerings: ${e.message}")
        }
    }

    fun purchase(
        activity: Activity,
        targetPackageId: String,
        onSuccess: () -> Unit,
        onError: (String, Boolean) -> Unit
    ) {
        if (!isConfigured) {
            onError("Billing system not configured yet with valid Google Play credentials.", false)
            return
        }

        val targetPackage = _offerings.value?.current?.availablePackages?.find {
            it.identifier == targetPackageId || it.product.id == targetPackageId
        }

        if (targetPackage == null) {
            onError("Selected plan is currently unavailable in Google Play. Please try again shortly.", false)
            return
        }

        try {
            val params = PurchaseParams.Builder(activity, targetPackage).build()
            Purchases.sharedInstance.purchase(
                params,
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        processCustomerInfo(customerInfo)
                        if (isPro()) {
                            onSuccess()
                        } else {
                            onError("Purchase completed but Pro entitlement is still syncing.", false)
                        }
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        Log.w(TAG, "Purchase failed: ${error.message}, userCancelled: $userCancelled")
                        onError(error.message, userCancelled)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Purchase initiation error", e)
            onError(e.message ?: "Failed to initiate purchase", false)
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        if (!isConfigured) {
            onResult(false, "Billing configuration is unavailable.")
            return
        }

        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val proEntitlement = customerInfo.entitlements[ENTITLEMENT_PRO]
                    if (proEntitlement != null && proEntitlement.isActive) {
                        processCustomerInfo(customerInfo)
                        onResult(true, "Pro subscription restored successfully.")
                    } else {
                        _subscriptionState.value = SubscriptionState.Free(customerInfo)
                        onResult(false, "No active Pro subscription found for this Google Play account.")
                    }
                }

                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Restore failed: ${error.message}")
                    onResult(false, "Unable to restore purchases: ${error.message}")
                }
            })
        } catch (e: Exception) {
            onResult(false, "Restore error: ${e.message}")
        }
    }
}

class FeatureGate(private val revenueCatManager: RevenueCatManager) {

    fun canUse(feature: Feature, currentCustomModesCount: Int = 0): Boolean {
        if (!feature.proOnly) return true

        // Verified active Pro entitlement from RevenueCat
        val isPro = revenueCatManager.isPro()
        if (isPro) return true

        // Free tier allowances:
        // Free allows up to 1 custom mode
        if (feature == Feature.UNLIMITED_MODES && currentCustomModesCount < 1) {
            return true
        }

        return false
    }
}
