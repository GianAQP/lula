package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import com.aqpseller.lulaapp.data.local.entity.AreaDeVidaEntity
import com.aqpseller.lulaapp.data.local.entity.CitaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.ConexionEntity
import com.aqpseller.lulaapp.data.local.entity.EntradaDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.data.local.entity.FechaImportanteDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.ListaEjecucionEntity
import com.aqpseller.lulaapp.data.local.entity.ListaItemEntity
import com.aqpseller.lulaapp.data.local.entity.MedicamentoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.MetaEntity
import com.aqpseller.lulaapp.data.local.entity.NotaEntity
import com.aqpseller.lulaapp.data.local.entity.PropositoPersonalEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroSemanalEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarEntity
import com.aqpseller.lulaapp.data.local.entity.RutinaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.SesionCitaEntity
import com.aqpseller.lulaapp.data.local.entity.SolicitudCompartirEntity
import com.aqpseller.lulaapp.data.local.entity.TareaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.TomaMedicamentoEntity
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.CanalEnvio
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.ItemListaSnapshot
import com.aqpseller.lulaapp.domain.model.ListaEjecucion
import com.aqpseller.lulaapp.domain.model.ListaItem
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.MetodoLogin
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.model.TipoConexion
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val listJson = Json { ignoreUnknownKeys = true }

fun encodeStringList(list: List<String>): String = listJson.encodeToString(list)
fun decodeStringList(json: String): List<String> =
    if (json.isBlank()) emptyList() else listJson.decodeFromString(json)

fun encodeStringMap(map: Map<String, String>): String = listJson.encodeToString(map)
fun decodeStringMap(json: String): Map<String, String> =
    if (json.isBlank()) emptyMap() else listJson.decodeFromString(json)

// --- Usuario ---

fun UsuarioEntity.toDomain() = Usuario(
    id = id,
    nombreCompleto = nombreCompleto,
    nombrePreferido = nombrePreferido,
    correo = correo,
    metodoLogin = MetodoLogin.valueOf(metodoLogin),
    privacidadAceptadaEn = privacidadAceptadaEn,
    modoDefectoAsistente = modoDefectoAsistente,
    horaDesayuno = horaDesayuno,
    horaAlmuerzo = horaAlmuerzo,
    horaCena = horaCena,
    confirmoMayorDe13 = confirmoMayorDe13,
    terminosAceptadosEn = terminosAceptadosEn,
    consentimientoDatosSaludEn = consentimientoDatosSaludEn,
    firebaseUid = firebaseUid,
    onboardingCompletadoEn = onboardingCompletadoEn,
    queMejorar = decodeStringList(queMejorarJson),
    comoEmpezar = comoEmpezar,
    momentoDelDiaPreferido = momentoDelDiaPreferido,
    porQueEmpezar = porQueEmpezar,
)

fun Usuario.toEntity() = UsuarioEntity(
    id = id,
    nombreCompleto = nombreCompleto,
    nombrePreferido = nombrePreferido,
    correo = correo,
    metodoLogin = metodoLogin.name,
    privacidadAceptadaEn = privacidadAceptadaEn,
    modoDefectoAsistente = modoDefectoAsistente,
    horaDesayuno = horaDesayuno,
    horaAlmuerzo = horaAlmuerzo,
    horaCena = horaCena,
    confirmoMayorDe13 = confirmoMayorDe13,
    terminosAceptadosEn = terminosAceptadosEn,
    consentimientoDatosSaludEn = consentimientoDatosSaludEn,
    firebaseUid = firebaseUid,
    onboardingCompletadoEn = onboardingCompletadoEn,
    queMejorarJson = encodeStringList(queMejorar),
    comoEmpezar = comoEmpezar,
    momentoDelDiaPreferido = momentoDelDiaPreferido,
    porQueEmpezar = porQueEmpezar,
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
    nombre = nombre,
)

fun ConexionEntity.toDomain() = Conexion(
    id = id,
    usuarioA = usuarioA,
    usuarioB = usuarioB,
    tipo = TipoConexion.valueOf(tipo),
    origenSolicitudId = origenSolicitudId,
    fechaConexion = fechaConexion,
)

fun EspacioMiembroEntity.toDomain() = EspacioMiembro(
    espacioId = espacioId,
    usuarioId = usuarioId,
    rol = RolEnEspacio.valueOf(rol),
    nombre = nombre,
)

fun RetoFamiliarEntity.toDomain(participantesIds: List<String>) = RetoFamiliar(
    id = id,
    espacioId = espacioId,
    nombre = nombre,
    objetivo = objetivo,
    frecuencia = FrecuenciaRetoFamiliar.valueOf(frecuencia),
    participantesIds = participantesIds,
    recompensa = recompensa,
)

fun RetoFamiliar.toEntity() = RetoFamiliarEntity(
    id = id,
    espacioId = espacioId,
    nombre = nombre,
    objetivo = objetivo,
    frecuencia = frecuencia.name,
    recompensa = recompensa,
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
    activa = activa,
    fechaCompletado = fechaCompletado,
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
    activa = activa,
    detalle = detalle,
    fechaCompletado = fechaCompletado,
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
    horaRecordatorio = horaRecordatorio,
    nivelRecordatorio = nivelRecordatorio.name,
    duracionActualMin = duracionActualMin,
    proximaRevisionEpochDay = proximaRevisionEpochDay,
)

fun HabitoDetalleEntity.toDomain() = ActividadDetalle.Habito(
    momentoDelDia = MomentoDelDia.valueOf(momentoDelDia),
    frecuencia = FrecuenciaHabito.valueOf(frecuencia),
    diasEspecificos = diasEspecificosJson?.let { listJson.decodeFromString(it) } ?: emptyList(),
    duracionInicialMin = duracionInicialMin,
    duracionObjetivoMin = duracionObjetivoMin,
    incrementoMin = incrementoMin,
    frecuenciaRevisionDias = frecuenciaRevisionDias,
    horaRecordatorio = horaRecordatorio,
    nivelRecordatorio = NivelRecordatorio.valueOf(nivelRecordatorio),
    duracionActualMin = duracionActualMin,
    proximaRevisionEpochDay = proximaRevisionEpochDay,
)

fun ActividadDetalle.Tarea.toEntity(actividadId: String) = TareaDetalleEntity(
    actividadId = actividadId,
    fechaLimite = fechaLimite,
    prioridad = prioridad,
    importante = importante,
    urgente = urgente,
    horaRecordatorio = horaRecordatorio,
    nivelRecordatorio = nivelRecordatorio.name,
    recurrencia = recurrencia.name,
    actividadVinculadaId = actividadVinculadaId,
)

fun TareaDetalleEntity.toDomain() = ActividadDetalle.Tarea(
    fechaLimite = fechaLimite,
    prioridad = prioridad,
    importante = importante,
    urgente = urgente,
    nivelRecordatorio = NivelRecordatorio.valueOf(nivelRecordatorio),
    horaRecordatorio = horaRecordatorio,
    recurrencia = RecurrenciaTarea.valueOf(recurrencia),
    actividadVinculadaId = actividadVinculadaId,
)

fun ActividadDetalle.Rutina.toEntity(actividadId: String) = RutinaDetalleEntity(
    actividadId = actividadId,
    actividadesIncluidasJson = encodeStringList(actividadesIncluidasIds),
    momentoDelDia = momentoDelDia.name,
)

fun RutinaDetalleEntity.toDomain() = ActividadDetalle.Rutina(
    actividadesIncluidasIds = decodeStringList(actividadesIncluidasJson),
    momentoDelDia = MomentoDelDia.valueOf(momentoDelDia),
)

fun ActividadDetalle.Medicamento.toEntity(actividadId: String) = MedicamentoDetalleEntity(
    actividadId = actividadId,
    nombreMedicamento = nombreMedicamento,
    dosis = dosis,
    modoFrecuencia = modoFrecuencia.name,
    intervaloHoras = intervaloHoras,
    horaPrimeraDosis = horaPrimeraDosis,
    horariosCalculadosJson = if (horariosCalculados.isEmpty()) null else listJson.encodeToString(horariosCalculados),
    comidasRelacionadasJson = if (comidasRelacionadas.isEmpty()) null else listJson.encodeToString(comidasRelacionadas),
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    cantidadDosisTotal = cantidadDosisTotal,
    nivelRecordatorio = nivelRecordatorio.name,
    recordatorioPersistente = recordatorioPersistente,
    intervaloPersistenciaMin = intervaloPersistenciaMin,
)

fun MedicamentoDetalleEntity.toDomain() = ActividadDetalle.Medicamento(
    nombreMedicamento = nombreMedicamento,
    dosis = dosis,
    modoFrecuencia = ModoFrecuenciaMedicamento.valueOf(modoFrecuencia),
    intervaloHoras = intervaloHoras,
    horaPrimeraDosis = horaPrimeraDosis,
    horariosCalculados = horariosCalculadosJson?.let { listJson.decodeFromString(it) } ?: emptyList(),
    comidasRelacionadas = comidasRelacionadasJson?.let { listJson.decodeFromString<List<ComidaRelacionada>>(it) } ?: emptyList(),
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    cantidadDosisTotal = cantidadDosisTotal,
    nivelRecordatorio = NivelRecordatorio.valueOf(nivelRecordatorio),
    recordatorioPersistente = recordatorioPersistente,
    intervaloPersistenciaMin = intervaloPersistenciaMin,
)

fun ActividadDetalle.Cita.toEntity(actividadId: String) = CitaDetalleEntity(
    actividadId = actividadId,
    lugar = lugar,
    motivo = motivo,
    fechaHora = fechaHora,
    recordatoriosJson = if (recordatorios.isEmpty()) null else listJson.encodeToString(recordatorios),
    nivelRecordatorio = nivelRecordatorio.name,
    esCurso = esCurso,
    diasSemanaJson = if (diasSemana.isEmpty()) null else listJson.encodeToString(diasSemana.toList()),
    horaSesion = horaSesion,
    fechaInicioCurso = fechaInicioCurso,
    cantidadSesionesTotal = cantidadSesionesTotal,
)

fun CitaDetalleEntity.toDomain() = ActividadDetalle.Cita(
    lugar = lugar,
    motivo = motivo,
    fechaHora = fechaHora,
    recordatorios = recordatoriosJson?.let { listJson.decodeFromString<List<RecordatorioCita>>(it) } ?: emptyList(),
    nivelRecordatorio = NivelRecordatorio.valueOf(nivelRecordatorio),
    esCurso = esCurso,
    diasSemana = diasSemanaJson?.let { listJson.decodeFromString<List<Int>>(it) }?.toSet() ?: emptySet(),
    horaSesion = horaSesion,
    fechaInicioCurso = fechaInicioCurso,
    cantidadSesionesTotal = cantidadSesionesTotal,
)

fun SesionCita.toEntity() = SesionCitaEntity(
    id = id,
    actividadId = actividadId,
    numeroSesion = numeroSesion,
    fecha = fecha,
    fechaOriginal = fechaOriginal,
    horario = horario,
    estado = estado.name,
)

fun SesionCitaEntity.toDomain() = SesionCita(
    id = id,
    actividadId = actividadId,
    numeroSesion = numeroSesion,
    fecha = fecha,
    fechaOriginal = fechaOriginal,
    horario = horario,
    estado = EstadoActividad.valueOf(estado),
)

fun TomaMedicamentoEntity.toDomain() = TomaMedicamento(
    id = id,
    actividadId = actividadId,
    fecha = fecha,
    horario = horario,
    estado = EstadoActividad.valueOf(estado),
)

fun ActividadDetalle.FechaImportante.toEntity(actividadId: String) = FechaImportanteDetalleEntity(
    actividadId = actividadId,
    recurrencia = recurrencia.name,
    fechaBase = fechaBase,
    horaNotificacion = horaNotificacion,
    anticipacion = anticipacion.name,
    tipoAviso = tipoAviso.name,
)

fun FechaImportanteDetalleEntity.toDomain() = ActividadDetalle.FechaImportante(
    recurrencia = Recurrencia.valueOf(recurrencia),
    fechaBase = fechaBase,
    horaNotificacion = horaNotificacion,
    anticipacion = AnticipacionRecordatorio.valueOf(anticipacion),
    tipoAviso = TipoAviso.valueOf(tipoAviso),
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

fun RegistroSemanal.toEntity() = RegistroSemanalEntity(
    id = id,
    espacioId = espacioId,
    semana = semana,
    cumplimientoGeneralPorcentaje = cumplimientoGeneralPorcentaje,
    rachaMaxima = rachaMaxima,
    queLogre = queLogre,
    queNoFunciono = queNoFunciono,
    queAjusto = queAjusto,
)

fun RegistroSemanalEntity.toDomain() = RegistroSemanal(
    id = id,
    espacioId = espacioId,
    semana = semana,
    cumplimientoGeneralPorcentaje = cumplimientoGeneralPorcentaje,
    rachaMaxima = rachaMaxima,
    queLogre = queLogre,
    queNoFunciono = queNoFunciono,
    queAjusto = queAjusto,
)

// --- Lista ---

fun ListaItemEntity.toDomain() = ListaItem(
    id = id,
    listaId = listaId,
    texto = texto,
    marcado = marcado,
    orden = orden,
)

fun ListaEjecucion.toEntity() = ListaEjecucionEntity(
    id = id,
    listaId = listaId,
    fecha = fecha,
    itemsJson = listJson.encodeToString(items),
)

fun ListaEjecucionEntity.toDomain() = ListaEjecucion(
    id = id,
    listaId = listaId,
    fecha = fecha,
    items = listJson.decodeFromString<List<ItemListaSnapshot>>(itemsJson),
)

// --- Meta ---

fun Meta.toEntity() = MetaEntity(
    id = id,
    espacioId = espacioId,
    nombre = nombre,
    areaDeVidaId = areaDeVidaId,
    fechaLimite = fechaLimite,
    comoSeMide = comoSeMide.name,
    valorObjetivo = valorObjetivo,
    valorActual = valorActual,
    ultimoHitoCelebrado = ultimoHitoCelebrado,
    categoria = categoria?.name,
    nivelRecordatorio = nivelRecordatorio.name,
)

fun MetaEntity.toDomain(actividadesVinculadasIds: List<String>) = Meta(
    id = id,
    espacioId = espacioId,
    nombre = nombre,
    areaDeVidaId = areaDeVidaId,
    fechaLimite = fechaLimite,
    comoSeMide = ComoSeMideMeta.valueOf(comoSeMide),
    valorObjetivo = valorObjetivo,
    valorActual = valorActual,
    actividadesVinculadasIds = actividadesVinculadasIds,
    ultimoHitoCelebrado = ultimoHitoCelebrado,
    categoria = categoria?.let { runCatching { CategoriaMeta.valueOf(it) }.getOrNull() },
    nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(nivelRecordatorio) }.getOrDefault(NivelRecordatorio.SONIDO),
)

// --- Solicitud de compartir (Círculo de cuidado) ---

fun SolicitudCompartir.toEntity() = SolicitudCompartirEntity(
    id = id,
    de = de,
    para = para,
    tieneCuenta = tieneCuenta,
    elementoId = elementoId,
    contexto = contexto,
    deNombre = deNombre,
    tipo = tipo.name,
    permisos = permisos.name,
    estado = estado.name,
    canalEnvio = canalEnvio?.name,
    fechaSolicitud = fechaSolicitud,
    fechaRespuesta = fechaRespuesta,
)

fun SolicitudCompartirEntity.toDomain() = SolicitudCompartir(
    id = id,
    de = de,
    para = para,
    tieneCuenta = tieneCuenta,
    elementoId = elementoId,
    contexto = contexto,
    deNombre = deNombre,
    tipo = runCatching { TipoSolicitud.valueOf(tipo) }.getOrDefault(TipoSolicitud.ACTIVIDAD),
    permisos = PermisoCompartir.valueOf(permisos),
    estado = EstadoSolicitud.valueOf(estado),
    canalEnvio = canalEnvio?.let { CanalEnvio.valueOf(it) },
    fechaSolicitud = fechaSolicitud,
    fechaRespuesta = fechaRespuesta,
)

// --- Nota ---

fun Nota.toEntity() = NotaEntity(
    id = id,
    espacioId = espacioId,
    propietario = propietario,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    fechaEdicion = fechaEdicion,
    orden = orden,
)

fun NotaEntity.toDomain() = Nota(
    id = id,
    espacioId = espacioId,
    propietario = propietario,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    fechaEdicion = fechaEdicion,
    orden = orden,
)

// --- Propósito personal ---

fun PropositoPersonal.toEntity() = PropositoPersonalEntity(
    espacioId = espacioId,
    propietario = propietario,
    respuestasJson = encodeStringMap(respuestas),
    fechaEdicion = fechaEdicion,
)

fun PropositoPersonalEntity.toDomain() = PropositoPersonal(
    espacioId = espacioId,
    propietario = propietario,
    respuestas = decodeStringMap(respuestasJson),
    fechaEdicion = fechaEdicion,
)

// --- Diario ---

fun EntradaDiario.toEntity() = EntradaDiarioEntity(
    id = id,
    espacioId = espacioId,
    propietario = propietario,
    titulo = titulo,
    texto = texto,
    areaDeVidaId = areaDeVidaId,
    fecha = fecha,
    privacidad = privacidad.name,
    fotosJson = if (fotos.isEmpty()) null else encodeStringList(fotos),
)

fun EntradaDiarioEntity.toDomain() = EntradaDiario(
    id = id,
    espacioId = espacioId,
    propietario = propietario,
    titulo = titulo,
    texto = texto,
    areaDeVidaId = areaDeVidaId,
    fecha = fecha,
    privacidad = Privacidad.valueOf(privacidad),
    fotos = fotosJson?.let { decodeStringList(it) } ?: emptyList(),
)
