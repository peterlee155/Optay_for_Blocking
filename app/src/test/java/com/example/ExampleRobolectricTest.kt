package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.Feature
import com.example.billing.FeatureGate
import com.example.billing.RevenueCatManager
import com.example.data.repository.FocusModeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Optay", appName)
    }

    @Test
    fun `verify default focus modes exist`() {
        val defaultModes = FocusModeRepository.DEFAULT_MODES
        assertTrue(defaultModes.isNotEmpty())
        val offDay = defaultModes.find { it.name == "Off Day" }
        assertNotNull(offDay)
        assertTrue(offDay!!.blockedApps.contains("com.whatsapp"))
        assertTrue(offDay.blockedApps.contains("com.instagram.android"))
    }

    @Test
    fun `verify feature gate limits on free tier`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val revenueCatManager = RevenueCatManager(context)
        revenueCatManager.setProStatus(false)
        val featureGate = FeatureGate(revenueCatManager)

        // Free tier allows 1 custom mode
        assertTrue(featureGate.canUse(Feature.UNLIMITED_MODES, currentCustomModesCount = 0))
        assertFalse(featureGate.canUse(Feature.UNLIMITED_MODES, currentCustomModesCount = 1))

        // AI commands and strict mode require Pro
        assertFalse(featureGate.canUse(Feature.AI_COMMANDS))

        // Upgrade to Pro
        revenueCatManager.setProStatus(true)
        assertTrue(featureGate.canUse(Feature.AI_COMMANDS))
        assertTrue(featureGate.canUse(Feature.UNLIMITED_MODES, currentCustomModesCount = 5))
    }
}
