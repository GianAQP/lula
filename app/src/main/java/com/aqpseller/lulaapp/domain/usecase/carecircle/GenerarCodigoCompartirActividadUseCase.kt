package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.CodigoCompartirActividad
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Genera un código de "Compartir seguimiento" de corta duración para mostrarse como QR — quien
 * lo escanea queda acompañando de inmediato, sin escribir nombre/correo/teléfono a mano ni
 * esperar que acepte. Necesita cuenta vinculada con Google (mismo requisito que compartir por
 * correo); lanza si no la tiene, quien llama debe mostrar el mismo aviso que ya existe para
 * Familia ("vincula tu cuenta primero"). Ver `Plan/08-decisiones-tecnicas.md`. */
class GenerarCodigoCompartirActividadUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(
        usuarioId: String,
        actividadId: String,
        tipoActividad: TipoActividad,
        nombreActividad: String,
        permiso: PermisoCompartir,
    ): CodigoCompartirActividad {
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Alguien"
        return compartirSyncRepository.generarCodigoCompartir(actividadId, tipoActividad, nombreActividad, permiso, usuarioId, miNombre)
    }
}
