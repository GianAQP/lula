package com.aqpseller.lulaapp.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ajustesRepository: AjustesRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) : ViewModel() {

    val sonidoCheckHabilitado: StateFlow<Boolean> = ajustesRepository.observarSonidoCheckHabilitado()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val diaRevisionSemanal: StateFlow<Int> = ajustesRepository.observarDiaRevisionSemanal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7)

    val horaRecordatorioCierreDia: StateFlow<String?> = ajustesRepository.observarHoraRecordatorioCierreDia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bottomBarPosicion2: StateFlow<String> = ajustesRepository.observarBottomBarPosicion2()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "asistente")
    val bottomBarPosicion3: StateFlow<String> = ajustesRepository.observarBottomBarPosicion3()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "habitos")
    val bottomBarPosicion4: StateFlow<String> = ajustesRepository.observarBottomBarPosicion4()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "finanzas")

    fun setSonidoCheckHabilitado(habilitado: Boolean) {
        viewModelScope.launch { ajustesRepository.setSonidoCheckHabilitado(habilitado) }
    }

    fun setDiaRevisionSemanal(diaIso: Int) {
        viewModelScope.launch { ajustesRepository.setDiaRevisionSemanal(diaIso) }
    }

    /** `hora = null` apaga el recordatorio — cancela la alarma en vez de dejarla programada sin
     * que nadie la vea en Ajustes. */
    fun setHoraRecordatorioCierreDia(hora: String?) {
        viewModelScope.launch {
            ajustesRepository.setHoraRecordatorioCierreDia(hora)
            if (hora != null) {
                recordatorioScheduler.programarRecordatorioCierreDia(hora)
            } else {
                recordatorioScheduler.cancelarRecordatorioCierreDia()
            }
        }
    }

    fun setBottomBarPosicion2(opcionId: String) {
        viewModelScope.launch { ajustesRepository.setBottomBarPosicion2(opcionId) }
    }

    fun setBottomBarPosicion3(opcionId: String) {
        viewModelScope.launch { ajustesRepository.setBottomBarPosicion3(opcionId) }
    }

    fun setBottomBarPosicion4(opcionId: String) {
        viewModelScope.launch { ajustesRepository.setBottomBarPosicion4(opcionId) }
    }
}
