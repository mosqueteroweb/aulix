package com.seguimiento.clases

import com.seguimiento.clases.ui.screens.today.TodayViewModel.Companion.adjustToWeekday
import com.seguimiento.clases.ui.screens.today.TodayViewModel.Companion.selectedDateAfterDayChange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DateNavigationTest {

    private fun nextSchoolDay(date: LocalDate): LocalDate {
        var next = date.plusDays(1)
        while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
            next = next.plusDays(1)
        }
        return next
    }

    private fun previousSchoolDay(date: LocalDate): LocalDate {
        var prev = date.minusDays(1)
        while (prev.dayOfWeek == DayOfWeek.SATURDAY || prev.dayOfWeek == DayOfWeek.SUNDAY) {
            prev = prev.minusDays(1)
        }
        return prev
    }

    @Test
    fun testFridayAdvancesToMonday() {
        val friday = LocalDate.of(2026, 9, 18) // Friday
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)

        val nextDay = nextSchoolDay(friday)
        assertEquals(DayOfWeek.MONDAY, nextDay.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 21), nextDay)
    }

    @Test
    fun testMondayRetreatsToFriday() {
        val monday = LocalDate.of(2026, 9, 21) // Monday
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)

        val prevDay = previousSchoolDay(monday)
        assertEquals(DayOfWeek.FRIDAY, prevDay.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 18), prevDay)
    }

    @Test
    fun testWeekdayAdjustments() {
        val saturday = LocalDate.of(2026, 9, 19)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 21), adjustToWeekday(saturday))

        val sunday = LocalDate.of(2026, 9, 20)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 21), adjustToWeekday(sunday))

        val wednesday = LocalDate.of(2026, 9, 16)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
        assertEquals(wednesday, adjustToWeekday(wednesday))
    }

    @Test
    fun testMidnightMovesSelectionOnlyWhenViewingToday() {
        val tuesday = LocalDate.of(2026, 9, 22)
        val wednesday = LocalDate.of(2026, 9, 23)
        val lastFriday = LocalDate.of(2026, 9, 18)

        // Viendo el «hoy» anterior: pasa al nuevo día
        assertEquals(wednesday, selectedDateAfterDayChange(tuesday, tuesday, wednesday))
        // Navegando por otra fecha: se respeta la fecha elegida
        assertEquals(lastFriday, selectedDateAfterDayChange(lastFriday, tuesday, wednesday))

        // Viernes a medianoche: el nuevo «hoy» lectivo es el lunes
        val friday = LocalDate.of(2026, 9, 25)
        val newToday = adjustToWeekday(friday.plusDays(1))
        assertEquals(LocalDate.of(2026, 9, 28), selectedDateAfterDayChange(friday, friday, newToday))
    }
}
