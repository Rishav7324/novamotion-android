package com.novamotion.core.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.novamotion.R
import android.util.Log

/**
 * Foreground service for long-running video exports.
 * Prevents system from killing the app during MediaCodec encoding.
 *
 * Uses foregroundServiceType="dataSync" (declared in manifest) with 6h timeout
 * handling via onTimeout() on Android 15+.
 */
class ExportForegroundService : Service() {

    companion object {
        private const val TAG = "ExportFGS"
        private const val CHANNEL_ID = "novamotion_export_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL = "com.novamotion.action.CANCEL_EXPORT"

        fun start(context: Context) {
            val intent = Intent(context, ExportForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ExportForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = buildNotification("Exporting video...", 0, true)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
        return START_NOT_STICKY
    }

    fun updateProgress(progress: Float) {
        val pct = (progress * 100).toInt().coerceIn(0, 100)
        val notification = buildNotification("Exporting video... $pct%", pct, false)
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "notify progress failed", e)
        }
    }

    fun notifyComplete(success: Boolean, outputPath: String?) {
        val text = if (success) "Export complete" else "Export failed"
        val notification = buildNotification(text, 100, false, ongoing = false)
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID + 1, notification)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun buildNotification(text: String, progress: Int, indeterminate: Boolean, ongoing: Boolean = true): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NovaMotion")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play) // fallback; replace with app icon if available
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Video Export", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows video export progress" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        Log.w(TAG, "FGS timeout fgsType=$fgsType startId=$startId")
        // Must call stopSelf within a few seconds or system crashes the app
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
