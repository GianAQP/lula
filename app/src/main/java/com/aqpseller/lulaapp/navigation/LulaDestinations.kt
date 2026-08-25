package com.aqpseller.lulaapp.navigation

object LulaDestinations {
    const val HOY = "hoy"
    const val PROXIMAMENTE = "proximamente/{titulo}"
    const val CREAR_HABITO = "crear_habito?actividadId={actividadId}"
    const val CREAR_TAREA = "crear_tarea?actividadId={actividadId}"
    const val CREAR_MOVIMIENTO = "crear_movimiento/{tipo}?movimientoId={movimientoId}"
    const val CERRAR_DIA = "cerrar_dia?fecha={fecha}"
    const val HABITOS = "habitos"
    const val HABITO_DETALLE = "habito/{actividadId}"
    const val TAREAS = "tareas"
    const val TAREA_DETALLE = "tarea/{actividadId}"
    const val FINANZAS = "finanzas"
    const val FINANZAS_HISTORIAL = "finanzas_historial"
    const val ZONA_PRIVADA_GATE = "zona_privada_gate"
    const val ZONA_PRIVADA_GATE_DIARIO = "zona_privada_gate_diario"
    const val ZONA_PRIVADA_GATE_NOTAS = "zona_privada_gate_notas"
    const val HISTORIAL = "historial"
    const val METAS = "metas"
    const val META_DETALLE = "meta/{metaId}"
    const val CREAR_META = "crear_meta?metaId={metaId}"
    const val RUTINAS = "rutinas"
    const val RUTINA_DETALLE = "rutina/{actividadId}"
    const val CREAR_RUTINA = "crear_rutina?actividadId={actividadId}"
    const val REVISION_SEMANAL = "revision_semanal"
    const val REVISION_SEMANAL_HISTORIAL = "revision_semanal_historial"
    const val RECORDATORIO_ACCION = "recordatorio/{actividadId}"
    const val AJUSTES = "ajustes"
    const val PERFIL = "perfil"
    const val LISTAS = "listas"
    const val LISTA_DETALLE = "lista/{listaId}"
    const val LISTA_HISTORIAL = "lista_historial/{listaId}"
    const val CREAR_LISTA = "crear_lista"
    const val SALUD = "salud"
    const val CREAR_MEDICAMENTO = "crear_medicamento?actividadId={actividadId}"
    const val MEDICAMENTO_DETALLE = "medicamento/{actividadId}"
    const val CREAR_CITA = "crear_cita?actividadId={actividadId}"
    const val CITA_DETALLE = "cita/{actividadId}"
    const val ACCION_TOMA = "toma_accion/{actividadId}/{horario}"
    const val CIRCULO_CUIDADO = "circulo_cuidado"
    const val LO_QUE_ME_COMPARTEN = "lo_que_me_comparten"
    const val FECHAS_IMPORTANTES = "fechas_importantes"
    const val CREAR_FECHA_IMPORTANTE = "crear_fecha_importante?actividadId={actividadId}"
    const val NOTAS = "notas"
    const val NOTA_EDITOR = "nota?notaId={notaId}"
    const val CALENDARIO = "calendario"
    const val DIARIO = "diario"
    const val DIARIO_ENTRADA = "diario_entrada?entradaId={entradaId}&fecha={fecha}"
    const val DIARIO_CALENDARIO = "diario_calendario"
    const val PROGRESO = "progreso"
    const val FAMILIA = "familia"
    const val RETOS_FAMILIARES = "retos_familiares"
    const val CREAR_RETO_FAMILIAR = "crear_reto_familiar"
    const val PROPOSITO_PERSONAL = "proposito_personal"
    const val EDITAR_RESPUESTA_PROPOSITO = "editar_respuesta_proposito/{preguntaId}"
    const val TEXTO_LEGAL = "texto_legal/{tipo}"

    fun proximamente(titulo: String) = "proximamente/$titulo"
    fun crearMovimiento(tipo: String, movimientoId: String? = null) =
        "crear_movimiento/$tipo" + (movimientoId?.let { "?movimientoId=$it" } ?: "")
    fun crearHabito(actividadId: String? = null) = "crear_habito" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun crearTarea(actividadId: String? = null) = "crear_tarea" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun habitoDetalle(actividadId: String) = "habito/$actividadId"
    fun tareaDetalle(actividadId: String) = "tarea/$actividadId"
    fun metaDetalle(metaId: String) = "meta/$metaId"
    fun crearMeta(metaId: String? = null) = "crear_meta" + (metaId?.let { "?metaId=$it" } ?: "")
    fun rutinaDetalle(actividadId: String) = "rutina/$actividadId"
    fun crearRutina(actividadId: String? = null) = "crear_rutina" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun recordatorioAccion(actividadId: String) = "recordatorio/$actividadId"
    fun listaDetalle(listaId: String) = "lista/$listaId"
    fun listaHistorial(listaId: String) = "lista_historial/$listaId"
    fun crearMedicamento(actividadId: String? = null) = "crear_medicamento" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun medicamentoDetalle(actividadId: String) = "medicamento/$actividadId"
    fun crearCita(actividadId: String? = null) = "crear_cita" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun citaDetalle(actividadId: String) = "cita/$actividadId"
    fun accionToma(actividadId: String, horario: String) = "toma_accion/$actividadId/$horario"
    fun crearFechaImportante(actividadId: String? = null) = "crear_fecha_importante" + (actividadId?.let { "?actividadId=$it" } ?: "")
    fun notaEditor(notaId: String? = null) = "nota" + (notaId?.let { "?notaId=$it" } ?: "")
    fun editarRespuestaProposito(preguntaId: String) = "editar_respuesta_proposito/$preguntaId"
    fun textoLegal(tipo: String) = "texto_legal/$tipo"
    fun diarioEntrada(entradaId: String? = null, fecha: Long? = null): String {
        val params = listOfNotNull(entradaId?.let { "entradaId=$it" }, fecha?.let { "fecha=$it" })
        return "diario_entrada" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
    }

    /** [fechaEpochDay] null = hoy (comportamiento de siempre). Un epoch day explícito permite
     * llenar o actualizar el cierre de un día anterior desde Calendario. */
    fun cerrarDia(fechaEpochDay: Long? = null): String =
        "cerrar_dia" + (fechaEpochDay?.let { "?fecha=$it" } ?: "")
}
