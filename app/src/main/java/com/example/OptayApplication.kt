package com.example

import android.app.Application
import androidx.room.Room
import com.example.ai.OptayAIProvider
import com.example.analytics.AnalyticsTracker
import com.example.billing.FeatureGate
import com.example.billing.RevenueCatManager
import com.example.core.firewall.FirewallManager
import com.example.core.schedule.ScheduleManager
import com.example.data.db.OptayDatabase
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.FocusModeRepository
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    lateinit var firebaseManager: com.example.core.firebase.FirebaseManager
        private set

    lateinit var analyticsTracker: AnalyticsTracker
        private set

    lateinit var aiProvider: OptayAIProvider
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Safe Room migration from v1 to v2 with fallback for development builds
        database = Room.databaseBuilder(
            applicationContext,
            OptayDatabase::class.java,
            "optay_database"
        )
            .addMigrations(OptayDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

        appDiscoveryRepository = AppDiscoveryRepository(this)
        focusModeRepository = FocusModeRepository(database.focusModeDao(), appDiscoveryRepository)
        preferencesRepository = PreferencesRepository(this)
        firewallManager = FirewallManager(database.sessionHistoryDao(), database.activeSessionDao())

        // Initialize real RevenueCat Purchases
        revenueCatManager = RevenueCatManager(this)
        featureGate = FeatureGate(revenueCatManager)
        firebaseManager = com.example.core.firebase.FirebaseManager(this)
        analyticsTracker = AnalyticsTracker(this)
        aiProvider = OptayAIProvider()

        // Persistent Session Recovery & Schedule arming
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Restore valid session if active prior to process destruction
                firewallManager.restorePersistedSession(this@OptayApplication)

                // Arm scheduled profile alarms
                val modes = focusModeRepository.focusModes.first()
                for (mode in modes) {
                    if (mode.schedule?.isEnabled == true) {
                        ScheduleManager.scheduleNextForMode(this@OptayApplication, mode)
                    }
                }
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    companion object {
        lateinit var instance: OptayApplication
            private set
    }
}
