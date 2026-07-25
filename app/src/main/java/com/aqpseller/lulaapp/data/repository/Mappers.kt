package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import com.aqpseller.lulaapp.data.local.entity.AreaDeVidaEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.TareaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MetodoLogin
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val listJson = Json { ignoreUnknownKeys = true }

fun encodeStringList(list: List<String>): String = listJson.encodeToString(list)
fun decodeStringList(json: String): List<String> =
    if (json.isBlank()) emptyList() else listJson.decodeFromString(json)

// --- Usuario ---

fun UsuarioEntity.toDomain() = Usuario(
    id = id,
    nombreCompleto = nombreCompleto,
    nombrePreferido = nombrePreferido,
    correo = correo,
    metodoLogin = MetodoLogin.valueOf(metodoLogin),
    privacidadAceptadaEn = privacidadAceptadaEn,
    modoDefectoAsistente = modoDefectoAsistente,
)

fun Usuario.toEntity() = UsuarioEntity(
    id = id,
    nombreCompleto = nombreCompleto,
    nombrePreferido = nombrePreferido,
    correo = correo,
    metodoLogin = metodoLogin.name,
    privacidadAceptadaEn = privacidadAceptadaEn,
    modoDefectoAsistente = modoDefectoAsistente,
)

// --- Espacio ---

fun EspacioEntity.toDomain() = Espacio(
    id = id,
    tipo = TipoEspacio.valueOf(tipo),
    nombre = nombre,
    creadoPor = creadoPor,
    fechaCreacion = fechaCreacion,
)

fun Espacio.toEntity() = EspacioEntity(
    id = id,
    tipo = tipo.name,
    nombre = nombre,
    creadoPor = creadoPor,
    fechaCreacion = fechaCreacion,
)

fun EspacioMiembro.toEntity() = EspacioMiembroEntity(
    espacioId = espacioId,
    usuarioId = usuarioId,
    rol = rol.name,
)

fun AreaDeVidaEntity.toDomain() = AreaDeVida(
    id = id,
    nombre = nombre,
    activa = activa,
    esPredefinida = esPredefinida,
)

fun AreaDeVida.toEntity() = AreaDeVidaEntity(
    id = id,
    nombre = nombre,
    activa = activa,
    esPredefinida = esPredefinida,
)

// --- Actividad ---

fun Actividad.toEntity() = ActividadEntity(
    id = id,
    tipo = tipo.name,
    espacioId = espacioId,
    nombre = nombre,
    propietario = propietario,
    responsablesJson = encodeStringList(responsables),
    puedeVerJson = encodeStringList(puedeVer),
    puedeRecordarJson = encodeStringList(puedeRecordar),
    estado = estado.name,
    privacidad = privacidad.name,
    syncStatus = syncStatus.name,
    esPremiumFeature = esPremiumFeature,
    areaDeVidaId = areaDeVidaId,
    momentoDelDia = momentoDelDia?.name,
    fechaCreacion = fechaCreacion,
)

fun ActividadEntity.toDomain(detalle: ActividadDetalle?) = Actividad(
    id = id,
    tipo = TipoActividad.valueOf(tipo),
    espacioId = espacioId,
    nombre = nombre,
    propietario = propietario,
    responsables = decodeStringList(responsablesJson),
    puedeVer = decodeStringList(puedeVerJson),
    puedeRecordar = decodeStringList(puedeRecordarJson),
    estado = EstadoActividad.valueOf(estado),
    privacidad = Privacidad.valueOf(privacidad),
    syncStatus = SyncStatus.valueOf(syncStatus),
    esPremiumFeature = esPremiumFeature,
    areaDeVidaId = areaDeVidaId,
    momentoDelDia = momentoDelDia?.let { MomentoDelDia.valueOf(it) },
    fechaCreacion = fechaCreacion,
    detalle = detalle,
)

fun ActividadDetalle.Habito.toEntity(actividadId: String) = HabitoDetalleEntity(
    actividadId = actividadId,
    momentoDelDia = momentoDelDia.name,
    frecuencia = frecuencia.name,
    diasEspecificosJson = if (diasEspecificos.isEmpty()) null else listJson.encodeToString(diasEspecificos),
    duracionInicialMin = duracionInicialMin,
    duracionObjetivoMin = duracionObjetivoMin,
    incrementoMin = incrementoMin,
    frecuenciaRevisionDias = frecuenciaRevisionDias,
)

fun HabitoDetalleEntity.toDomain() = ActividadDetalle.Habito(
    momentoDelDia = MomentoDelDia.valueOf(momentoDelDia),
    frecuencia = FrecuenciaHabito.valueOf(frecuencia),
    diasEspecificos = diasEspecificosJson?.let { listJson.decodeFromString(it) } ?: emptyList(),
    duracionInicialMin = duracionInicialMin,
    duracionObjetivoMin = duracionObjetivoMin,
    incrementoMin = incrementoMin,
    frecuenciaRevisionDias = frecuenciaRevisionDias,
)

fun ActividadDetalle.Tarea.toEntity(actividadId: String) = TareaDetalleEntity(
    actividadId = actividadId,
    fechaLimite = fechaLimite,
    prioridad = prioridad,
    importante = importante,
    urgente = urgente,
)

fun TareaDetalleEntity.toDomain() = ActividadDetalle.Tarea(
    fechaLimite = fechaLimite,
    prioridad = prioridad,
    importante = importante,
    urgente = urgente,
)

// --- Finanzas ---

fun MovimientoFinanciero.toEntity() = FinanzasEntity(
    id = id,
    espacioId = espacioId,
    tipo = tipo.name,
    monto = monto,
    categoria = categoria,
    descripcion = descripcion,
    fecha = fecha,
    privacidad = privacidad.name,
)

fun FinanzasEntity.toDomain() = MovimientoFinanciero(
    id = id,
    espacioId = espacioId,
    tipo = TipoMovimientoFinanciero.valueOf(tipo),
    monto = monto,
    categoria = categoria,
    descripcion = descripcion,
    fecha = fecha,
    privacidad = Privacidad.valueOf(privacidad),
)

// --- Registro diario ---

fun RegistroDiario.toEntity() = RegistroDiarioEntity(
    id = id,
    espacioId = espacioId,
    fecha = fecha,
    actividadesCompletadas = actividadesCompletadas,
    actividadesTotales = actividadesTotales,
    puntuacion = puntuacion,
    estadoAnimo = estadoAnimo,
    queLogre = queLogre,
    queCosto = queCosto,
    queAjusto = queAjusto,
)

fun RegistroDiarioEntity.toDomain() = RegistroDiario(
    id = id,
    espacioId = espacioId,
    fecha = fecha,
    actividadesCompletadas = actividadesCompletadas,
    actividadesTotales = actividadesTotales,
    puntuacion = puntuacion,
    estadoAnimo = estadoAnimo,
    queLogre = queLogre,
    queCosto = queCosto,
    queAjusto = queAjusto,
)
