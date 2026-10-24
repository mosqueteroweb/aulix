package com.seguimiento.clases

import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.util.SubjectCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectCodesTest {

    private val subjects = listOf(
        SubjectEntity(id = 1, code = "DI"),
        SubjectEntity(id = 2, code = "ASI", isArchived = true)
    )

    @Test
    fun testDuplicateCodeIsDetectedIgnoringCaseAndSpaces() {
        assertTrue(SubjectCodes.isTaken(" di ", subjects))
        // También cuenta un módulo archivado
        assertTrue(SubjectCodes.isTaken("asi", subjects))
        assertFalse(SubjectCodes.isTaken("SI", subjects))
        assertFalse(SubjectCodes.isTaken("   ", subjects))
    }

    @Test
    fun testEditingKeepsItsOwnCode() {
        assertFalse(SubjectCodes.isTaken("di", subjects, editingSubjectId = 1))
        assertTrue(SubjectCodes.isTaken("ASI", subjects, editingSubjectId = 1))
    }

    @Test
    fun testNormalize() {
        assertEquals("AFH", SubjectCodes.normalize("  afh "))
    }
}
