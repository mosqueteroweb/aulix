package com.seguimiento.clases.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.seguimiento.clases.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copia de seguridad automática en Descargas/Aulix. Se conservan solo las [KEEP_COPIES] copias
 * automáticas más recientes: al crear una nueva se borran las anteriores. Nunca toca las copias
 * exportadas a mano (no empiezan por [FILE_PREFIX]).
 * Usa MediaStore.Downloads, así que necesita Android 10 (API 29) o posterior y ningún permiso.
 */
class AutoBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val backupManager: BackupManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Momento de la última copia automática correcta (0 si aún no hay ninguna). */
    val lastBackupMillis: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)

    fun lastBackupFlow(): Flow<Long> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LAST_BACKUP) trySend(lastBackupMillis)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(lastBackupMillis)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Crea una copia automática y elimina las antiguas. Devuelve el nombre del archivo creado,
     * o null si no había datos que copiar (curso sin configurar).
     */
    suspend fun createBackup(nowMillis: Long = System.currentTimeMillis()): Result<String?> = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext Result.failure(UnsupportedOperationException("Necesita Android 10 o posterior"))
        runCatching {
            val hasData = database.subjectDao().getAllSubjects().first().isNotEmpty() ||
                database.classLogDao().getAllLogs().isNotEmpty()
            if (!hasData) return@runCatching null

            val fileName = fileNameFor(nowMillis)
            writeToDownloads(fileName, backupManager.createBackupJson())
            deleteOldCopies()
            prefs.edit().putLong(KEY_LAST_BACKUP, nowMillis).apply()
            fileName
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToDownloads(fileName: String, content: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas")
        try {
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: error("No se pudo escribir la copia")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /** Copias automáticas que hay ahora en Descargas/Aulix (solo las creadas por esta app). */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun listCopies(): List<Pair<Long, String>> {
        val copies = mutableListOf<Pair<Long, String>>()
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf(RELATIVE_PATH, "$FILE_PREFIX%"),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) copies += cursor.getLong(0) to cursor.getString(1)
        }
        return copies
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteOldCopies() {
        val copies = listCopies()
        val namesToDelete = copiesToDelete(copies.map { it.second }, KEEP_COPIES).toSet()
        copies.filter { it.second in namesToDelete }.forEach { (id, _) ->
            context.contentResolver.delete(
                android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                null,
                null
            )
        }
    }

    companion object {
        const val KEEP_COPIES = 3
        const val FILE_PREFIX = "aulix_copia_auto_"
        const val RELATIVE_PATH = "Download/Aulix/"
        private const val PREFS_NAME = "aulix_auto_backup"
        private const val KEY_LAST_BACKUP = "key_last_auto_backup_millis"

        /** Nombre con fecha y hora que se ordena alfabéticamente igual que en el tiempo. */
        fun fileNameFor(millis: Long): String =
            FILE_PREFIX + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(millis)) + ".json"

        /** De una lista de copias automáticas, las que sobran para quedarse con las [keep] más recientes. */
        fun copiesToDelete(fileNames: List<String>, keep: Int): List<String> =
            fileNames.filter { it.startsWith(FILE_PREFIX) }.sortedDescending().drop(keep)
    }
}
