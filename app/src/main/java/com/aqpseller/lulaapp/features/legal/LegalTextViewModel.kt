package com.aqpseller.lulaapp.features.legal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.legal.TextosLegales
import com.aqpseller.lulaapp.domain.legal.TipoDocumentoLegal
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalTextViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioRepository: UsuarioRepository,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
) : ViewModel() {

    val tipo: TipoDocumentoLegal = TipoDocumentoLegal.entries.first {
        it.id == checkNotNull(savedStateHandle.get<String>("tipo"))
    }
    val texto: String = TextosLegales.textoPara(tipo)

    /** Política de privacidad se acepta una sola vez, en la semilla — acá es de solo lectura. */
    val permiteAceptar: Boolean = tipo != TipoDocumentoLegal.PRIVACIDAD

    private val _aceptadoEn = MutableStateFlow<Long?>(null)
    val aceptadoEn: StateFlow<Long?> = _aceptadoEn.asStateFlow()

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { usuario ->
                _aceptadoEn.value = when (tipo) {
                    TipoDocumentoLegal.PRIVACIDAD -> usuario?.privacidadAceptadaEn
                    TipoDocumentoLegal.TERMINOS -> usuario?.terminosAceptadosEn
                    TipoDocumentoLegal.DATOS_SALUD -> usuario?.consentimientoDatosSaludEn
                }
            }
        }
    }

    fun aceptar() {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val ahora = DateTimeUtils.ahoraEpochMillis()
            when (tipo) {
                TipoDocumentoLegal.TERMINOS ->
                    usuarioRepository.actualizarConsentimientos(sesion.usuarioId, terminosAceptadosEn = ahora)
                TipoDocumentoLegal.DATOS_SALUD ->
                    usuarioRepository.actualizarConsentimientos(sesion.usuarioId, consentimientoDatosSaludEn = ahora)
                TipoDocumentoLegal.PRIVACIDAD -> Unit
            }
        }
    }
}
