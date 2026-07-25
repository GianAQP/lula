package com.aqpseller.lulaapp.domain.model

/**
 * Tabla de auditoría escrita desde el MVP (lección aprendida de Mayia: quedó diseñada y nunca
 * construida). Cada método de escritura de los repositorios inserta una fila aquí — nunca un
 * caso de uso aparte, para que sea imposible de omitir al agregar un flujo nuevo.
 */
data class HistorialCambios(
    val id: String,
    val entidad: String,
    val entidadId: String,
    val accion: AccionAuditoria,
    val valoresAntesJson: String?,
    val valoresDespuesJson: String?,
    val usuarioId: String,
    val timestamp: Long,
    val origen: OrigenCambio,
)
