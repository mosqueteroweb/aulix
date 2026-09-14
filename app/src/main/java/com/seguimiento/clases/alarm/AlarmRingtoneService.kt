package com.seguimiento.clases.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.seguimiento.clases.MainActivity
import com.seguimiento.clases.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmRingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var autoDismissJob: Job? = null

    companion object {
        const val CHANNEL_ID = "aulix_alarm_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START_RINGTONE = "com.seguimiento.clases.ACTION_START_RINGTONE"
        const val ACTION_STOP_RINGTONE = "com.seguimiento.clases.ACTION_STOP_RINGTONE"
        const val AUTO_DISMISS_TIMEOUT_MS = 60_000L // 1 minuto de timeout automático
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_RINGTONE) {
            stopRinging()
            stopSelf()
            return START_NOT_STICKY
        }

        // Iniciar alarma sonora, vibración y notificación
        acquireWakeLock()
        val notification = buildAlarmNotification()
        startForeground(NOTIFICATION_ID, notification)
        startAudioPlayback()
        startVibration()

        // Iniciar cuenta atrás de 1 minuto para auto-cancelación si el usuario no responde
        autoDismissJob?.cancel()
        autoDismissJob = serviceScope.launch {
            delay(AUTO_DISMISS_TIMEOUT_MS)
            // Cancelar automáticamente tras 1 minuto
            AlarmScheduler(applicationContext).onAlarmAutoExpired()
            stopRinging()
            stopSelf()
        }

        return START_STICKY
    }

    private fun startAudioPlayback() {
        if (mediaPlayer != null) return
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging() {
        autoDismissJob?.cancel()
        autoDismissJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "aulix:alarm_ringtone_wakelock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(AUTO_DISMISS_TIMEOUT_MS + 5000L)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmas de Aulix",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alarma activa con sonido y vibración"
                setSound(null, null) // El sonido lo reproduce el servicio con USAGE_ALARM en bucle
                enableVibration(false) // La vibración la controla el servicio
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildAlarmNotification(): Notification {
        // Intent para abrir la app al tocar la notificación
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            AlarmScheduler.REQUEST_CODE_SHOW,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: Posponer 5 min
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            AlarmScheduler.REQUEST_CODE_SNOOZE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: Cancelar
        val cancelIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_CANCEL_ALARM
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            AlarmScheduler.REQUEST_CODE_CANCEL,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ ¡Alarma de Aulix!")
            .setContentText("Alarma en curso. Toca para posponer o cancelar.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Posponer (+5 min)", snoozePendingIntent)
            .addAction(0, "Cancelar", cancelPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopRinging()
        serviceJob.cancel()
        super.onDestroy()
    }
}
