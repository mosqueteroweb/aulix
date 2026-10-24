package com.seguimiento.clases.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android elimina las alarmas programadas al reiniciar el dispositivo o actualizar la app.
 * Este receptor vuelve a programar la alarma activa guardada en preferencias.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> AlarmScheduler(context).restoreScheduledAlarm()
        }
    }
}
