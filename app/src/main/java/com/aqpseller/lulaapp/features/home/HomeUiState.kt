package com.aqpseller.lulaapp.features.home

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.MedicamentoDeHoy
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso

data class ActividadUi(
    val id: String,
    val nombre: String,
    val estado: EstadoActividad,
    val tipo: TipoActividad,
    /** Formato "HH:mm", null si no tiene hora de recordatorio — usado para pintar en rojo si ya
     * pasó la hora y sigue `SIN_CONFIRMAR` (mismo criterio que Cita/Fecha importante/Medicamento). */
    val horaRecordatorio: String? = null,
)

/** Tarjeta "¿Aumentamos?" de un Hábito progresivo — ver `Plan/02-pantallas.md`. */
data class RevisionHabitoUi(
    val actividadId: String,
    val nombre: String,
    val duracionActualMin: Int,
    val incrementoMin: Int,
    val duracionObjetivoMin: Int,
    val frecuenciaRevisionDias: Int,
    val diasCumplidos: Int,
    val diasTotales: Int,
)

data class HomeUiState(
    val cargando: Boolean = true,
    /** Pendientes (hábitos/tareas sin confirmar) en el espacio Familia del usuario, si tiene uno
     * y no es el espacio activo ahora mismo — para el aviso "tienes pendientes en Familia". */
    val pendientesEnFamilia: Int = 0,
    val racha: Int = 0,
    val diaYaCerrado: Boolean = false,
    val actividadesManana: List<ActividadUi> = emptyList(),
    val actividadesTarde: List<ActividadUi> = emptyList(),
    val actividadesNoche: List<ActividadUi> = emptyList(),
    val tareasDeHoy: List<ActividadUi> = emptyList(),
    val gastosHoyTotal: Double = 0.0,
    val hayMetas: Boolean = false,
    val metasActivas: List<MetaConProgreso> = emptyList(),
    val hayRutinas: Boolean = false,
    val hayListas: Boolean = false,
    val sonidoCheckHabilitado: Boolean = true,
    val revisionesHabitoPendientes: List<RevisionHabitoUi> = emptyList(),
    val medicamentosDeHoy: List<MedicamentoDeHoy> = emptyList(),
    val hayMedicamentosOCitas: Boolean = false,
    val hayFechasImportantes: Boolean = false,
    val hayNotas: Boolean = false,
    val citasDeHoy: List<ItemAgenda> = emptyList(),
    val fechasImportantesDeHoy: List<ItemAgenda> = emptyList(),
) {
    val totalActividades: Int
        get() = actividadesManana.size + actividadesTarde.size + actividadesNoche.size + tareasDeHoy.size + citasDeHoy.size

    val completadas: Int
        get() = (actividadesManana + actividadesTarde + actividadesNoche + tareasDeHoy)
            .count { it.estado == EstadoActividad.CONFIRMADO } +
            citasDeHoy.count { it.estado == EstadoActividad.CONFIRMADO }

    val hayAlgoParaHoy: Boolean
        get() = totalActividades > 0 || medicamentosDeHoy.isNotEmpty() || citasDeHoy.isNotEmpty() ||
            fechasImportantesDeHoy.isNotEmpty() || metasActivas.isNotEmpty()

    /** Metas que cruzaron un hito (25/50/75/100%) que todavía no se les mostró — ver
     * `MetaConProgreso.hayHitoNuevo`. */
    val hitosMetaPendientes: List<MetaConProgreso>
        get() = metasActivas.filter { it.hayHitoNuevo }

    /** Sin las ya completadas — una vez al 100%, seguir mostrándola en Hoy todos los días no
     * aporta nada (la celebración de hito ya avisó una vez, ver `hitosMetaPendientes`). Con
     * muchas metas, además, esto evita que las ya logradas ensucien la lista de las que siguen
     * en curso. */
    val metasEnProgreso: List<MetaConProgreso>
        get() = metasActivas.filter { it.fraccion < 1f }
}
