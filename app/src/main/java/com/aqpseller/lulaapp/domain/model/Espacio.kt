package com.aqpseller.lulaapp.domain.model

data class Espacio(
    val id: String,
    val tipo: TipoEspacio,
    val nombre: String,
    val creadoPor: String,
    val fechaCreacion: Long,
)

data class EspacioMiembro(
    val espacioId: String,
    val usuarioId: String,
    val rol: RolEnEspacio,
    /** Nombre para mostrar — denormalizado porque `usuarioId` es el id LOCAL de cada quien (una
     * UUID distinta por dispositivo, sin significado fuera de su propio celular); sin esto, la
     * lista de miembros mostraba el id en vez del nombre para cualquiera que no fuera "yo". Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    val nombre: String? = null,
    /** uid de Firebase de este miembro — necesario para que un admin pueda quitarlo de verdad
     * (el documento de membresía en Firestore se guarda por uid de Firebase, no por `usuarioId`
     * local). Se captura al recibir su membresía por sync (`escucharMiembros`); null para
     * miembros nunca vistos por Firestore. Ver `Plan/08-decisiones-tecnicas.md`. */
    val firebaseUid: String? = null,
)

data class AreaDeVida(
    val id: String,
    val nombre: String,
    val activa: Boolean = true,
    val esPredefinida: Boolean = true,
)
