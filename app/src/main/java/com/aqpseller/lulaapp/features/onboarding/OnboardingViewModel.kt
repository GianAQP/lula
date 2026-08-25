package com.aqpseller.lulaapp.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.usuario.ReclamarCuentaConGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasoOnboarding {
    BIENVENIDA, CUENTA, PRIVACIDAD, QUE_MEJORAR, COMO_EMPEZAR, MOMENTO_DEL_DIA, COMO_LLAMARTE, POR_QUE_HOY, RESUMEN
}

data class OnboardingUiState(
    val paso: PasoOnboarding = PasoOnboarding.BIENVENIDA,
    val usuario: Usuario? = null,
    val vinculandoCuenta: Boolean = false,
    val errorCuenta: String? = null,
    val privacidadAceptada: Boolean = false,
    val queMejorar: Set<String> = emptySet(),
    val comoEmpezar: String? = null,
    val momentoDelDia: String? = null,
    val nombrePreferido: String = "",
    val porQueHoy: String? = null,
)

/** Orden lineal de los pasos — ver `Plan/06-onboarding.md`. No incluye "Hábitos sugeridos"
 * (paso 5 del documento), pendiente para una ronda siguiente, ver `Plan/10-pendientes.md`. */
private val ORDEN_PASOS = PasoOnboarding.entries

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val reclamarCuentaConGoogleUseCase: ReclamarCuentaConGoogleUseCase,
    private val compartirSyncRepository: CompartirSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _terminado = MutableStateFlow(false)
    val terminado: StateFlow<Boolean> = _terminado.asStateFlow()

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { usuario ->
                _uiState.value = _uiState.value.copy(
                    usuario = usuario,
                    nombrePreferido = _uiState.value.nombrePreferido.ifBlank { usuario?.nombreCompleto.orEmpty() },
                )
            }
        }
    }

    fun avanzar() {
        val indiceActual = ORDEN_PASOS.indexOf(_uiState.value.paso)
        val siguiente = ORDEN_PASOS.getOrNull(indiceActual + 1) ?: return
        _uiState.value = _uiState.value.copy(paso = siguiente)
    }

    fun retroceder() {
        val indiceActual = ORDEN_PASOS.indexOf(_uiState.value.paso)
        val anterior = ORDEN_PASOS.getOrNull(indiceActual - 1) ?: return
        _uiState.value = _uiState.value.copy(paso = anterior)
    }

    fun reclamarConGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(vinculandoCuenta = true, errorCuenta = null)
            try {
                val yaTeniaRegistroCompleto = reclamarCuentaConGoogleUseCase(idToken)
                _uiState.value = _uiState.value.copy(vinculandoCuenta = false)
                if (yaTeniaRegistroCompleto) {
                    // Esta cuenta ya se registró antes en otro celular — no hace falta volver a
                    // preguntar nada, `completarOnboarding` ya se aplicó vía `aplicarPerfilRemoto`.
                    _terminado.value = true
                } else {
                    avanzar()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    vinculandoCuenta = false,
                    errorCuenta = "No se pudo iniciar sesión con Google. Intenta de nuevo.",
                )
            }
        }
    }

    fun aceptarPrivacidad(aceptada: Boolean) {
        _uiState.value = _uiState.value.copy(privacidadAceptada = aceptada)
    }

    fun continuarDesdePrivacidad() {
        val usuario = _uiState.value.usuario ?: return
        if (!_uiState.value.privacidadAceptada) return
        viewModelScope.launch {
            usuarioRepository.actualizarConsentimientos(usuario.id, terminosAceptadosEn = DateTimeUtils.ahoraEpochMillis())
        }
        avanzar()
    }

    fun alternarQueMejorar(opcion: String) {
        val actual = _uiState.value.queMejorar
        val nuevo = if (opcion in actual) {
            actual - opcion
        } else if (actual.size < 2) {
            actual + opcion
        } else {
            actual
        }
        _uiState.value = _uiState.value.copy(queMejorar = nuevo)
    }

    fun elegirComoEmpezar(valor: String) {
        _uiState.value = _uiState.value.copy(comoEmpezar = valor)
    }

    fun elegirMomentoDelDia(valor: String) {
        _uiState.value = _uiState.value.copy(momentoDelDia = valor)
    }

    fun cambiarNombrePreferido(valor: String) {
        _uiState.value = _uiState.value.copy(nombrePreferido = valor)
    }

    fun elegirPorQueHoy(valor: String) {
        _uiState.value = _uiState.value.copy(porQueHoy = valor)
    }

    fun finalizar() {
        viewModelScope.launch {
            val usuarioId = usuarioRepository.observarUsuario().first()?.id ?: return@launch
            val estado = _uiState.value
            usuarioRepository.guardarRespuestasOnboarding(
                usuarioId = usuarioId,
                queMejorar = estado.queMejorar.toList(),
                comoEmpezar = estado.comoEmpezar,
                momentoDelDiaPreferido = estado.momentoDelDia,
                nombrePreferido = estado.nombrePreferido.ifBlank { null },
                porQueEmpezar = estado.porQueHoy,
            )
            usuarioRepository.completarOnboarding(usuarioId)
            // Recién acá el nombre real ya está guardado — si el perfil se hubiera subido antes
            // (al vincular la cuenta, unos pasos atrás), la nube se habría quedado pegada con el
            // placeholder "Tú". Se sube de nuevo ahora que ya está completo. Ver
            // `Plan/08-decisiones-tecnicas.md`.
            usuarioRepository.observarUsuario().first()?.let { usuario ->
                runCatching { compartirSyncRepository.subirPerfil(usuario) }
            }
            _terminado.value = true
        }
    }
}
