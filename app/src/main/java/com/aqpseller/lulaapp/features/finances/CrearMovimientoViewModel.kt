package com.aqpseller.lulaapp.features.finances

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.finanzas.ActualizarMovimientoUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.EliminarMovimientoUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerMovimientoUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.RegistrarMovimientoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovimientoFormInicial(
    val tipo: TipoMovimientoFinanciero,
    val monto: Double,
    val categoria: String,
    val descripcion: String?,
    val fecha: Long,
)

@HiltViewModel
class CrearMovimientoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerMovimientoUseCase: ObtenerMovimientoUseCase,
    private val registrarMovimientoUseCase: RegistrarMovimientoUseCase,
    private val actualizarMovimientoUseCase: ActualizarMovimientoUseCase,
    private val eliminarMovimientoUseCase: EliminarMovimientoUseCase,
) : ViewModel() {

    private val movimientoId: String? = savedStateHandle.get<String>("movimientoId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = movimientoId != null

    val tipoInicial: TipoMovimientoFinanciero = savedStateHandle.get<String>("tipo")
        ?.let { runCatching { TipoMovimientoFinanciero.valueOf(it) }.getOrNull() }
        ?: TipoMovimientoFinanciero.EGRESO

    private val _formInicial = MutableStateFlow<MovimientoFormInicial?>(null)
    val formInicial: StateFlow<MovimientoFormInicial?> = _formInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _eliminado = MutableStateFlow(false)
    val eliminado: StateFlow<Boolean> = _eliminado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    fun errorMostrado() {
        _mensajeError.value = null
    }

    init {
        val id = movimientoId
        if (id != null) {
            viewModelScope.launch {
                obtenerMovimientoUseCase(id)?.let {
                    _formInicial.value = MovimientoFormInicial(it.tipo, it.monto, it.categoria, it.descripcion, it.fecha)
                }
            }
        }
    }

    fun guardar(tipo: TipoMovimientoFinanciero, monto: Double, categoria: String, descripcion: String?, fecha: Long) {
        if (categoria.isBlank()) {
            _mensajeError.value = "Elige o escribe una categoría"
            return
        }
        if (monto <= 0.0) {
            _mensajeError.value = "Escribe un monto mayor a 0"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val id = movimientoId
            if (id != null) {
                actualizarMovimientoUseCase(id, sesion.usuarioId, tipo, monto, categoria, descripcion, fecha)
            } else {
                registrarMovimientoUseCase(
                    espacioId = sesion.espacioId,
                    usuarioId = sesion.usuarioId,
                    tipo = tipo,
                    monto = monto,
                    categoria = categoria,
                    descripcion = descripcion,
                    fecha = fecha,
                )
            }
            _guardado.value = true
        }
    }

    fun eliminar() {
        val id = movimientoId ?: return
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            eliminarMovimientoUseCase(id, sesion.usuarioId)
            _eliminado.value = true
        }
    }
}
