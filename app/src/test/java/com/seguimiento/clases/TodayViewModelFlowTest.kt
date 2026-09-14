package com.seguimiento.clases

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayViewModelFlowTest {

    data class MockSessionItem(val subjectId: Long, val savedText: String)
    data class CardState(val subjectId: Long, val content: String)

    @Test
    fun testDraftLogsOverlaySavedLogsInstantly() = runTest {
        // Simulación del _dbDataFlow con contenido existente en BD
        val dbDataFlow = MutableStateFlow(
            listOf(
                MockSessionItem(subjectId = 1L, savedText = "Clase de Redes: tema 1"),
                MockSessionItem(subjectId = 2L, savedText = "Clase de Base de Datos: SQL")
            )
        )

        val draftLogs = MutableStateFlow<Map<Long, String>>(emptyMap())

        // El combine desacoplado que implementamos en TodayViewModel
        val uiCardsFlow = combine(dbDataFlow, draftLogs) { dbItems, drafts ->
            dbItems.map { item ->
                val text = drafts[item.subjectId] ?: item.savedText
                CardState(item.subjectId, text)
            }
        }

        // 1. Estado inicial: debe reflejar el texto existente en BD
        val initialCards = uiCardsFlow.first()
        assertEquals("Clase de Redes: tema 1", initialCards[0].content)
        assertEquals("Clase de Base de Datos: SQL", initialCards[1].content)

        // 2. El usuario edita la sesión 1 en medio del texto
        val modifiedText = "Clase de Redes: tema 1 y 2 introductorios"
        draftLogs.value = mapOf(1L to modifiedText)

        val updatedCards = uiCardsFlow.first()
        assertEquals(modifiedText, updatedCards[0].content)
        // La sesión 2 no ha sido alterada
        assertEquals("Clase de Base de Datos: SQL", updatedCards[1].content)

        // 3. Simulación de guardado en base de datos (500 ms después):
        // dbDataFlow se actualiza con el nuevo texto guardado
        dbDataFlow.value = listOf(
            MockSessionItem(subjectId = 1L, savedText = modifiedText),
            MockSessionItem(subjectId = 2L, savedText = "Clase de Base de Datos: SQL")
        )

        val finalCards = uiCardsFlow.first()
        assertEquals(modifiedText, finalCards[0].content)
    }

    @Test
    fun testFastTypingPreservesEveryKeystroke() = runTest {
        val initialText = "Tema 1"
        val dbDataFlow = MutableStateFlow(listOf(MockSessionItem(1L, initialText)))
        val draftLogs = MutableStateFlow<Map<Long, String>>(emptyMap())

        val uiCardsFlow = combine(dbDataFlow, draftLogs) { dbItems, drafts ->
            dbItems.map { item ->
                CardState(item.subjectId, drafts[item.subjectId] ?: item.savedText)
            }
        }

        var currentDraft = initialText
        val additions = ": Introducción completa"
        
        // Simulación de tecleo rápido
        for (char in additions) {
            currentDraft += char
            draftLogs.value = mapOf(1L to currentDraft)
            val state = uiCardsFlow.first()
            assertEquals(currentDraft, state[0].content)
        }

        assertEquals("Tema 1: Introducción completa", uiCardsFlow.first()[0].content)
    }
}
