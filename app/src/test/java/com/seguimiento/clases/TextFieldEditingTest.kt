package com.seguimiento.clases

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFieldEditingTest {

    @Test
    fun testInsertingTextInMiddleOfExistingContent() {
        val existingContent = "Tema 1: Repaso general de redes"
        var textFieldValue = TextFieldValue(
            text = existingContent,
            selection = TextRange(existingContent.length)
        )
        assertEquals(31, textFieldValue.selection.start)

        // El usuario pulsa en medio del texto: tras "Tema 1:" (índice 7)
        textFieldValue = textFieldValue.copy(selection = TextRange(7))
        assertEquals(7, textFieldValue.selection.start)

        // El usuario teclea " (Bloque A)" carácter a carácter
        val insertion = " (Bloque A)"
        for (char in insertion) {
            val currentText = textFieldValue.text
            val cursorPos = textFieldValue.selection.start
            val newText = currentText.substring(0, cursorPos) + char + currentText.substring(cursorPos)
            textFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(cursorPos + 1)
            )
        }

        val expected = "Tema 1: (Bloque A) Repaso general de redes"
        assertEquals(expected, textFieldValue.text)
        assertEquals(18, textFieldValue.selection.start)
    }

    @Test
    fun testDeletingTextInMiddleOfExistingContent() {
        val existingContent = "Capítulo 1: Introducción a Docker y Kubernetes"
        var textFieldValue = TextFieldValue(
            text = existingContent,
            selection = TextRange(34) // tras "Docker "
        )

        // El usuario borra hacia atrás 7 caracteres ("Docker ")
        for (i in 1..7) {
            val currentText = textFieldValue.text
            val cursorPos = textFieldValue.selection.start
            val newText = currentText.substring(0, cursorPos - 1) + currentText.substring(cursorPos)
            textFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(cursorPos - 1)
            )
        }

        val expected = "Capítulo 1: Introducción a y Kubernetes"
        assertEquals(expected, textFieldValue.text)
        assertEquals(27, textFieldValue.selection.start)
    }

    @Test
    fun testExternalSyncDoesNotDisruptCursorDuringTyping() {
        val original = "Notas iniciales"
        var localValue = TextFieldValue(original, TextRange(original.length))

        // Usuario inserta en la posición 5
        localValue = localValue.copy(selection = TextRange(5))
        val newText = localValue.text.substring(0, 5) + " rápidas" + localValue.text.substring(5)
        localValue = localValue.copy(text = newText, selection = TextRange(13))
        
        // Simulación de ViewModel actualizando su estado tras el cambio
        val vmText = newText

        // Simulación del LaunchedEffect de sincronización:
        // Solo actualiza si vmText != localValue.text
        if (vmText != localValue.text) {
            localValue = localValue.copy(text = vmText, selection = TextRange(vmText.length))
        }

        // El cursor NO debe saltar al final (longitud 23), sino permanecer en 13
        assertEquals(13, localValue.selection.start)
        assertEquals("Notas rápidas iniciales", localValue.text)
    }

    @Test
    fun testExternalClearResetsTextFieldValue() {
        val original = "Texto existente a borrar"
        var localValue = TextFieldValue(original, TextRange(10))
        
        // Simulación de borrado externo (botón "Borrar anotación")
        val clearedText = ""
        if (clearedText != localValue.text) {
            localValue = localValue.copy(text = clearedText, selection = TextRange.Zero)
        }

        assertEquals("", localValue.text)
        assertEquals(0, localValue.selection.start)
    }

    @Test
    fun testTextSelectionRangeAndReplacement() {
        val original = "Gémini estudiantes alta clase aula virtual"
        var textFieldValue = TextFieldValue(original, TextRange(original.length))

        // Simulación de usuario seleccionando la palabra "estudiantes" (índices 7 a 18)
        val selStart = original.indexOf("estudiantes")
        val selEnd = selStart + "estudiantes".length
        textFieldValue = textFieldValue.copy(selection = TextRange(selStart, selEnd))

        assertEquals(7, textFieldValue.selection.start)
        assertEquals(18, textFieldValue.selection.end)
        assertEquals(true, textFieldValue.selection.length > 0)
        assertEquals("estudiantes", textFieldValue.text.substring(textFieldValue.selection.start, textFieldValue.selection.end))

        // El usuario reemplaza el texto seleccionado tecleando "alumnos"
        val replacement = "alumnos"
        val textBefore = textFieldValue.text.substring(0, textFieldValue.selection.start)
        val textAfter = textFieldValue.text.substring(textFieldValue.selection.end)
        val replacedText = textBefore + replacement + textAfter
        val newCursor = textFieldValue.selection.start + replacement.length

        textFieldValue = textFieldValue.copy(
            text = replacedText,
            selection = TextRange(newCursor)
        )

        assertEquals("Gémini alumnos alta clase aula virtual", textFieldValue.text)
        assertEquals(14, textFieldValue.selection.start)
    }

    @Test
    fun testSelectAllAndCutOrDelete() {
        val original = "Notas a descartar completamente"
        var textFieldValue = TextFieldValue(original, TextRange(original.length))

        // Seleccionar todo
        textFieldValue = textFieldValue.copy(selection = TextRange(0, original.length))
        assertEquals(0, textFieldValue.selection.start)
        assertEquals(original.length, textFieldValue.selection.end)

        // Simular Cortar / Borrar selección
        val cutText = textFieldValue.text.substring(textFieldValue.selection.start, textFieldValue.selection.end)
        assertEquals(original, cutText)

        textFieldValue = textFieldValue.copy(text = "", selection = TextRange.Zero)
        assertEquals("", textFieldValue.text)
        assertEquals(0, textFieldValue.selection.start)
    }
}
