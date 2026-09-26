package com.example.core.firebase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.SessionHistoryEntity
import com.example.domain.model.CallBehavior
import com.example.domain.model.FocusMode
import com.example.domain.model.ScheduleRule
import com.example.domain.model.StrictModeType
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FirebaseSyncState {
    object Idle : FirebaseSyncState()
    object Syncing : FirebaseSyncState()
    data class Success(val message: String) : FirebaseSyncState()
    data class Error(val error: String) : FirebaseSyncState()
}

class FirebaseManager(private val context: Context) {

    companion object {
        private const val TAG = "OptayFirebase"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncState = MutableStateFlow<FirebaseSyncState>(FirebaseSyncState.Idle)
    val syncState: StateFlow<FirebaseSyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                // Check if options are configured via environment or fallback
                val projectId = getBuildConfigValue("FIREBASE_PROJECT_ID").ifBlank { "optay-app" }
                val appId = getBuildConfigValue("FIREBASE_APP_ID").ifBlank { "1:1234567890:android:abcdef" }
                val apiKey = getBuildConfigValue("FIREBASE_API_KEY").ifBlank { "AIzaSyFakeKeyForPreviewInit0000000" }

                val options = FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                apps[0]
            }

            if (app != null) {
                auth = FirebaseAuth.getInstance(app)
                firestore = FirebaseFirestore.getInstance(app)
                _currentUser.value = auth?.currentUser
                _isConfigured.value = true

                auth?.addAuthStateListener { firebaseAuth ->
                    _currentUser.value = firebaseAuth.currentUser
                }
                Log.i(TAG, "Firebase initialized successfully.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
            _isConfigured.value = false
        }
    }

    fun signInAnonymously(onComplete: (Boolean, String) -> Unit) {
        val a = auth
        if (a == null) {
            onComplete(false, "Firebase is not configured yet. Check credentials.")
            return
        }

        a.signInAnonymously()
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                onComplete(true, "Signed in anonymously as ${result.user?.uid?.take(8)}...")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Anonymous auth failed: ${e.message}")
                onComplete(false, e.localizedMessage ?: "Authentication failed")
            }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }

    fun backupProfilesToCloud(
        profiles: List<FocusMode>,
        onResult: (Boolean, String) -> Unit
    ) {
        val db = firestore
        val user = _currentUser.value
        if (db == null) {
            onResult(false, "Firestore is unavailable.")
            return
        }

        val uid = user?.uid ?: "local_guest_backup"
        _syncState.value = FirebaseSyncState.Syncing

        scope.launch {
            try {
                val batch = db.batch()
                val userProfilesRef = db.collection("users").document(uid).collection("focus_profiles")

                for (profile in profiles) {
                    val docRef = userProfilesRef.document(profile.id)
                    val data = hashMapOf(
                        "id" to profile.id,
                        "name" to profile.name,
                        "description" to profile.description,
                        "iconName" to profile.iconName,
                        "colorHex" to profile.colorHex,
                        "blockedApps" to profile.blockedApps.toList(),
                        "allowedApps" to profile.allowedApps.toList(),
                        "callBehavior" to profile.callBehavior.name,
                        "isStrictMode" to profile.isStrictMode,
                        "strictModeType" to profile.strictModeType.name,
                        "durationMinutes" to profile.durationMinutes,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }

                batch.commit().await()
                _lastSyncTime.value = System.currentTimeMillis()
                val msg = "Successfully backed up ${profiles.size} profiles to Cloud Firestore."
                _syncState.value = FirebaseSyncState.Success(msg)
                onResult(true, msg)
            } catch (e: Exception) {
                Log.e(TAG, "Backup to Firestore failed", e)
                val err = e.localizedMessage ?: "Failed to backup profiles to Cloud"
                _syncState.value = FirebaseSyncState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun restoreProfilesFromCloud(onResult: (Boolean, List<FocusMode>, String) -> Unit) {
        val db = firestore
        val user = _currentUser.value
        if (db == null) {
            onResult(false, emptyList(), "Firestore is unavailable.")
            return
        }

        val uid = user?.uid ?: "local_guest_backup"
        _syncState.value = FirebaseSyncState.Syncing

        scope.launch {
            try {
                val snapshot = db.collection("users").document(uid).collection("focus_profiles").get().await()
                val restored = mutableListOf<FocusMode>()

                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Profile"
                    val desc = doc.getString("description") ?: ""
                    val icon = doc.getString("iconName") ?: "shield"
                    val color = doc.getString("colorHex") ?: "#06B6D4"
                    val blocked = (doc.get("blockedApps") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    val allowed = (doc.get("allowedApps") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    val call = try {
                        CallBehavior.valueOf(doc.getString("callBehavior") ?: "ALLOW_ALL")
                    } catch (e: Exception) {
                        CallBehavior.ALLOW_ALL
                    }
                    val strict = doc.getBoolean("isStrictMode") ?: false
                    val strictType = try {
                        StrictModeType.valueOf(doc.getString("strictModeType") ?: "CONFIRMATION_DELAY_10S")
                    } catch (e: Exception) {
                        StrictModeType.CONFIRMATION_DELAY_10S
                    }
                    val duration = doc.getLong("durationMinutes")?.toInt() ?: 120

                    restored.add(
                        FocusMode(
                            id = id,
                            name = name,
                            description = desc,
                            iconName = icon,
                            colorHex = color,
                            blockedApps = blocked,
                            allowedApps = allowed,
                            callBehavior = call,
                            isStrictMode = strict,
                            strictModeType = strictType,
                            durationMinutes = duration,
                            schedule = null,
                            isPredefined = false
                        )
                    )
                }

                _lastSyncTime.value = System.currentTimeMillis()
                val msg = "Restored ${restored.size} profiles from Cloud Firestore."
                _syncState.value = FirebaseSyncState.Success(msg)
                onResult(true, restored, msg)
            } catch (e: Exception) {
                Log.e(TAG, "Restore from Firestore failed", e)
                val err = e.localizedMessage ?: "Failed to restore profiles from Cloud"
                _syncState.value = FirebaseSyncState.Error(err)
                onResult(false, emptyList(), err)
            }
        }
    }

    private fun getBuildConfigValue(fieldName: String): String {
        return try {
            val field = BuildConfig::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(null)?.toString() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }
}
