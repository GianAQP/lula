package com.aqpseller.lulaapp.features.health

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.Comida
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ActualizarMedicamentoUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.CrearMedicamentoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ActualizarHorariosComidaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicamentoFormInicial(
    val nombreMedicamento: String,
    val dosis: String,
    val modoFrecuencia: ModoFrecuenciaMedicamento,
    val intervaloHoras: Int?,
    val horaPrimeraDosis: String?,
    val comidasRelacionadas: List<ComidaRelacionada>,
    val fechaFin: Long?,
    val cantidadDosisTotal: Int?,
    val nivelRecordatorio: NivelRecordatorio,
    val recordatorioPersistente: Boolean,
    val intervaloPersistenciaMin: Int?,
)

data class HorariosComida(
    val desayuno: String? = null,
    val almuerzo: String? = null,
    val cena: String? = null,
)

@HiltViewModel
class CrearMedicamentoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val crearMedicamentoUseCase: CrearMedicamentoUseCase,
    private val actualizarMedicamentoUseCase: ActualizarMedicamentoUseCase,
    private val actualizarHorariosComidaUseCase: ActualizarHorariosComidaUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<MedicamentoFormInicial?>(null)
    val formInicial: StateFlow<MedicamentoFormInicial?> = _formInicial.asStateFlow()

    private val _horariosComida = MutableStateFlow(HorariosComida())
    val horariosComida: StateFlow<HorariosComida> = _horariosComida.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    private var fechaInicioExistente: Long? = null

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { usuario ->
                _horariosComida.value = HorariosComida(usuario?.horaDesayuno, usuario?.horaAlmuerzo, usuario?.horaCena)
            }
        }
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.Medicamento
                if (actividad != null && detalle != null) {
                    fechaInicioExistente = detalle.fechaInicio
                    _formInicial.value = MedicamentoFormInicial(
                        nombreMedicamento = detalle.nombreMedicamento,
                        dosis = detalle.dosis,
                        modoFrecuencia = detalle.modoFrecuencia,
                        intervaloHoras = detalle.intervaloHoras,
                        horaPrimeraDosis = detalle.horaPrimeraDosis,
                        comidasRelacionadas = detalle.comidasRelacionadas,
                        fechaFin = detalle.fechaFin,
                        cantidadDosisTotal = detalle.cantidadDosisTotal,
                        nivelRecordatorio = detalle.nivelRecordatorio,
                        recordatorioPersistente = detalle.recordatorioPersistente,
                        intervaloPersistenciaMin = detalle.intervaloPersistenciaMin,
                    )
                }
            }
        }
    }

    /** Guardado inmediato al elegir la hora — así queda disponible para el resto de medicamentos futuros, no solo este. */
    fun actualizarHorarioComida(comida: Comida, hora: String) {
        val actual = _horariosComida.value
        val nuevo = when (comida) {
            Comida.DESAYUNO -> actual.copy(desayuno = hora)
            Comida.ALMUERZO -> actual.copy(almuerzo = hora)
            Comida.CENA -> actual.copy(cena = hora)
        }
        _horariosComida.value = nuevo
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            actualizarHorariosComidaUseCase(sesion.usuarioId, nuevo.desayuno, nuevo.almuerzo, nuevo.cena)
        }
    }

    fun guardar(
        nombreMedicamento: String,
        dosis: String,
        modoFrecuencia: ModoFrecuenciaMedicamento,
        intervaloHoras: Int?,
        horaPrimeraDosis: String?,
        comidasRelacionadas: List<ComidaRelacionada>,
        fechaFin: Long?,
        cantidadDosisTotal: Int?,
        nivelRecordatorio: NivelRecordatorio,
        recordatorioPersistente: Boolean,
        intervaloPersistenciaMin: Int?,
    ) {
        // Antes estos 3 casos simplemente no hacían nada al tocar "Crear" — sin ningún aviso,
        // se veía como si el botón estuviera roto. Ahora avisan qué falta.
        if (nombreMedicamento.isBlank() || dosis.isBlank()) {
            _mensajeError.value = "Escribe el nombre y la dosis del medicamento"
            return
        }
        if (modoFrecuencia == ModoFrecuenciaMedicamento.INTERVALO_HORAS && (horaPrimeraDosis == null || intervaloHoras == null)) {
            _mensajeError.value = "Elige la hora de la primera dosis y cada cuántas horas"
            return
        }
        if (modoFrecuencia == ModoFrecuenciaMedicamento.RELACION_COMIDA && comidasRelacionadas.isEmpty()) {
            _mensajeError.value = "Elige con qué comida se relaciona la toma"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val horarios = _horariosComida.value
            val id = actividadId
            if (id != null) {
                actualizarMedicamentoUseCase(
                    actividadId = id,
                    nombreMedicamento = nombreMedicamento,
                    dosis = dosis,
                    modoFrecuencia = modoFrecuencia,
                    intervaloHoras = intervaloHoras,
                    horaPrimeraDosis = horaPrimeraDosis,
                    comidasRelacionadas = comidasRelacionadas,
                    horaDesayuno = horarios.desayuno,
                    horaAlmuerzo = horarios.almuerzo,
                    horaCena = horarios.cena,
                    fechaInicio = fechaInicioExistente ?: DateTimeUtils.inicioDeHoyEpochMillis(),
                    fechaFin = fechaFin,
                    cantidadDosisTotal = cantidadDosisTotal,
                    nivelRecordatorio = nivelRecordatorio,
                    recordatorioPersistente = recordatorioPersistente,
                    intervaloPersistenciaMin = intervaloPersistenciaMin,
                    usuarioId = sesion.usuarioId,
                )
            } else {
                crearMedicamentoUseCase(
                    espacioId = sesion.espacioId,
                    propietario = sesion.usuarioId,
                    nombreMedicamento = nombreMedicamento,
                    dosis = dosis,
                    modoFrecuencia = modoFrecuencia,
                    intervaloHoras = intervaloHoras,
                    horaPrimeraDosis = horaPrimeraDosis,
                    comidasRelacionadas = comidasRelacionadas,
                    horaDesayuno = horarios.desayuno,
                    horaAlmuerzo = horarios.almuerzo,
                    horaCena = horarios.cena,
                    fechaInicio = DateTimeUtils.inicioDeHoyEpochMillis(),
                    fechaFin = fechaFin,
                    cantidadDosisTotal = cantidadDosisTotal,
                    nivelRecordatorio = nivelRecordatorio,
                    recordatorioPersistente = recordatorioPersistente,
                    intervaloPersistenciaMin = intervaloPersistenciaMin,
                )
            }
            _guardado.value = true
        }
    }

    fun errorMostrado() {
        _mensajeError.value = null
    }
}
