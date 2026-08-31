package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import javax.inject.Inject

/** Trae los Ajustes guardados en la nube para esta cuenta (si había) y los aplica en este
 * dispositivo — pensado para correr UNA sola vez, al vincular la cuenta (ver
 * `ReclamarCuentaConGoogleUseCase`), no en cada apertura de la app: aplicarlo siempre pisaría
 * un cambio reciente hecho en ESTE mismo celular con una copia vieja de otro. Ver
 * `Plan/08-decisiones-tecnicas.md`. */
class RestaurarAjustesUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val ajustesRepository: AjustesRepository,
) {
    suspend operator fun invoke(firebaseUid: String) {
        val remotos = compartirSyncRepository.restaurarAjustes(firebaseUid) ?: return
        remotos.sonidoCheckHabilitado?.let { ajustesRepository.setSonidoCheckHabilitado(it) }
        remotos.diaRevisionSemanal?.let { ajustesRepository.setDiaRevisionSemanal(it) }
        ajustesRepository.setHoraRecordatorioCierreDia(remotos.horaRecordatorioCierreDia)
        ajustesRepository.setHoraRecordatorioFranja(MomentoDelDia.MANANA, remotos.horaRecordatorioFranjaManana)
        ajustesRepository.setHoraRecordatorioFranja(MomentoDelDia.TARDE, remotos.horaRecordatorioFranjaTarde)
        ajustesRepository.setHoraRecordatorioFranja(MomentoDelDia.NOCHE, remotos.horaRecordatorioFranjaNoche)
        remotos.bottomBarPosicion2?.let { ajustesRepository.setBottomBarPosicion2(it) }
        remotos.bottomBarPosicion3?.let { ajustesRepository.setBottomBarPosicion3(it) }
        remotos.bottomBarPosicion4?.let { ajustesRepository.setBottomBarPosicion4(it) }
        ajustesRepository.setDuracionMaximaAlarmaMin(remotos.duracionMaximaAlarmaMin)
    }
}
