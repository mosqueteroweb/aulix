package com.seguimiento.clases

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
import com.seguimiento.clases.data.repository.NewCourseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Empezar un curso nuevo borra todo el curso anterior y crea los módulos y el horario por horas. */
@RunWith(AndroidJUnit4::class)
class NewCourseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: ClassRepository
    private var savedPinnedNote: String? = null

    @Before
    fun setUp() {
        savedPinnedNote = pinnedPrefs().getString("key_pinned_note", null)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ClassRepository(database, BackupManager(context, database), context)
    }

    @After
    fun tearDown() {
        database.close()
        pinnedPrefs().edit().putString("key_pinned_note", savedPinnedNote).commit()
    }

    @Test
    fun startNewCourseReplacesEverythingWithTheNewModulesAndHours() = runBlocking {
        // Curso anterior con datos de todo tipo
        database.subjectDao().insertSubjects(listOf(SubjectEntity(id = 1, code = "SI"), SubjectEntity(id = 2, code = "OLD", isArchived = true)))
        database.scheduleDao().insertSessions(listOf(ScheduleSessionEntity(dayOfWeek = 1, subjectId = 1, orderIndex = 0, startHour = 1, endHour = 1)))
        database.classLogDao().insertLogs(listOf(ClassLogEntity(subjectId = 1, date = "2026-05-04", content = "Curso pasado")))
        database.ideaDao().insertIdeas(listOf(IdeaEntity(subjectId = 2, text = "Idea vieja")))
        repository.savePinnedNote("Nota del curso pasado")
        val resetsBefore = repository.dataResets.value

        // Curso nuevo: DI, ASI, ISO, FH. Lunes: FH a 2ª y 3ª, DI a 4ª, ISO a 6ª. Martes: ASI a 1ª-2ª.
        val di = 0; val asi = 1; val iso = 2; val fh = 3
        repository.startNewCourse(
            modules = listOf(
                NewCourseModule("di", "Desarrollo de Interfaces", "#2563EB"),
                NewCourseModule("ASI", "", "#7C3AED"),
                NewCourseModule("ISO", "", "#059669"),
                NewCourseModule("FH", "", "#D97706")
            ),
            hoursByDay = mapOf(
                1 to listOf(null, fh, fh, di, null, iso),
                2 to listOf(asi, asi, null, null, null, null),
                3 to List(6) { null },
                4 to List(6) { null },
                5 to List(6) { null }
            )
        )

        val subjects = database.subjectDao().getAllSubjects().first()
        assertEquals(listOf("DI", "ASI", "ISO", "FH"), subjects.map { it.code })
        val idByCode = subjects.associate { it.code to it.id }

        val monday = database.scheduleDao().getRawSessionsForDay(1)
        assertEquals(
            listOf(Triple(idByCode["FH"], 2, 3), Triple(idByCode["DI"], 4, 4), Triple(idByCode["ISO"], 6, 6)),
            monday.map { Triple(it.subjectId, it.startHour, it.endHour) }
        )
        val tuesday = database.scheduleDao().getRawSessionsForDay(2)
        assertEquals(listOf(Triple(idByCode["ASI"], 1, 2)), tuesday.map { Triple(it.subjectId, it.startHour, it.endHour) })
        assertTrue(database.scheduleDao().getRawSessionsForDay(3).isEmpty())

        assertTrue(database.classLogDao().getAllLogs().isEmpty())
        assertTrue(database.ideaDao().getAllIdeas().isEmpty())
        assertEquals("", repository.getPinnedNote().value)
        assertEquals(resetsBefore + 1, repository.dataResets.value)
    }

    private fun pinnedPrefs() = context.getSharedPreferences("aulix_pinned_notes", Context.MODE_PRIVATE)
}
