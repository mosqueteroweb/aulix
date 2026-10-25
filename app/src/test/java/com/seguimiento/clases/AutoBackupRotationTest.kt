package com.seguimiento.clases

import com.seguimiento.clases.data.backup.AutoBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AutoBackupRotationTest {

    @Test
    fun testKeepsOnlyTheMostRecentAutomaticCopies() {
        val names = listOf(
            "aulix_copia_auto_20261011_100000.json",
            "aulix_copia_auto_20261108_100000.json",
            "aulix_copia_auto_20260927_100000.json",
            "aulix_copia_auto_20261025_100000.json"
        )
        assertEquals(listOf("aulix_copia_auto_20260927_100000.json"), AutoBackupManager.copiesToDelete(names, keep = 3))
    }

    @Test
    fun testNeverDeletesManualCopies() {
        val names = listOf(
            "aulix_copia_fin_de_curso_20260601.json",
            "seguimiento_clases_backup_20261001.json",
            "aulix_copia_auto_20261025_100000.json"
        )
        assertTrue(AutoBackupManager.copiesToDelete(names, keep = 0).all { it.startsWith(AutoBackupManager.FILE_PREFIX) })
        assertEquals(1, AutoBackupManager.copiesToDelete(names, keep = 0).size)
    }

    @Test
    fun testFileNameSortsInChronologicalOrder() {
        val earlier = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 9, 23, 59, 59) }.timeInMillis
        val later = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 0, 0, 1) }.timeInMillis
        val first = AutoBackupManager.fileNameFor(earlier)
        val second = AutoBackupManager.fileNameFor(later)
        assertEquals("aulix_copia_auto_20261009_235959.json", first)
        assertTrue(second > first)
    }
}
