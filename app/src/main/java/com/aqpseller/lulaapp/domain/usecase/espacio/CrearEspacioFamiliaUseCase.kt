package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import javax.inject.Inject

/**
 * Crea el espacio Familia con el usuario actual como único admin (base local de Fase 1.5, ver
 * `Plan/08-decisiones-tecnicas.md`) y lo deja como espacio activo de una — no tendría sentido
 * crearlo y seguir viendo Personal.
 */
class CrearEspacioFamiliaUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val ajustesRepository: AjustesRepository,
) {
    suspend operator fun invoke(nombre: String, usuarioId: String): Espacio {
        val espacio = Espacio(
            id = IdGenerator.newId(),
            tipo = TipoEspacio.FAMILIA,
            nombre = nombre,
            creadoPor = usuarioId,
            fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
        )
        espacioRepository.crearEspacio(
            espacio = espacio,
            miembro = EspacioMiembro(espacioId = espacio.id, usuarioId = usuarioId, rol = RolEnEspacio.ADMIN),
        )
        ajustesRepository.setEspacioActivoId(espacio.id)
        return espacio
    }
}
