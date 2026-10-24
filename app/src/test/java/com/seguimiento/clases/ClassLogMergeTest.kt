package com.seguimiento.clases

import com.seguimiento.clases.data.repository.ClassRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassLogMergeTest {

    @Test
    fun testNewContentIsAppendedToExistingLog() {
        val merged = ClassRepository.mergeLogContent("Tema 1: redes", "Tema 2: subredes")
        assertEquals("Tema 1: redes\n\nTema 2: subredes", merged)
    }

    @Test
    fun testWithoutExistingLogKeepsNewContent() {
        assertEquals("Tema 1", ClassRepository.mergeLogContent(null, "Tema 1"))
        assertEquals("Tema 1", ClassRepository.mergeLogContent("   ", " Tema 1 "))
    }

    @Test
    fun testBlankAdditionKeepsExistingContent() {
        assertEquals("Tema 1", ClassRepository.mergeLogContent("Tema 1\n", "  "))
    }
}
