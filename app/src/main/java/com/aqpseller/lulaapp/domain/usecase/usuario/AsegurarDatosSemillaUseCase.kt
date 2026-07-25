package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.MetodoLogin
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import javax.inject.Inject

private val AREAS_DE_VIDA_PREDEFINIDAS = listOf(
    "Salud", "Finanzas", "Aprendizaje", "Hogar", "Trabajo", "Familia", "Personal/espiritual",
)

/**
 * Idempotente: si ya existe un usuario, no hace nada. Crea el usuario local + su espacio
 * personal + las áreas de vida predefinidas en el primer arranque, sin depender de Firebase
 * (ver `Plan/08-decisiones-tecnicas.md`, sección "usuario semilla").
 */
class AsegurarDatosSemillaUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val espacioRepository: EspacioRepository,
) {
    suspend operator fun invoke() {
        if (usuarioRepository.contarUsuarios() > 0) return

        val usuarioId = IdGenerator.newId()
        usuarioRepository.crearUsuario(
            Usuario(
                id = usuarioId,
                nombreCompleto = "Tú",
                nombrePreferido = "Tú",
                correo = null,
                metodoLogin = MetodoLogin.LOCAL,
                privacidadAceptadaEn = DateTimeUtils.ahoraEpochMillis(),
            ),
        )

        val espacioId = IdGenerator.newId()
        espacioRepository.crearEspacioPersonal(
            espacio = Espacio(
                id = espacioId,
                tipo = TipoEspacio.PERSONAL,
                nombre = "Personal",
                creadoPor = usuarioId,
                fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
            ),
            miembro = EspacioMiembro(espacioId = espacioId, usuarioId = usuarioId, rol = RolEnEspacio.ADMIN),
        )

        if (espacioRepository.contarAreasDeVida() == 0) {
            espacioRepository.sembrarAreasDeVida(
                AREAS_DE_VIDA_PREDEFINIDAS.map { nombre -> AreaDeVida(id = IdGenerator.newId(), nombre = nombre) },
            )
        }
    }
}
