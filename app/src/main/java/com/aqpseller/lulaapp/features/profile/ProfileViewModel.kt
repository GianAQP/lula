package com.aqpseller.lulaapp.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.usuario.ActualizarHorariosComidaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val actualizarHorariosComidaUseCase: ActualizarHorariosComidaUseCase,
) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { _usuario.value = it }
        }
    }

    fun actualizarHorarioDesayuno(hora: String) = actualizarHorarios(desayuno = hora)
    fun actualizarHorarioAlmuerzo(hora: String) = actualizarHorarios(almuerzo = hora)
    fun actualizarHorarioCena(hora: String) = actualizarHorarios(cena = hora)

    private fun actualizarHorarios(
        desayuno: String? = _usuario.value?.horaDesayuno,
        almuerzo: String? = _usuario.value?.horaAlmuerzo,
        cena: String? = _usuario.value?.horaCena,
    ) {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            actualizarHorariosComidaUseCase(sesion.usuarioId, desayuno, almuerzo, cena)
        }
    }

    fun confirmarMayorDe13() {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            usuarioRepository.actualizarConsentimientos(sesion.usuarioId, confirmoMayorDe13 = true)
        }
    }

    fun eliminarCuenta(despuesDeEliminar: () -> Unit) {
        viewModelScope.launch {
            usuarioRepository.eliminarCuenta()
            despuesDeEliminar()
        }
    }
}
