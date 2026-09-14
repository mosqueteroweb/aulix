package com.seguimiento.clases.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            AlarmScheduler.ACTION_TRIGGER_ALARM -> {
                // Iniciar el servicio que reproduce sonido, vibración y muestra la notificación
                val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
                    this.action = AlarmRingtoneService.ACTION_START_RINGTONE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            AlarmScheduler.ACTION_SNOOZE_ALARM -> {
                // Detener el sonido actual
                stopRingtoneService(context)
                // Posponer 5 minutos la alarma
                AlarmScheduler(context).snoozeAlarm(5)
            }

            AlarmScheduler.ACTION_CANCEL_ALARM -> {
                // Detener el sonido actual
                stopRingtoneService(context)
                // Cancelar y limpiar estado
                AlarmScheduler(context).cancelAlarm()
            }
        }
    }

    private fun stopRingtoneService(context: Context) {
        val stopIntent = Intent(context, AlarmRingtoneService::class.java).apply {
            action = AlarmRingtoneService.ACTION_STOP_RINGTONE
        }
        context.startService(stopIntent)
    }
}
