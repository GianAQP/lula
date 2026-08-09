package com.aqpseller.lulaapp.domain.usecase.registrosemanal

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
import javax.inject.Inject

class GuardarRevisionSemanalUseCase @Inject constructor(
    private val registroSemanalRepository: RegistroSemanalRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        usuarioId: String,
        cumplimientoGeneralPorcentaje: Int,
        rachaMaxima: Int,
        queLogre: String?,
        queNoFunciono: String?,
        queAjusto: String?,
    ): RegistroSemanal {
        val semana = DateTimeUtils.claveSemana()
        val existente = registroSemanalRepository.obtenerPorSemana(espacioId, semana)
        val registro = RegistroSemanal(
            id = existente?.id ?: IdGenerator.newId(),
            espacioId = espacioId,
            semana = semana,
            cumplimientoGeneralPorcentaje = cumplimientoGeneralPorcentaje,
            rachaMaxima = rachaMaxima,
            queLogre = queLogre,
            queNoFunciono = queNoFunciono,
            queAjusto = queAjusto,
        )
        registroSemanalRepository.guardarRevision(registro, usuarioId)
        return registro
    }
}
