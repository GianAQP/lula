package com.aqpseller.lulaapp.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.usecase.actividad.CrearTareaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearTareaViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val crearTareaUseCase: CrearTareaUseCase,
) : ViewModel() {

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    fun guardar(nombre: String, fechaLimite: Long?, importante: Boolean, urgente: Boolean) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            crearTareaUseCase(
                espacioId = sesion.espacioId,
                propietario = sesion.usuarioId,
                nombre = nombre,
                fechaLimite = fechaLimite,
                importante = importante,
                urgente = urgente,
            )
            _guardado.value = true
        }
    }
}
