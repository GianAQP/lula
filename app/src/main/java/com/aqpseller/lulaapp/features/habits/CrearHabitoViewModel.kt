package com.aqpseller.lulaapp.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.usecase.actividad.CrearHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearHabitoViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val crearHabitoUseCase: CrearHabitoUseCase,
) : ViewModel() {

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    fun guardar(nombre: String, momentoDelDia: MomentoDelDia, duracionInicialMin: Int?) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            crearHabitoUseCase(
                espacioId = sesion.espacioId,
                propietario = sesion.usuarioId,
                nombre = nombre,
                momentoDelDia = momentoDelDia,
                frecuencia = FrecuenciaHabito.DIARIA,
                duracionInicialMin = duracionInicialMin,
            )
            _guardado.value = true
        }
    }
}
