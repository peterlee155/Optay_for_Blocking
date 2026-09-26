package com.example.calls

import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.example.OptayApplication
import com.example.domain.model.CallBehavior

class OptayCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "OptayCallScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val responseBuilder = CallResponse.Builder()

        // Only filter incoming phone calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        // CRITICAL SAFETY: Never screen emergency calls
        val handle: Uri? = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        if (rawNumber.isNotBlank() && PhoneNumberUtils.isEmergencyNumber(rawNumber)) {
            Log.i(TAG, "Emergency number detected. Bypassing Optay call screening completely.")
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        val app = applicationContext as? OptayApplication
        val activeSession = app?.firewallManager?.activeSession?.value

        // If no mode is active or emergency override is on, allow call
        if (activeSession == null || activeSession.isEmergencyOverrideActive) {
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        val behavior = activeSession.callBehavior
        when (behavior) {
            CallBehavior.ALLOW_ALL -> {
                // Allow through normally
            }
            CallBehavior.SILENCE_UNKNOWN -> {
                // Private, hidden, or unknown callers silenced
                if (rawNumber.isBlank() || !isContactKnown(rawNumber)) {
                    silenceCall(responseBuilder)
                    Log.i(TAG, "Silenced unknown or private caller per active focus mode rule.")
                }
            }
            CallBehavior.ALLOW_FAVORITES_ONLY -> {
                if (rawNumber.isBlank() || !isContactStarred(rawNumber)) {
                    silenceCall(responseBuilder)
                    Log.i(TAG, "Silenced non-favorite caller per active focus mode rule.")
                }
            }
            CallBehavior.SILENCE_WORK_NUMBERS -> {
                if (rawNumber.isNotBlank() && isWorkContact(rawNumber)) {
                    silenceCall(responseBuilder)
                    Log.i(TAG, "Silenced work-related contact per focus rule.")
                }
            }
            CallBehavior.EMERGENCY_ONLY -> {
                if (rawNumber.isBlank() || !isContactStarred(rawNumber)) {
                    silenceCall(responseBuilder)
                    Log.i(TAG, "Silenced non-emergency caller per Deep Focus rule.")
                }
            }
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun silenceCall(builder: CallResponse.Builder) {
        builder.setSilenceCall(true)
        builder.setSkipNotification(true)
        builder.setSkipCallLog(false) // Retain in system call log so user can see missed calls
    }

    private fun isContactKnown(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isContactStarred(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.STARRED),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val starredIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                    starredIndex >= 0 && cursor.getInt(starredIndex) == 1
                } else false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isWorkContact(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.LABEL),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val typeIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)
                    val labelIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL)
                    val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else -1
                    val label = if (labelIdx >= 0) cursor.getString(labelIdx) ?: "" else ""
                    type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK ||
                        label.contains("work", ignoreCase = true)
                } else false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
