package com.example

import android.app.Application
import androidx.room.Room
import com.example.ai.OptayAIProvider
import com.example.analytics.AnalyticsTracker
import com.example.billing.FeatureGate
import com.example.billing.RevenueCatManager
import com.example.core.firewall.FirewallManager
import com.example.data.db.OptayDatabase
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.FocusModeRepository
import com.example.data.repository.PreferencesRepository

class OptayApplication : Application() {

    lateinit var database: OptayDatabase
        private set

    lateinit var focusModeRepository: FocusModeRepository
        private set

    lateinit var appDiscoveryRepository: AppDiscoveryRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var firewallManager: FirewallManager
        private set

    lateinit var revenueCatManager: RevenueCatManager
        private set

    lateinit var featureGate: FeatureGate
        private set

    lateinit var analyticsTracker: AnalyticsTracker
        private set

    lateinit var aiProvider: OptayAIProvider
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            OptayDatabase::class.java,
            "optay_database"
        ).fallbackToDestructiveMigration().build()

        appDiscoveryRepository = AppDiscoveryRepository(this)
        focusModeRepository = FocusModeRepository(database.focusModeDao(), appDiscoveryRepository)
        preferencesRepository = PreferencesRepository(this)
        firewallManager = FirewallManager(database.sessionHistoryDao())
        revenueCatManager = RevenueCatManager(this)
        featureGate = FeatureGate(revenueCatManager)
        analyticsTracker = AnalyticsTracker(this)
        aiProvider = OptayAIProvider()
    }

    companion object {
        lateinit var instance: OptayApplication
            private set
    }
}
