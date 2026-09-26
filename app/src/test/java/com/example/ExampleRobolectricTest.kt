package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calls.CallScreeningHelper
import com.example.core.firewall.ActiveSessionState
import com.example.data.repository.FocusModeRepository
import com.example.domain.model.CallBehavior
import com.example.domain.model.ScheduleRule
import com.example.domain.model.StrictModeType
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
    fun `verify wall clock session expiration logic`() {
        val now = 1000000L
        val activeSession = ActiveSessionState(
            sessionId = "test_session_1",
            modeId = "mode_work",
            modeName = "Work",
            blockedApps = setOf("com.instagram.android"),
            allowedApps = emptySet(),
            callBehavior = CallBehavior.ALLOW_ALL,
            startTimeMillis = now,
            endTargetTimeMillis = now + 60000L, // 60s in future
            isStrictMode = false,
            strictModeType = StrictModeType.CONFIRMATION_DELAY_10S
        )

        assertFalse(activeSession.isExpired(now))
        assertEquals(60L, activeSession.remainingSeconds(now))

        // Time passed target
        assertTrue(activeSession.isExpired(now + 60001L))
        assertEquals(0L, activeSession.remainingSeconds(now + 60001L))
    }

    @Test
    fun `verify schedule rule overnight and day formatting`() {
        val schedule = ScheduleRule(
            daysOfWeek = setOf(2, 3, 4, 5, 6),
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            isEnabled = true
        )
        val formatted = schedule.formatSchedule()
        assertTrue(formatted.contains("Mon–Fri"))
        assertTrue(formatted.contains("09:00"))
        assertTrue(formatted.contains("17:00"))
    }

    @Test
    fun `verify call screening helper handles context safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In Robolectric without telephony, should return false or safe boolean without throwing
        val hasContacts = CallScreeningHelper.hasContactsPermission(context)
        assertFalse(hasContacts) // Permission not granted by default in clean context
    }
}
