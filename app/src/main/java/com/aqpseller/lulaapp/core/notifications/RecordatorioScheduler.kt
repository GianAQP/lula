package com.aqpseller.lulaapp.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoAviso
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import javax.inject.Inject

/**
 * Programa recordatorios locales con `AlarmManager`. Si el dispositivo no permite alarmas
 * exactas (Android 12+ sin el permiso concedido), cae a una alarma inexacta en vez de fallar
 * — un recordatorio que llega con algunos minutos de diferencia es mejor que ninguno.
 */
class RecordatorioScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    fun programarHabito(actividadId: String, nombre: String, horaRecordatorio: String, nivel: NivelRecordatorio) {
        val trigger = proximoTrigger(horaRecordatorio) ?: return
        programar(actividadId, nombre, trigger, TipoActividad.HABITO, horaRecordatorio = horaRecordatorio, nivel = nivel)
    }

    fun programarTarea(actividadId: String, nombre: String, fechaLimiteMillis: Long, horaRecordatorio: String, nivel: NivelRecordatorio) {
        val trigger = triggerParaFecha(fechaLimiteMillis, horaRecordatorio) ?: return
        if (trigger <= DateTimeUtils.ahoraEpochMillis()) return
        programar(actividadId, nombre, trigger, TipoActividad.TAREA, horaRecordatorio = horaRecordatorio, nivel = nivel)
    }

    /**
     * Un Medicamento puede tener varias tomas por día — cada `horario` es su propia alarma
     * independiente (clave compuesta `actividadId:horario`), y se reprograma sola para el día
     * siguiente al sonar (`RecordatorioReceiver`), igual que un Hábito. [instruccion] es el
     * texto original (ej. "Después del almuerzo"), no solo la hora calculada.
     */
    fun programarMedicamento(
        actividadId: String,
        nombre: String,
        horario: String,
        instruccion: String,
        nivel: NivelRecordatorio,
        recordatorioPersistente: Boolean = false,
        intervaloPersistenciaMin: Int? = null,
    ) {
        val trigger = proximoTrigger(horario) ?: return
        programar(
            actividadId, nombre, trigger, TipoActividad.MEDICAMENTO,
            horaRecordatorio = horario, nivel = nivel, horario = horario, instruccion = instruccion,
            persistente = recordatorioPersistente, intervaloPersistenciaMin = intervaloPersistenciaMin,
        )
    }

    fun cancelarMedicamento(actividadId: String, horario: String) {
        cancelar(actividadId, horario)
        cancelarRenotificacionMedicamento(actividadId, horario)
    }

    /**
     * "Insistencia" del recordatorio persistente — una alarma aparte de la diaria normal, con
     * su propia clave (`actividadId:horario:renotif`) para no pisarla. `RecordatorioReceiver`
     * decide si sigue insistiendo (toma aún sin marcar y no cambió el día) o se detiene.
     */
    fun programarRenotificacionMedicamento(
        actividadId: String,
        nombre: String,
        horario: String,
        instruccion: String,
        nivel: NivelRecordatorio,
        intervaloMin: Int,
        fechaOriginalEpochDay: Long,
    ) {
        val manager = alarmManager ?: return
        val trigger = DateTimeUtils.ahoraEpochMillis() + intervaloMin * 60_000L
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_ACTIVIDAD_ID, actividadId)
            putExtra(RecordatorioReceiver.EXTRA_NOMBRE, nombre)
            putExtra(RecordatorioReceiver.EXTRA_TIPO, TipoActividad.MEDICAMENTO.name)
            putExtra(RecordatorioReceiver.EXTRA_HORA, horario)
            putExtra(RecordatorioReceiver.EXTRA_NIVEL, nivel.name)
            putExtra(RecordatorioReceiver.EXTRA_HORARIO, horario)
            if (instruccion.isNotBlank()) putExtra(RecordatorioReceiver.EXTRA_INSTRUCCION, instruccion)
            putExtra(RecordatorioReceiver.EXTRA_ES_RENOTIFICACION, true)
            putExtra(RecordatorioReceiver.EXTRA_INTERVALO_PERSISTENCIA_MIN, intervaloMin)
            putExtra(RecordatorioReceiver.EXTRA_FECHA_ORIGINAL_EPOCH_DAY, fechaOriginalEpochDay)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            claveRequestCodeRenotificacion(actividadId, horario),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (puedeExacta) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancelarRenotificacionMedicamento(actividadId: String, horario: String) {
        val intent = Intent(context, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            claveRequestCodeRenotificacion(actividadId, horario),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun claveRequestCodeRenotificacion(actividadId: String, horario: String): Int =
        "$actividadId:$horario:renotif".hashCode()

    /**
     * Una Cita puede tener varios recordatorios (ej. "un día antes a las 20:00" y "el mismo
     * día a las 7:00"), cada uno a **su propia hora**, no a la hora de la cita — antes se
     * calculaba restando el offset a `fechaHoraMillis`, lo que hacía sonar el recordatorio
     * siempre a la misma hora que la cita. Cada recordatorio es su propia alarma independiente
     * (clave compuesta `actividadId:cita:anticipacion`, igual que las tomas de Medicamento).
     */
    fun programarCita(actividadId: String, nombre: String, fechaHoraMillis: Long, recordatorios: List<RecordatorioCita>, nivel: NivelRecordatorio) {
        recordatorios.forEach { recordatorio ->
            val trigger = triggerParaRecordatorioCita(fechaHoraMillis, recordatorio)
            if (trigger <= DateTimeUtils.ahoraEpochMillis()) return@forEach
            programar(actividadId, nombre, trigger, TipoActividad.CITA, horaRecordatorio = "", nivel = nivel, horario = claveRecordatorioCita(recordatorio.anticipacion))
        }
    }

    fun cancelarCita(actividadId: String, anticipacion: AnticipacionRecordatorio) =
        cancelar(actividadId, claveRecordatorioCita(anticipacion))

    private fun claveRecordatorioCita(anticipacion: AnticipacionRecordatorio) = "cita:${anticipacion.name}"

    /**
     * Una sesión de un curso de Cita (radioterapia, masajes) es igual a una Cita puntual, pero
     * con una clave compuesta que además incluye [numeroSesion] — así reprogramar o marcar la
     * sesión 7 nunca pisa la alarma de la sesión 8, y viceversa.
     */
    fun programarSesionCita(
        actividadId: String,
        nombre: String,
        numeroSesion: Int,
        fechaHoraMillis: Long,
        recordatorios: List<RecordatorioCita>,
        nivel: NivelRecordatorio,
    ) {
        recordatorios.forEach { recordatorio ->
            val trigger = triggerParaRecordatorioCita(fechaHoraMillis, recordatorio)
            if (trigger <= DateTimeUtils.ahoraEpochMillis()) return@forEach
            programar(actividadId, nombre, trigger, TipoActividad.CITA, horaRecordatorio = "", nivel = nivel, horario = claveRecordatorioSesionCita(numeroSesion, recordatorio.anticipacion))
        }
    }

    fun cancelarSesionCita(actividadId: String, numeroSesion: Int, anticipacion: AnticipacionRecordatorio) =
        cancelar(actividadId, claveRecordatorioSesionCita(numeroSesion, anticipacion))

    private fun claveRecordatorioSesionCita(numeroSesion: Int, anticipacion: AnticipacionRecordatorio) =
        "cita:sesion:$numeroSesion:${anticipacion.name}"

    private fun triggerParaRecordatorioCita(fechaHoraMillis: Long, recordatorio: RecordatorioCita): Long {
        val diasAntes = when (recordatorio.anticipacion) {
            AnticipacionRecordatorio.MISMO_DIA -> 0
            AnticipacionRecordatorio.UN_DIA_ANTES -> 1
            AnticipacionRecordatorio.UNA_SEMANA_ANTES -> 7
        }
        val diaCita = DateTimeUtils.epochMillisToLocalDate(fechaHoraMillis)
        val diaTrigger = diaCita.minus(DatePeriod(days = diasAntes))
        val (hora, minuto) = parsearHora(recordatorio.hora) ?: (8 to 0)
        return LocalDateTime(diaTrigger.year, diaTrigger.monthNumber, diaTrigger.dayOfMonth, hora, minuto)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    /**
     * Fecha importante: un solo recordatorio con anticipación configurable. Si `recurrencia`
     * es semanal o anual, se reprograma sola (`RecordatorioReceiver`) para la **próxima**
     * ocurrencia — nunca se muta `fechaBase` en la base de datos, siempre se recalcula desde
     * la fecha original guardada, igual que `proximoTrigger` hace con la hora de un Hábito.
     */
    fun programarFechaImportante(
        actividadId: String,
        nombre: String,
        fechaBaseMillis: Long,
        horaNotificacion: String,
        anticipacion: AnticipacionRecordatorio,
        recurrencia: Recurrencia,
        tipoAviso: TipoAviso,
    ) {
        val proximaOcurrencia = proximaOcurrenciaFechaImportante(fechaBaseMillis, recurrencia) ?: return
        val diasAntes = diasSegunAnticipacion(anticipacion)
        val diaTrigger = DateTimeUtils.epochMillisToLocalDate(proximaOcurrencia).minus(DatePeriod(days = diasAntes))
        val (hora, minuto) = parsearHora(horaNotificacion) ?: (9 to 0)
        val trigger = LocalDateTime(diaTrigger.year, diaTrigger.monthNumber, diaTrigger.dayOfMonth, hora, minuto)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        if (trigger <= DateTimeUtils.ahoraEpochMillis()) return
        val nivel = if (tipoAviso == TipoAviso.ALARMA_SONORA) NivelRecordatorio.ALARMA else NivelRecordatorio.SILENCIOSO
        programar(actividadId, nombre, trigger, TipoActividad.FECHA_IMPORTANTE, horaRecordatorio = horaNotificacion, nivel = nivel)
    }

    private fun diasSegunAnticipacion(anticipacion: AnticipacionRecordatorio): Int = when (anticipacion) {
        AnticipacionRecordatorio.MISMO_DIA -> 0
        AnticipacionRecordatorio.UN_DIA_ANTES -> 1
        AnticipacionRecordatorio.UNA_SEMANA_ANTES -> 7
    }

    /** Fecha (medianoche local) de la próxima vez que ocurre `fechaBase`, según `recurrencia`. */
    private fun proximaOcurrenciaFechaImportante(fechaBaseMillis: Long, recurrencia: Recurrencia): Long? {
        var fecha = DateTimeUtils.epochMillisToLocalDate(fechaBaseMillis)
        val hoy = DateTimeUtils.hoy()
        when (recurrencia) {
            Recurrencia.UNICA -> if (fecha < hoy) return null
            Recurrencia.SEMANAL -> while (fecha < hoy) fecha = fecha.plus(DatePeriod(days = 7))
            Recurrencia.ANUAL -> while (fecha < hoy) fecha = fecha.plus(DatePeriod(years = 1))
        }
        return fecha.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    /**
     * Recordatorio diario genérico ("¿cerraste tu día?"), no ligado a ningún hábito/tarea en
     * particular — a diferencia de todo lo demás en esta clase. Se auto-reprograma cada vez que
     * suena (mismo patrón que un Hábito), y `RecordatorioReceiver` decide ahí si hace falta
     * mostrarlo (se salta si el día ya se cerró). Ver `Plan/08-decisiones-tecnicas.md`.
     */
    fun programarRecordatorioCierreDia(hora: String) {
        val trigger = proximoTrigger(hora) ?: return
        val manager = alarmManager ?: return
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_ES_RECORDATORIO_CIERRE_DIA, true)
            putExtra(RecordatorioReceiver.EXTRA_HORA, hora)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, CLAVE_REQUEST_CODE_CIERRE_DIA, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (puedeExacta) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancelarRecordatorioCierreDia() {
        val intent = Intent(context, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, CLAVE_REQUEST_CODE_CIERRE_DIA, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Un solo aviso el día que llega la fecha límite de una Meta — no se repite (no tiene
     * sentido recordar de nuevo al día siguiente algo que ya venció), a diferencia de Hábito/
     * Medicamento. Hora fija 09:00 — una Meta no tiene un "horaRecordatorio" propio que elegir,
     * a diferencia de Tarea. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    fun programarMeta(metaId: String, nombre: String, fechaLimiteMillis: Long, nivel: NivelRecordatorio) {
        val trigger = triggerParaFecha(fechaLimiteMillis, HORA_RECORDATORIO_META) ?: return
        if (trigger <= DateTimeUtils.ahoraEpochMillis()) return
        val manager = alarmManager ?: return
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_META_ID, metaId)
            putExtra(RecordatorioReceiver.EXTRA_NOMBRE, nombre)
            putExtra(RecordatorioReceiver.EXTRA_NIVEL, nivel.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, claveRequestCodeMeta(metaId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (puedeExacta) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancelarMeta(metaId: String) {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_META_ID, metaId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, claveRequestCodeMeta(metaId), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun claveRequestCodeMeta(metaId: String) = "recordatorio_meta_$metaId".hashCode()

    /**
     * Recordatorio genérico "revisa Lula" por franja del día (Mañana/Tarde/Noche) — igual que
     * el de cierre del día, pero independiente: no depende de si el día se cerró, es solo para
     * no perderse de entrar a la app durante esa franja. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    fun programarRecordatorioFranja(momento: MomentoDelDia, hora: String) {
        val trigger = proximoTrigger(hora) ?: return
        val manager = alarmManager ?: return
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_MOMENTO_FRANJA, momento.name)
            putExtra(RecordatorioReceiver.EXTRA_HORA, hora)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, claveRequestCodeFranja(momento), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (puedeExacta) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancelarRecordatorioFranja(momento: MomentoDelDia) {
        val intent = Intent(context, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, claveRequestCodeFranja(momento), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun claveRequestCodeFranja(momento: MomentoDelDia) = "recordatorio_franja_${momento.name}".hashCode()

    /**
     * Pospone el recordatorio de una actividad puntual [minutos] desde ahora, sin tocar la
     * hora fija diaria guardada en el hábito/tarea — al sonar de nuevo, si es hábito, se
     * reprograma solo para su hora habitual del día siguiente (ver `RecordatorioReceiver`).
     */
    fun posponer(actividadId: String, nombre: String, esHabito: Boolean, horaRecordatorio: String, nivel: NivelRecordatorio, minutos: Int = 15) {
        val trigger = DateTimeUtils.ahoraEpochMillis() + minutos * 60_000L
        programar(actividadId, nombre, trigger, if (esHabito) TipoActividad.HABITO else TipoActividad.TAREA, horaRecordatorio, nivel)
    }

    fun cancelar(actividadId: String, horario: String? = null) {
        val pendingIntent = crearPendingIntent(
            actividadId = actividadId,
            nombre = "",
            horaRecordatorio = "",
            tipo = TipoActividad.TAREA,
            nivel = NivelRecordatorio.SONIDO,
            horario = horario,
            instruccion = "",
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun programar(
        actividadId: String,
        nombre: String,
        triggerAtMillis: Long,
        tipo: TipoActividad,
        horaRecordatorio: String,
        nivel: NivelRecordatorio,
        horario: String? = null,
        instruccion: String = "",
        persistente: Boolean = false,
        intervaloPersistenciaMin: Int? = null,
    ) {
        val manager = alarmManager ?: return
        val pendingIntent = crearPendingIntent(
            actividadId = actividadId,
            nombre = nombre,
            horaRecordatorio = horaRecordatorio,
            tipo = tipo,
            nivel = nivel,
            horario = horario,
            instruccion = instruccion,
            persistente = persistente,
            intervaloPersistenciaMin = intervaloPersistenciaMin,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (puedeExacta) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun crearPendingIntent(
        actividadId: String,
        nombre: String,
        horaRecordatorio: String,
        tipo: TipoActividad,
        nivel: NivelRecordatorio,
        horario: String?,
        instruccion: String,
        flags: Int,
        persistente: Boolean = false,
        intervaloPersistenciaMin: Int? = null,
    ): PendingIntent? {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_ACTIVIDAD_ID, actividadId)
            putExtra(RecordatorioReceiver.EXTRA_NOMBRE, nombre)
            putExtra(RecordatorioReceiver.EXTRA_TIPO, tipo.name)
            putExtra(RecordatorioReceiver.EXTRA_HORA, horaRecordatorio)
            putExtra(RecordatorioReceiver.EXTRA_NIVEL, nivel.name)
            if (horario != null) putExtra(RecordatorioReceiver.EXTRA_HORARIO, horario)
            if (instruccion.isNotBlank()) putExtra(RecordatorioReceiver.EXTRA_INSTRUCCION, instruccion)
            if (persistente && intervaloPersistenciaMin != null) {
                putExtra(RecordatorioReceiver.EXTRA_INTERVALO_PERSISTENCIA_MIN, intervaloPersistenciaMin)
            }
        }
        return PendingIntent.getBroadcast(context, claveRequestCode(actividadId, horario), intent, flags)
    }

    /** Un Medicamento con varias tomas necesita una alarma independiente por horario. */
    private fun claveRequestCode(actividadId: String, horario: String?): Int =
        (if (horario != null) "$actividadId:$horario" else actividadId).hashCode()

    /** Próxima ocurrencia de esa hora: hoy si todavía no pasó, si no mañana. */
    private fun proximoTrigger(horaRecordatorio: String): Long? {
        val (hora, minuto) = parsearHora(horaRecordatorio) ?: return null
        val zona = TimeZone.currentSystemDefault()
        val hoy = DateTimeUtils.hoy()
        val hoyConHora = LocalDateTime(hoy.year, hoy.monthNumber, hoy.dayOfMonth, hora, minuto).toInstant(zona).toEpochMilliseconds()
        if (hoyConHora > DateTimeUtils.ahoraEpochMillis()) return hoyConHora
        val manana = hoy.plus(DatePeriod(days = 1))
        return LocalDateTime(manana.year, manana.monthNumber, manana.dayOfMonth, hora, minuto).toInstant(zona).toEpochMilliseconds()
    }

    private fun triggerParaFecha(fechaMillis: Long, horaRecordatorio: String): Long? {
        val (hora, minuto) = parsearHora(horaRecordatorio) ?: return null
        val fecha = DateTimeUtils.epochMillisToLocalDate(fechaMillis)
        return LocalDateTime(fecha.year, fecha.monthNumber, fecha.dayOfMonth, hora, minuto)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    companion object {
        private val CLAVE_REQUEST_CODE_CIERRE_DIA = "recordatorio_cierre_dia".hashCode()
        private const val HORA_RECORDATORIO_META = "09:00"
    }

    private fun parsearHora(horaRecordatorio: String): Pair<Int, Int>? {
        val partes = horaRecordatorio.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return hora to minuto
    }
}
