package com.aqpseller.lulaapp.domain.usecase.calendario

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.horariosParaFecha
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.core.utils.ocurreEnFecha
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject

/**
 * Agrega TODO lo programado (Hábito/Tarea/Medicamento/Cita/Fecha importante) en un rango de
 * fechas de una sola vez — usado por el Calendario para sus 3 vistas (Día = rango de 1,
 * Semana = rango de 7, Mes = rango de la grilla completa). Trae cada tipo con una sola
 * consulta por tipo (no una consulta por día), y arma el mapa fecha → items en memoria.
 */
class ObtenerAgendaDelRangoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(espacioId: String, desde: LocalDate, hasta: LocalDate): Map<LocalDate, List<ItemAgenda>> {
        val resultado = mutableMapOf<LocalDate, MutableList<ItemAgenda>>()
        fun agregar(fecha: LocalDate, item: ItemAgenda) {
            if (fecha < desde || fecha > hasta) return
            resultado.getOrPut(fecha) { mutableListOf() }.add(item)
        }
        fun rangoDeFechas(): Sequence<LocalDate> = generateSequence(desde) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= hasta }

        // Hábitos: son diarios, aplican todos los días del rango con su estado de ese día.
        val habitos = actividadRepository.observarHabitos(espacioId).first()
        if (habitos.isNotEmpty()) {
            val idsHabito = habitos.map { it.id }
            val estadosPorFechaYActividad = actividadRepository
                .obtenerEstadosDeRango(idsHabito, desde.toEpochDays().toLong(), hasta.toEpochDays().toLong())
                .associate { (it.fecha to it.actividadId) to it.estado }
            rangoDeFechas().forEach { fecha ->
                val epochDay = fecha.toEpochDays().toLong()
                habitos.forEach { habito ->
                    val detalle = habito.detalle as? ActividadDetalle.Habito ?: return@forEach
                    // Antes se mostraba en TODOS los días del rango sin importar cuándo se creó
                    // — un hábito creado hoy aparecía como "pendiente" en días pasados, antes de
                    // existir. Ver `Plan/08-decisiones-tecnicas.md`.
                    if (fecha < DateTimeUtils.epochMillisToLocalDate(habito.fechaCreacion)) return@forEach
                    val estado = estadosPorFechaYActividad[epochDay to habito.id] ?: EstadoActividad.SIN_CONFIRMAR
                    agregar(
                        fecha,
                        ItemAgenda(
                            actividadId = habito.id,
                            tipo = TipoActividad.HABITO,
                            nombre = habito.nombre,
                            horario = detalle.horaRecordatorio,
                            momentoDelDia = detalle.momentoDelDia,
                            estado = estado,
                            subtitulo = null,
                        ),
                    )
                }
            }
        }

        // Tareas: aparecen el día de su fechaLimite mientras están pendientes; una vez
        // completadas, se mueven al día en que de verdad se marcaron (`fechaCompletado`) — así
        // una tarea que debía hacerse ayer pero recién se terminó hoy queda marcada en HOY, no
        // en ayer (antes se quedaba pegada a su fecha límite para siempre, aunque se completara
        // días después). Ver `Plan/08-decisiones-tecnicas.md`.
        actividadRepository.observarTareas(espacioId).first().forEach { tarea ->
            val detalle = tarea.detalle as? ActividadDetalle.Tarea ?: return@forEach
            // Antes se salía acá si no había `fechaLimite`, ANTES de llegar a mirar
            // `fechaCompletado` — una Tarea creada sin fecha (sin nada, solo nombre) que se
            // marcaba hecha hoy nunca aparecía en Calendario, ni siquiera en el día en que de
            // verdad se completó. A pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
            val fechaAMostrar = (if (tarea.estado == EstadoActividad.CONFIRMADO) tarea.fechaCompletado else null)
                ?: detalle.fechaLimite ?: return@forEach
            agregar(
                DateTimeUtils.epochMillisToLocalDate(fechaAMostrar),
                ItemAgenda(
                    actividadId = tarea.id,
                    tipo = TipoActividad.TAREA,
                    nombre = tarea.nombre,
                    horario = detalle.horaRecordatorio,
                    momentoDelDia = null,
                    estado = tarea.estado,
                    subtitulo = null,
                ),
            )
        }

        // Citas puntuales: aparecen solo el día de su fechaHora. Citas de curso: una fila por
        // sesión generada (`SesionCita`), cada una con su propio estado — ver
        // `Plan/08-decisiones-tecnicas.md`.
        val citas = actividadRepository.observarCitas(espacioId).first()
        val citasPuntuales = citas.filterNot { (it.detalle as? ActividadDetalle.Cita)?.esCurso == true }
        citasPuntuales.forEach { cita ->
            val detalle = cita.detalle as? ActividadDetalle.Cita ?: return@forEach
            agregar(
                DateTimeUtils.epochMillisToLocalDate(detalle.fechaHora),
                ItemAgenda(
                    actividadId = cita.id,
                    tipo = TipoActividad.CITA,
                    nombre = cita.nombre,
                    horario = DateTimeUtils.horaHHmm(detalle.fechaHora),
                    momentoDelDia = null,
                    estado = cita.estado,
                    subtitulo = detalle.lugar,
                ),
            )
        }
        val citasDeCurso = citas.filter { (it.detalle as? ActividadDetalle.Cita)?.esCurso == true }
        if (citasDeCurso.isNotEmpty()) {
            val idsCurso = citasDeCurso.map { it.id }
            val sesiones = actividadRepository
                .obtenerSesionesCitaDeRango(idsCurso, desde.toEpochDays().toLong(), hasta.toEpochDays().toLong())
                .groupBy { it.actividadId }
            citasDeCurso.forEach { cita ->
                val detalle = cita.detalle as? ActividadDetalle.Cita ?: return@forEach
                sesiones[cita.id].orEmpty().forEach { sesion ->
                    agregar(
                        DateTimeUtils.epochDaysToLocalDate(sesion.fecha),
                        ItemAgenda(
                            actividadId = cita.id,
                            tipo = TipoActividad.CITA,
                            nombre = "${cita.nombre} (${sesion.numeroSesion}${detalle.cantidadSesionesTotal?.let { "/$it" } ?: ""})",
                            horario = sesion.horario,
                            momentoDelDia = null,
                            estado = sesion.estado,
                            subtitulo = detalle.lugar,
                            sesionNumero = sesion.numeroSesion,
                        ),
                    )
                }
            }
        }

        // Medicamentos: una fila por horario calculado, en los días entre fechaInicio y fechaFin.
        val medicamentos = actividadRepository.observarMedicamentos(espacioId).first()
        if (medicamentos.isNotEmpty()) {
            val idsMedicamento = medicamentos.map { it.id }
            val tomasPorClave = actividadRepository
                .obtenerTomasDeRango(idsMedicamento, desde.toEpochDays().toLong(), hasta.toEpochDays().toLong())
                .associateBy { Triple(it.fecha, it.actividadId, it.horario) }
            rangoDeFechas().forEach { fecha ->
                val epochDay = fecha.toEpochDays().toLong()
                medicamentos.forEach { medicamento ->
                    val detalle = medicamento.detalle as? ActividadDetalle.Medicamento ?: return@forEach
                    val horariosDelDia = horariosParaFecha(detalle, fecha)
                    horariosDelDia.forEachIndexed { index, horario ->
                        val estado = tomasPorClave[Triple(epochDay, medicamento.id, horario)]?.estado ?: EstadoActividad.SIN_CONFIRMAR
                        agregar(
                            fecha,
                            ItemAgenda(
                                actividadId = medicamento.id,
                                tipo = TipoActividad.MEDICAMENTO,
                                nombre = medicamento.nombre,
                                horario = horario,
                                momentoDelDia = null,
                                estado = estado,
                                subtitulo = instruccionParaHorario(detalle, index),
                                tomaHorario = horario,
                            ),
                        )
                    }
                }
            }
        }

        // Fechas importantes: ocurren en el/los día(s) del rango donde cae su recurrencia.
        val fechasImportantes = actividadRepository.observarFechasImportantes(espacioId).first()
        if (fechasImportantes.isNotEmpty()) {
            rangoDeFechas().forEach { fecha ->
                fechasImportantes.forEach { fechaImportante ->
                    val detalle = fechaImportante.detalle as? ActividadDetalle.FechaImportante ?: return@forEach
                    if (ocurreEnFecha(detalle.fechaBase, detalle.recurrencia, fecha)) {
                        agregar(
                            fecha,
                            ItemAgenda(
                                actividadId = fechaImportante.id,
                                tipo = TipoActividad.FECHA_IMPORTANTE,
                                nombre = fechaImportante.nombre,
                                horario = detalle.horaNotificacion,
                                momentoDelDia = null,
                                estado = fechaImportante.estado,
                                subtitulo = null,
                            ),
                        )
                    }
                }
            }
        }

        // Rastro de lo eliminado — a pedido del usuario, para que borrar algo dentro del rango
        // visible no lo haga desaparecer sin dejar huella. Se ve el día en que se eliminó (no su
        // fecha original), es de solo lectura. Ver `Plan/08-decisiones-tecnicas.md`.
        actividadRepository.obtenerEliminadosDeRango(espacioId, desde, hasta).forEach { (fecha, item) -> agregar(fecha, item) }

        return resultado.mapValues { (_, items) -> items.sortedBy { it.horario ?: "99:99" } }
    }
}
