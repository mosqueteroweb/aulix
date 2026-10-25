package com.seguimiento.clases.data.export

import android.content.Context
import android.net.Uri
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Genera el registro del curso en PDF a partir de la base de datos. */
class CourseReportExporter(
    private val context: Context,
    private val database: AppDatabase
) {
    suspend fun buildReport(pinnedNote: String?): CourseReport = withContext(Dispatchers.IO) {
        CourseReportBuilder.build(
            subjects = database.subjectDao().getAllSubjects().first(),
            sessions = database.scheduleDao().getAllRawSessions(),
            logs = database.classLogDao().getAllLogs(),
            ideas = database.ideaDao().getAllIdeas(),
            pinnedNote = pinnedNote,
            generatedOn = DateUtils.formatShortDate(LocalDate.now().toString())
        )
    }

    /** Escribe el PDF en [targetUri] y devuelve el número de páginas. */
    suspend fun export(targetUri: Uri, pinnedNote: String?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val report = buildReport(pinnedNote)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                PdfReportWriter().write(report, output)
            } ?: error("No se pudo abrir el archivo para escritura")
        }
    }
}
