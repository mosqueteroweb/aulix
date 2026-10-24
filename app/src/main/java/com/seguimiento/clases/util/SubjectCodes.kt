package com.seguimiento.clases.util

import com.seguimiento.clases.data.local.entity.SubjectEntity

object SubjectCodes {
    /** Forma en que se guarda una sigla: sin espacios alrededor y en mayúsculas. */
    fun normalize(code: String): String = code.trim().uppercase()

    /**
     * Indica si la sigla ya la usa otro módulo (activo o archivado), sin distinguir mayúsculas.
     * [editingSubjectId] excluye al propio módulo cuando se está editando.
     */
    fun isTaken(code: String, subjects: List<SubjectEntity>, editingSubjectId: Long? = null): Boolean {
        val normalized = normalize(code)
        if (normalized.isEmpty()) return false
        return subjects.any { it.id != editingSubjectId && normalize(it.code) == normalized }
    }

    const val DUPLICATE_MESSAGE = "Ya hay un módulo con esas siglas"
}
