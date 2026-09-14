package com.seguimiento.clases

import com.seguimiento.clases.alarm.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun testCalculateNextTriggerMillis_sameDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = cal.timeInMillis

        // Programar para las 14:30 (más tarde en el mismo día)
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(14, 30, nowMillis)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(14, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
        assertEquals(cal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testCalculateNextTriggerMillis_nextDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = cal.timeInMillis

        // Programar para las 08:00 (hora ya pasada hoy, debe ser mañana)
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(8, 0, nowMillis)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertTrue("El disparo debe ser posterior a la hora actual", triggerMillis > nowMillis)
        assertEquals(cal.get(Calendar.DAY_OF_YEAR) + 1, triggerCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testFormatRemainingTime() {
        val now = System.currentTimeMillis()

        // 30 minutos después
        val in30Min = now + (30 * 60 * 1000L)
        val formatted30 = AlarmScheduler.formatRemainingTime(in30Min, now)
        assertTrue(formatted30.contains("30 min"))

        // 2 horas y 15 minutos después
        val in2h15 = now + (135 * 60 * 1000L)
        val formatted2h = AlarmScheduler.formatRemainingTime(in2h15, now)
        assertTrue(formatted2h.contains("2 h"))
        assertTrue(formatted2h.contains("15 min"))
    }
}
