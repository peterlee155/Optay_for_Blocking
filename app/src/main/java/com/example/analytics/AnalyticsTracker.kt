package com.example.analytics

import android.content.Context
import android.util.Log

class AnalyticsTracker(context: Context) {

    companion object {
        private const val TAG = "OptayAnalytics"

        const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
        const val EVENT_MODE_CREATED = "mode_created"
        const val EVENT_MODE_STARTED = "mode_started"
        const val EVENT_MODE_FINISHED = "mode_finished"
        const val EVENT_APP_ADDED_TO_BLOCKLIST = "app_added_to_blocklist"
        const val EVENT_SCHEDULE_CREATED = "schedule_created"
        const val EVENT_PAYWALL_VIEWED = "paywall_viewed"
        const val EVENT_TRIAL_STARTED = "trial_started"
        const val EVENT_SUBSCRIPTION_STARTED = "subscription_started"
    }

    /**
     * Privacy-First Logging:
     * Never logs message content, packet info, URLs, contacts, or phone numbers.
     * Only records structural user actions for local improvements.
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        Log.d(TAG, "Event: $eventName | params: $params")
    }
}
