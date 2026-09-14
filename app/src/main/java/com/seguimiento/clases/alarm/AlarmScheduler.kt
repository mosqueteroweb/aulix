package com.seguimiento.clases.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.seguimiento.clases.MainActivity
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "aulix_alarm_prefs"
        private const val KEY_IS_ACTIVE = "key_is_alarm_active"
        private const val KEY_TRIGGER_MILLIS = "key_trigger_millis"
        private const val KEY_HOUR = "key_alarm_hour"
        private const val KEY_MINUTE = "key_alarm_minute"

        const val ACTION_TRIGGER_ALARM = "com.seguimiento.clases.ACTION_TRIGGER_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.seguimiento.clases.ACTION_SNOOZE_ALARM"
        const val ACTION_CANCEL_ALARM = "com.seguimiento.clases.ACTION_CANCEL_ALARM"

        const val REQUEST_CODE_ALARM = 1001
        const val REQUEST_CODE_SHOW = 1002
        const val REQUEST_CODE_SNOOZE = 1003
        const val REQUEST_CODE_CANCEL = 1004

        /**
         * Calcula el epoch timestamp para la próxima ocurrencia de una hora y minuto dados.
         * Si la hora ya pasó hoy, se programa para mañana.
         */
        fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val target = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        /**
         * Formatea el tiempo restante de forma legible en español.
         */
        fun formatRemainingTime(triggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
            val diffMillis = triggerMillis - nowMillis
            if (diffMillis <= 0) return "Sonará en unos instantes"

            val diffMinutes = (diffMillis / (1000 * 60)).toInt()
            val hours = diffMinutes / 60
            val minutes = diffMinutes % 60

            val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
            val targetHour = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

            val isTomorrow = diffMillis > (24 * 60 * 60 * 1000L - (nowMillis % (24 * 60 * 60 * 1000L)))
            val dayPrefix = if (isTomorrow || hours >= 18) "mañana " else "hoy "

            return when {
                hours == 0 && minutes == 0 -> "Sonará en menos de 1 minuto"
                hours == 0 -> "Sonará en $minutes min (${dayPrefix}a las $targetHour)"
                minutes == 0 -> "Sonará en $hours h (${dayPrefix}a las $targetHour)"
                else -> "Sonará en $hours h y $minutes min (${dayPrefix}a las $targetHour)"
            }
        }
    }

    /**
     * Programa una alarma a una hora y minuto determinados.
     */
    fun scheduleAlarm(hour: Int, minute: Int): Long {
        val triggerMillis = calculateNextTriggerMillis(hour, minute)
        setAlarmExact(triggerMillis)
        saveState(isActive = true, triggerMillis = triggerMillis, hour = hour, minute = minute)
        return triggerMillis
    }

    /**
     * Pospone la alarma activa durante los minutos especificados (por defecto 5 minutos).
     */
    fun snoozeAlarm(minutes: Int = 5): Long {
        val triggerMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        setAlarmExact(triggerMillis)
        saveState(isActive = true, triggerMillis = triggerMillis, hour = hour, minute = minute)
        return triggerMillis
    }

    /**
     * Cancela cualquier alarma programada en el sistema y limpia el estado persistido.
     */
    fun cancelAlarm() {
        val pendingIntent = createAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        saveState(isActive = false, triggerMillis = 0L, hour = getSavedHour(), minute = getSavedMinute())
    }

    /**
     * Se invoca cuando la alarma expiró automáticamente (por ejemplo tras 1 minuto sin respuesta).
     */
    fun onAlarmAutoExpired() {
        saveState(isActive = false, triggerMillis = 0L, hour = getSavedHour(), minute = getSavedMinute())
    }

    private fun setAlarmExact(triggerMillis: Long) {
        val pendingIntent = createAlarmPendingIntent()

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun saveState(isActive: Boolean, triggerMillis: Long, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, isActive)
            .putLong(KEY_TRIGGER_MILLIS, triggerMillis)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun isAlarmActive(): Boolean {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val triggerMillis = prefs.getLong(KEY_TRIGGER_MILLIS, 0L)
        // Si el tiempo ya pasó hace más de 2 minutos y no se pospuso, limpiar estado
        if (isActive && triggerMillis > 0 && triggerMillis < (System.currentTimeMillis() - 120_000L)) {
            saveState(isActive = false, triggerMillis = 0L, hour = getSavedHour(), minute = getSavedMinute())
            return false
        }
        return isActive
    }

    fun getTriggerMillis(): Long = prefs.getLong(KEY_TRIGGER_MILLIS, 0L)

    fun getSavedHour(): Int {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return prefs.getInt(KEY_HOUR, currentHour)
    }

    fun getSavedMinute(): Int {
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        return prefs.getInt(KEY_MINUTE, currentMinute)
    }
}
