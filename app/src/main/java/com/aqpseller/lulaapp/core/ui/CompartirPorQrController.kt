package com.aqpseller.lulaapp.core.ui

import com.aqpseller.lulaapp.core.utils.codificarCodigoCompartirQr
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.carecircle.GenerarCodigoCompartirActividadUseCase
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CompartirQrEstado {
    data object Oculto : CompartirQrEstado
    data object Generando : CompartirQrEstado
    data class Mostrando(val qrTexto: String) : CompartirQrEstado
    data class Confirmado(val nombrePersona: String) : CompartirQrEstado
    data object Vencido : CompartirQrEstado
    data object SinCuentaVinculada : CompartirQrEstado
}

/**
 * Coordina generar un código de "Compartir seguimiento" por QR y detectar en vivo cuando alguien
 * lo escanea — cada detalle de Actividad (Hábito/Tarea/Rutina/Medicamento/Cita) inyecta su propia
 * instancia (sin `@Singleton`/`@ViewModelScoped`, Hilt entrega una nueva por sitio de inyección)
 * para no compartir estado entre pantallas. Mismo espíritu que el QR de Familia
 * (`FamiliaViewModel.mostrarCodigoQr`) pero, a diferencia de ese, el código no se auto-renueva —
 * dura 3 minutos y si vence sin usarse hay que tocar "Generar de nuevo", más simple de razonar
 * que un loop en segundo plano. Ver `Plan/08-decisiones-tecnicas.md`.
 */
class CompartirPorQrController @Inject constructor(
    private val generarCodigoCompartirActividadUseCase: GenerarCodigoCompartirActividadUseCase,
    private val compartirSyncRepository: CompartirSyncRepository,
) {
    private val _estado = MutableStateFlow<CompartirQrEstado>(CompartirQrEstado.Oculto)
    val estado: StateFlow<CompartirQrEstado> = _estado.asStateFlow()

    private var job: Job? = null

    fun generar(
        scope: CoroutineScope,
        usuarioId: String,
        actividadId: String,
        tipoActividad: TipoActividad,
        nombreActividad: String,
        permiso: PermisoCompartir,
    ) {
        job?.cancel()
        _estado.value = CompartirQrEstado.Generando
        job = scope.launch {
            val codigo = runCatching {
                generarCodigoCompartirActividadUseCase(usuarioId, actividadId, tipoActividad, nombreActividad, permiso)
            }.getOrNull()
            if (codigo == null) {
                _estado.value = CompartirQrEstado.SinCuentaVinculada
                return@launch
            }
            _estado.value = CompartirQrEstado.Mostrando(codificarCodigoCompartirQr(codigo.codigoId))

            launch {
                delay((codigo.expiraEn - System.currentTimeMillis()).coerceAtLeast(0))
                if (_estado.value is CompartirQrEstado.Mostrando) _estado.value = CompartirQrEstado.Vencido
            }
            compartirSyncRepository.escucharReclamoDeCodigoCompartir(codigo.codigoId).collect { nombre ->
                if (nombre != null) _estado.value = CompartirQrEstado.Confirmado(nombre)
            }
        }
    }

    fun ocultar() {
        job?.cancel()
        job = null
        _estado.value = CompartirQrEstado.Oculto
    }
}
