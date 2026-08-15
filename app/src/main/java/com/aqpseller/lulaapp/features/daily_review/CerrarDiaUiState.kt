package com.aqpseller.lulaapp.features.daily_review

import kotlinx.datetime.LocalDate

data class CerrarDiaUiState(
    val cargando: Boolean = true,
    val fecha: LocalDate? = null,
    /** false = fecha distinta de hoy (llenar/actualizar un día anterior desde Calendario) —
     * ahí `actividadesCompletadas`/`actividadesTotales` no se auto-calculan del estado en vivo
     * (eso reflejaría HOY, no el día elegido), se escriben a mano. Ver `08-decisiones-tecnicas.md`. */
    val esHoy: Boolean = true,
    val actividadesCompletadas: Int = 0,
    val actividadesTotales: Int = 0,
    /** Si esa fecha ya se había cerrado antes, las respuestas que había — para no partir en
     * blanco y borrarlas sin querer al volver a entrar (bug reportado, ver `08-decisiones-tecnicas.md`). */
    val yaExistiaRegistro: Boolean = false,
    val queLogreInicial: String? = null,
    val queCostoInicial: String? = null,
    val queAjustoInicial: String? = null,
    val cerrado: Boolean = false,
    val rachaFinal: Int = 0,
    /** Distinto de null solo si `rachaFinal` acaba de cruzar un hito (7/21/30/60...) por primera
     * vez — dispara la pantalla grande de celebración en vez de la vista chica normal. */
    val hitoAlcanzado: Int? = null,
    /** Mensaje a mostrar al cerrar: el de hito si `hitoAlcanzado != null`, si no el diario (o el
     * de "casi llegas" si falta un solo día para el próximo hito). Elegido al azar entre varios
     * — se calcula una vez al cerrar, no en cada recomposición, para no cambiar solo. Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    val mensajeCierre: String = "",
)
