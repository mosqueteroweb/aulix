package com.seguimiento.clases.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import java.io.OutputStream

/**
 * Dibuja un [CourseReport] como PDF en tamaño A4 con la API de Android (sin librerías externas).
 * Los bloques largos se reparten entre páginas por líneas completas y los títulos nunca se quedan
 * solos al final de una página.
 */
class PdfReportWriter {

    private val pageWidth = 595 // A4 en puntos (1/72 pulgada)
    private val pageHeight = 842
    private val margin = 48f
    private val footerSpace = 28f
    private val contentWidth = (pageWidth - 2 * margin).toInt()
    private val bottomLimit = pageHeight - margin - footerSpace

    private val titlePaint = textPaint(20f, bold = true, color = 0xFF1E3A8A.toInt())
    private val metaPaint = textPaint(10f, color = 0xFF475569.toInt())
    private val modulePaint = textPaint(15f, bold = true, color = 0xFF1E3A8A.toInt())
    private val sectionPaint = textPaint(11.5f, bold = true, color = 0xFF334155.toInt())
    private val bodyPaint = textPaint(10.5f)
    private val emptyPaint = textPaint(10f, color = 0xFF64748B.toInt()).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }
    private val footerPaint = textPaint(8.5f, color = 0xFF94A3B8.toInt())
    private val rulePaint = Paint().apply { color = 0xFFCBD5E1.toInt(); strokeWidth = 1f }

    private lateinit var document: PdfDocument
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f

    /** Escribe el PDF en [output] y devuelve el número de páginas. */
    fun write(report: CourseReport, output: OutputStream): Int {
        document = PdfDocument()
        pageNumber = 0
        try {
            newPage()
            block(layout("Registro del curso", titlePaint), spaceAfter = 4f)
            val meta = listOfNotNull(report.period, "Generado el ${report.generatedOn} con Aulix").joinToString("  ·  ")
            block(layout(meta, metaPaint), spaceAfter = 14f)

            if (report.modules.isEmpty()) {
                block(layout("No hay módulos en este curso.", emptyPaint), spaceAfter = 8f)
            }
            report.modules.forEach { module -> moduleSection(module) }

            report.pinnedNote?.let { note ->
                rule()
                heading(layout("Nota fija", modulePaint), spaceAfter = 6f)
                block(layout(note, bodyPaint), spaceAfter = 8f)
            }

            finishPage()
            document.writeTo(output)
            return pageNumber
        } finally {
            document.close()
        }
    }

    private fun moduleSection(module: ModuleReport) {
        rule()
        val title = if (module.isArchived) "${module.title} (archivado)" else module.title
        heading(layout(title, modulePaint), spaceAfter = 2f)
        module.schedule?.let { block(layout("Horario: $it", metaPaint), spaceAfter = 8f) } ?: run { y += 6f }

        heading(layout("Lo visto en clase", sectionPaint), spaceAfter = 4f)
        if (module.entries.isEmpty()) {
            block(layout("Sin anotaciones.", emptyPaint), spaceAfter = 8f)
        } else {
            module.entries.forEach { entry ->
                val text = SpannableStringBuilder().apply {
                    append(entry.date, StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append("\n")
                    append(entry.text)
                }
                block(layout(text, bodyPaint), spaceAfter = 7f, minLinesTogether = 2)
            }
        }

        ideasList("Ideas pendientes", module.pendingIdeas)
        ideasList("Ideas utilizadas", module.usedIdeas)
        y += 8f
    }

    private fun ideasList(title: String, ideas: List<String>) {
        if (ideas.isEmpty()) return
        heading(layout(title, sectionPaint), spaceAfter = 4f)
        ideas.forEach { idea -> block(layout("•  $idea", bodyPaint), spaceAfter = 3f) }
        y += 4f
    }

    // --- Maquetación ---

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: CharSequence, paint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .build()

    /** Título: si no cabe junto con un poco de contenido debajo, pasa a la página siguiente. */
    private fun heading(layout: StaticLayout, spaceAfter: Float) {
        if (y + layout.height + 40f > bottomLimit) newPage()
        draw(layout, 0, layout.lineCount)
        y += spaceAfter
    }

    /** Bloque de texto que puede repartirse entre páginas, sin separar sus [minLinesTogether] primeras líneas. */
    private fun block(layout: StaticLayout, spaceAfter: Float, minLinesTogether: Int = 1) {
        var line = 0
        val firstChunk = minOf(minLinesTogether, layout.lineCount)
        if (y + layout.getLineBottom(firstChunk - 1) > bottomLimit) newPage()
        while (line < layout.lineCount) {
            var end = line
            val startTop = layout.getLineTop(line)
            while (end < layout.lineCount && y + (layout.getLineBottom(end) - startTop) <= bottomLimit) end++
            if (end == line) end = line + 1 // una línea más alta que la página: se dibuja igualmente
            draw(layout, line, end)
            line = end
            if (line < layout.lineCount) newPage()
        }
        y += spaceAfter
    }

    private fun draw(layout: StaticLayout, fromLine: Int, toLine: Int) {
        val canvas = page!!.canvas
        val top = layout.getLineTop(fromLine).toFloat()
        val bottom = layout.getLineBottom(toLine - 1).toFloat()
        canvas.save()
        canvas.translate(margin, y - top)
        canvas.clipRect(0f, top, contentWidth.toFloat(), bottom)
        layout.draw(canvas)
        canvas.restore()
        y += bottom - top
    }

    private fun rule() {
        if (y + 60f > bottomLimit) newPage()
        y += 4f
        page!!.canvas.drawLine(margin, y, pageWidth - margin, y, rulePaint)
        y += 10f
    }

    private fun newPage() {
        finishPage()
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = margin
    }

    private fun finishPage() {
        val current = page ?: return
        drawFooter(current.canvas)
        document.finishPage(current)
        page = null
    }

    private fun drawFooter(canvas: Canvas) {
        val text = "Aulix · Registro del curso · Página $pageNumber"
        canvas.drawText(text, margin, pageHeight - margin + 10f, footerPaint)
    }
}
