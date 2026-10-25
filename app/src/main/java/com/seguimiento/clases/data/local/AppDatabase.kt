package com.seguimiento.clases.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seguimiento.clases.data.local.dao.ClassLogDao
import com.seguimiento.clases.data.local.dao.IdeaDao
import com.seguimiento.clases.data.local.dao.ScheduleDao
import com.seguimiento.clases.data.local.dao.SubjectDao
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity

@Database(
    entities = [
        SubjectEntity::class,
        ScheduleSessionEntity::class,
        ClassLogEntity::class,
        IdeaEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun classLogDao(): ClassLogDao
    abstract fun ideaDao(): IdeaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE class_logs ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3: el horario pasa a tener horas lectivas (1ª a 6ª). Las sesiones existentes solo tenían
        // orden, así que se asigna una hora por sesión según ese orden (1ª, 2ª, 3ª…).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_sessions ADD COLUMN startHour INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_sessions ADD COLUMN endHour INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE schedule_sessions SET startHour = orderIndex + 1, endHour = orderIndex + 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Una instalación nueva empieza vacía: el curso se configura con el asistente «Nuevo curso»
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seguimiento_clases.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
