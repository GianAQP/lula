package com.aqpseller.lulaapp.domain.model

data class Usuario(
    val id: String,
    val nombreCompleto: String,
    val nombrePreferido: String,
    val correo: String?,
    val metodoLogin: MetodoLogin,
    val privacidadAceptadaEn: Long?,
    val modoDefectoAsistente: String? = null,
    /** Formato "HH:mm" — usados para calcular horarios de medicamentos "según las comidas". */
    val horaDesayuno: String? = null,
    val horaAlmuerzo: String? = null,
    val horaCena: String? = null,
    /** Ver `Plan/11-cuentas-y-conexiones.md` — checkbox, no fecha de nacimiento exacta. */
    val confirmoMayorDe13: Boolean = false,
    val terminosAceptadosEn: Long? = null,
    /** Solo se pide si la persona declara que va a usar Medicamentos/Citas — categoría
     * sensible aparte en Play Store, no se mezcla con `privacidadAceptadaEn`. */
    val consentimientoDatosSaludEn: Long? = null,
)
