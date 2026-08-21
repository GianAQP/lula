package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.core.utils.decodificarListaQr
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

/** Crea una copia local de la Lista codificada en un QR escaneado — transferencia de una sola
 * vez, sin quedar vinculada a la lista original. Devuelve `true` si el QR era válido. */
class ImportarListaDesdeQrUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(qrTexto: String, espacioId: String, usuarioId: String): Boolean {
        val payload = decodificarListaQr(qrTexto) ?: return false
        listaRepository.crear(espacioId, payload.nombre, payload.items.filter { it.isNotBlank() }, usuarioId)
        return true
    }
}
