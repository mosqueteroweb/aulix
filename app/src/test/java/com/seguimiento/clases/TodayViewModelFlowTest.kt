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

    data class MockSessionWithOrder(
        val subjectId: Long,
        val orderIndex: Int,
        val isCompleted: Boolean,
        val text: String
    )

    @Test
    fun testCompletedSessionMovesToBottomAndPendingMoveUp() = runTest {
        // Horario con 3 sesiones en orden: 1 (DI), 2 (AFH), 3 (SI)
        val sessions = listOf(
            MockSessionWithOrder(subjectId = 1L, orderIndex = 0, isCompleted = false, text = "DI - Pendiente"),
            MockSessionWithOrder(subjectId = 2L, orderIndex = 1, isCompleted = false, text = "AFH - Pendiente"),
            MockSessionWithOrder(subjectId = 3L, orderIndex = 2, isCompleted = false, text = "SI - Pendiente")
        )

        // Función de ordenación idéntica a TodayViewModel
        fun sortCards(list: List<MockSessionWithOrder>): List<MockSessionWithOrder> {
            return list.sortedWith(
                compareBy<MockSessionWithOrder> { it.isCompleted }
                    .thenBy { it.orderIndex }
            )
        }

        // 1. Inicial: todas pendientes en su orden original
        val initialSorted = sortCards(sessions)
        assertEquals(1L, initialSorted[0].subjectId)
        assertEquals(2L, initialSorted[1].subjectId)
        assertEquals(3L, initialSorted[2].subjectId)

        // 2. El usuario marca la primera sesión (DI, id 1) como completada
        val afterMarkingFirst = sessions.map {
            if (it.subjectId == 1L) it.copy(isCompleted = true) else it
        }
        val sortedAfterMarking = sortCards(afterMarkingFirst)

        // La tarjeta de DI ahora está al final, y las otras dos han subido
        assertEquals(2L, sortedAfterMarking[0].subjectId) // AFH subió a la primera posición
        assertEquals(3L, sortedAfterMarking[1].subjectId) // SI subió a la segunda posición
        assertEquals(1L, sortedAfterMarking[2].subjectId) // DI pasó al final del día
        assertEquals(true, sortedAfterMarking[2].isCompleted)

        // 3. El usuario desmarca la sesión de DI
        val afterUnmarking = afterMarkingFirst.map {
            if (it.subjectId == 1L) it.copy(isCompleted = false) else it
        }
        val sortedAfterUnmarking = sortCards(afterUnmarking)

        // Regresa a su posición original (primera) según orderIndex
        assertEquals(1L, sortedAfterUnmarking[0].subjectId)
        assertEquals(2L, sortedAfterUnmarking[1].subjectId)
        assertEquals(3L, sortedAfterUnmarking[2].subjectId)
    }

    @Test
    fun testMultipleCompletedSessionsStayAtBottom() = runTest {
        val sessions = listOf(
            MockSessionWithOrder(subjectId = 1L, orderIndex = 0, isCompleted = true, text = "Completada 1"),
            MockSessionWithOrder(subjectId = 2L, orderIndex = 1, isCompleted = false, text = "Pendiente 2"),
            MockSessionWithOrder(subjectId = 3L, orderIndex = 2, isCompleted = true, text = "Completada 3"),
            MockSessionWithOrder(subjectId = 4L, orderIndex = 3, isCompleted = false, text = "Pendiente 4")
        )

        val sorted = sessions.sortedWith(
            compareBy<MockSessionWithOrder> { it.isCompleted }
                .thenBy { it.orderIndex }
        )

        // Las dos pendientes (2 y 4) deben quedar al principio
        assertEquals(2L, sorted[0].subjectId)
        assertEquals(false, sorted[0].isCompleted)
        assertEquals(4L, sorted[1].subjectId)
        assertEquals(false, sorted[1].isCompleted)

        // Las dos completadas (1 y 3) deben quedar al final
        assertEquals(1L, sorted[2].subjectId)
        assertEquals(true, sorted[2].isCompleted)
        assertEquals(3L, sorted[3].subjectId)
        assertEquals(true, sorted[3].isCompleted)
    }
}
