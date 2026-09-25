package com.example.core.firewall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OptayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var trafficJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "OptayVpnService"
        const val ACTION_START = "com.example.optay.vpn.START"
        const val ACTION_STOP = "com.example.optay.vpn.STOP"

        const val EXTRA_MODE_ID = "extra_mode_id"
        const val EXTRA_MODE_NAME = "extra_mode_name"
        const val EXTRA_BLOCKED_PACKAGES = "extra_blocked_packages"
        const val EXTRA_ALLOWED_PACKAGES = "extra_allowed_packages"
        const val EXTRA_END_TIME = "extra_end_time"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "optay_firewall_channel"

        fun start(
            context: Context,
            modeId: String,
            modeName: String,
            blockedPackages: ArrayList<String>,
            allowedPackages: ArrayList<String>,
            endTimeMillis: Long
        ) {
            val intent = Intent(context, OptayVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE_ID, modeId)
                putExtra(EXTRA_MODE_NAME, modeName)
                putStringArrayListExtra(EXTRA_BLOCKED_PACKAGES, blockedPackages)
                putStringArrayListExtra(EXTRA_ALLOWED_PACKAGES, allowedPackages)
                putExtra(EXTRA_END_TIME, endTimeMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OptayVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val modeName = intent.getStringExtra(EXTRA_MODE_NAME) ?: "Off Day"
                val blockedPackages = intent.getStringArrayListExtra(EXTRA_BLOCKED_PACKAGES) ?: arrayListOf()
                startFirewall(modeName, blockedPackages)
            }
            ACTION_STOP -> {
                stopFirewall()
            }
        }
        return START_NOT_STICKY
    }

    private fun startFirewall(modeName: String, blockedPackages: List<String>) {
        if (isRunning.get()) {
            stopFirewallInternal()
        }

        try {
            val builder = Builder()
                .setSession("Optay: $modeName")
                .addAddress("10.1.0.1", 32)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            // Configure local application boundaries:
            // Route ONLY the selected blocked applications to our local black-hole VPN TUN interface.
            // All other non-listed apps completely bypass the VPN interface and maintain normal direct network access.
            var appsRouted = 0
            val pm = packageManager
            for (pkg in blockedPackages) {
                try {
                    // Check package exists on this device
                    pm.getPackageInfo(pkg, 0)
                    builder.addAllowedApplication(pkg)
                    appsRouted++
                } catch (e: PackageManager.NameNotFoundException) {
                    // Package not installed, ignore
                } catch (e: Exception) {
                    Log.w(TAG, "Failed adding package to VPN restriction: $pkg", e)
                }
            }

            // Establish the local VPN interface
            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (null)")
                stopSelf()
                return
            }

            isRunning.set(true)
            startForegroundNotification(modeName, appsRouted)
            startPacketDrainer()
            Log.i(TAG, "Optay local firewall active. Restricting $appsRouted apps.")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting Optay local firewall", e)
            stopSelf()
        }
    }

    private fun startPacketDrainer() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)

        trafficJob?.cancel()
        trafficJob = serviceScope.launch {
            val buffer = ByteArray(32767)
            try {
                while (isActive && isRunning.get()) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    // Deliberate sink: Any packet from blocked apps is swallowed locally.
                    // No packets are sent to third parties or remote servers.
                }
            } catch (e: IOException) {
                // Tunnel closed normally or connection reset
            } catch (e: Exception) {
                Log.w(TAG, "Drainer exception", e)
            }
        }
    }

    private fun stopFirewall() {
        stopFirewallInternal()
        stopSelf()
    }

    private fun stopFirewallInternal() {
        isRunning.set(false)
        trafficJob?.cancel()
        trafficJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification(modeName: String, appsCount: Int) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Optay Active • $modeName")
            .setContentText("$appsCount apps offline. Tap to manage.")
            .setSmallIcon(R.drawable.ic_optay_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFirewallInternal()
    }
}
