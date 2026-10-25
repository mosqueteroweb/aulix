package com.seguimiento.clases.data.repository

/** Módulo dado de alta en el asistente «Nuevo curso», antes de guardarse en la base de datos. */
data class NewCourseModule(
    val code: String,
    val name: String,
    val colorHex: String
)
