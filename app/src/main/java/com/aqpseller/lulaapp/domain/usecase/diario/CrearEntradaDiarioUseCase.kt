package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class CrearEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
        titulo: String?,
        texto: String,
        areaDeVidaId: String?,
        fecha: Long,
    ) {
        val entrada = EntradaDiario(
            id = IdGenerator.newId(),
            espacioId = espacioId,
            propietario = propietario,
            titulo = titulo?.takeIf { it.isNotBlank() },
            texto = texto,
            areaDeVidaId = areaDeVidaId,
            fecha = fecha,
            // Toda entrada de Diario vive dentro de Zona Privada — nace siempre solo_yo,
            // no es un campo que el usuario elija en el formulario (ver 02-pantallas.md).
            privacidad = Privacidad.SOLO_YO,
        )
        entradaDiarioRepository.crear(entrada)
        runCatching { personalSyncRepository.subirEntradaDiario(entrada) }
    }
}
