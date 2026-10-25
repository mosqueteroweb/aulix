package com.seguimiento.clases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seguimiento.clases.data.export.CourseReport
import com.seguimiento.clases.data.export.LogEntry
import com.seguimiento.clases.data.export.ModuleReport
import com.seguimiento.clases.data.export.PdfReportWriter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** El registro se genera como un PDF válido, legible y paginado. */
@RunWith(AndroidJUnit4::class)
class PdfReportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val file = File(context.cacheDir, "registro_test.pdf")

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun shortReportFitsInOnePageWithVisibleText() {
        val report = CourseReport(
            generatedOn = "25/10/26",
            period = "Del 13/10/26 al 20/10/26",
            modules = listOf(
                ModuleReport("DI · Desarrollo de Interfaces", false, "Lunes 4ª h", listOf(LogEntry("13/10/26", "Tema 1: layouts")), listOf("Idea"), emptyList())
            ),
            pinnedNote = "Nota fija"
        )
        val pages = file.outputStream().use { PdfReportWriter().write(report, it) }

        assertEquals(1, pages)
        assertEquals(1, pageCount())
        assertTrue("La primera página no debe estar en blanco", firstPageHasInk())
    }

    @Test
    fun longReportSpreadsOverSeveralPages() {
        val longText = "Explicación del tema con ejercicios y dudas resueltas en clase. ".repeat(12)
        val modules = (1..4).map { m ->
            ModuleReport(
                title = "M$m · Módulo $m",
                isArchived = m == 4,
                schedule = "Lunes ${m}ª h",
                entries = (1..40).map { d -> LogEntry("%02d/10/26".format((d % 28) + 1), "Sesión $d. $longText") },
                pendingIdeas = (1..5).map { "Idea pendiente $it" },
                usedIdeas = listOf("Idea usada")
            )
        }
        val pages = file.outputStream().use { PdfReportWriter().write(CourseReport("25/10/26", null, modules, null), it) }

        assertTrue("Debe ocupar varias páginas (ocupa $pages)", pages > 10)
        assertEquals(pages, pageCount())
    }

    private fun pageCount(): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }

    private fun firstPageHasInk(): Boolean =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    var darkPixels = 0
                    for (x in 0 until bitmap.width step 3) for (y in 0 until bitmap.height step 3) {
                        if (Color.red(bitmap.getPixel(x, y)) < 128) darkPixels++
                    }
                    darkPixels > 200
                }
            }
        }
}
