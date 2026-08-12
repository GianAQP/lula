# Arquitectura y modelo de datos — Lula

> Actualizado 2026-08-08 para que cuadre con el código real (auditoría solicitada por el
> usuario). Antes de esta fecha, este documento reflejaba el diseño original de Fase 0.1 y
> había quedado desactualizado a medida que el modelo creció en sesiones posteriores — el
> detalle de cada cambio individual sigue viviendo en `08-decisiones-tecnicas.md` con su fecha;
> este archivo es la fotografía consolidada de "cómo es hoy", no un historial.

## Estructura de carpetas (Clean Architecture)

Estructura real (`app/src/main/java/com/aqpseller/lulaapp/`):

```
core/
├── database/       # Room: LulaDatabase, Migrations, Converters
├── notifications/  # AlarmManager (RecordatorioScheduler/Receiver), canales, BootReceiver
├── security/       # BiometricAuthenticator, SesionPrivadaState (Zona Privada)
├── sync/           # SyncStatus — sin cliente de red todavía (ver sección de sincronización)
├── ui/             # componentes reutilizables (SelectorRow, StatPill, EmptyState, etc.)
└── utils/          # DateTimeUtils, IdGenerator, HorariosMedicamentoUtils, etc.

data/
├── local/           # entidades Room (entity/) + DAOs (dao/)
├── repository/      # implementación de repositorios + Mappers.kt (entity ↔ dominio)
└── preferences/      # DataStore: AjustesRepositoryImpl

domain/
├── model/           # modelos de dominio (Actividad, Espacio, Meta, Usuario...)
├── repository/      # interfaces
└── usecase/         # casos de uso, organizados por sub-paquete (actividad/, cita/, medicamento/...)

navigation/          # LulaNavHost, LulaDestinations, LulaTopBar, LulaBottomBar, AddMenuSheet

features/            # una carpeta por pantalla/flujo, cada una con *Screen.kt + *ViewModel.kt + *UiState.kt
├── home/            # "Hoy"
├── habits/, tasks/, routines/, finances/, goals/, health/, important_dates/
├── daily_review/    # Cerrar mi día
├── weekly_review/   # Revisión semanal
├── progress/        # "Progreso" (constancia, racha máxima, matriz de Eisenhower)
├── calendar/         # Calendario Día/Semana/Mes
├── lists/            # Listas reutilizables ("Lista de viaje")
├── notes/            # Notas (Zona Privada)
├── diary/            # Diario (Zona Privada)
├── proposito/        # Mi propósito (Misión/Visión/Propósito)
├── care_circle/       # Círculo de cuidado (fase 1.0, parcial — ver `10-pendientes.md`)
├── family/            # Espacio Familia, tareas del hogar, retos familiares (fase 1.5, parcial)
├── privacy/           # Gate de Zona Privada (biometría/PIN)
├── legal/             # Textos legales (Política de Privacidad, Términos)
├── profile/, settings/, history/, reminders/, common/
```

**No existen todavía** (fases futuras, carpetas no creadas): `onboarding/`, `assistant/`,
`statistics/`, `core/voice/`, `data/remote/`. El usuario semilla local sigue siendo la única
"autenticación" — no hay pantallas de login reales todavía (ver sección de sincronización).

## Modelo de datos completo

### USUARIO

```kotlin
Usuario(
    id, nombreCompleto, nombrePreferido, correo?, metodoLogin: GOOGLE|CORREO_MAGICO|LOCAL,
    privacidadAceptadaEn?,
    modoDefectoAsistente?,                       // reservado para Fase 2.0, sin usar todavía
    horaDesayuno?, horaAlmuerzo?, horaCena?,      // "HH:mm" — medicamentos "según las comidas"
    confirmoMayorDe13: Boolean = false,           // checkbox, no fecha de nacimiento exacta
    terminosAceptadosEn?,
    consentimientoDatosSaludEn?,                  // aparte de privacidadAceptadaEn — categoría sensible (Play Store)
)
```

Hoy siempre hay un único `Usuario` semilla local (`metodoLogin = LOCAL`), creado por
`AsegurarDatosSemillaUseCase` en el primer arranque. Login real (Google/correo mágico) está
diseñado pero no construido — ver `12-firebase-auth-y-sync.md`.

### ESPACIO

```kotlin
Espacio(id, tipo: PERSONAL|FAMILIA|EQUIPO, nombre, creadoPor, fechaCreacion)
EspacioMiembro(espacioId, usuarioId, rol: ADMIN|MIEMBRO)
AreaDeVida(id, nombre, activa: Boolean = true, esPredefinida: Boolean = true)
```

Todo usuario tiene un espacio `PERSONAL` desde el primer arranque. Un espacio `FAMILIA` se
puede crear desde la app (fase 1.5, parcial — ver `10-pendientes.md`: hoy solo tiene sentido
con un miembro real, invitar a una segunda persona depende de backend). `EQUIPO` está en el
enum pero sin ninguna pantalla que lo use todavía.

### ACTIVIDAD — entidad genérica central

```kotlin
Actividad(
    id, tipo: TipoActividad, espacioId, nombre, propietario,
    responsables: List<String>, puedeVer: List<String>, puedeRecordar: List<String>,
    estado: CONFIRMADO|SIN_CONFIRMAR|OMITIDO,
    privacidad: SOLO_YO|COMPARTIDO|FAMILIA|GRUPO,
    syncStatus: LOCAL|PENDIENTE|SINCRONIZADO,
    esPremiumFeature: Boolean,
    areaDeVidaId?, momentoDelDia?,                 // denormalizados — evita join en la consulta de Hoy
    fechaCreacion, activa: Boolean,
    fechaCompletado?,                              // solo Tarea puntual: cuándo pasó a CONFIRMADO
    detalle: ActividadDetalle?,
)

enum TipoActividad { HABITO, TAREA, RUTINA, MEDICAMENTO, CITA, FECHA_IMPORTANTE }
```

**`evento` no existe como tipo** (estaba en el diseño original, nunca se construyó — lo más
cercano es Cita/Fecha importante). `ActividadDetalle` es una interfaz sellada de Kotlin, un
subtipo por `TipoActividad`:

```kotlin
// HABITO
Habito(
    momentoDelDia, frecuencia: DIARIA|DIAS_ESPECIFICOS, diasEspecificos: List<Int>,
    duracionInicialMin?, duracionObjetivoMin?, incrementoMin?, frecuenciaRevisionDias?,
    horaRecordatorio?, nivelRecordatorio,
    duracionActualMin?, proximaRevisionEpochDay?,  // progresión: "¿aumentamos?"
)
// esProgresivo = true solo si los 4 campos de progresión están completos

// TAREA
Tarea(
    fechaLimite?, prioridad?, importante: Boolean, urgente: Boolean,
    horaRecordatorio?, nivelRecordatorio,
    recurrencia: RecurrenciaTarea,                 // SIN_REPETIR|DIARIA|SEMANAL|QUINCENAL|MENSUAL|BIMESTRAL|TRIMESTRAL|ANUAL
    actividadVinculadaId?,                          // acompaña a un Medicamento/Cita ("cuidar a alguien por un tiempo")
)

// RUTINA
Rutina(actividadesIncluidasIds: List<String>, momentoDelDia)

// MEDICAMENTO
Medicamento(
    nombreMedicamento, dosis,
    modoFrecuencia: INTERVALO_HORAS|RELACION_COMIDA,
    intervaloHoras?, horaPrimeraDosis?,             // si INTERVALO_HORAS
    comidasRelacionadas: List<ComidaRelacionada>,   // si RELACION_COMIDA — { comida, momento: ANTES|DESPUES }
    horariosCalculados: List<String>,               // "HH:mm", editables individualmente
    fechaInicio, fechaFin?,
    cantidadDosisTotal?,                            // alternativa a fechaFin — recorta la última dosis del día final
    nivelRecordatorio,
    recordatorioPersistente: Boolean = false,       // "insiste" cada N min hasta marcar o fin del día
    intervaloPersistenciaMin?,
)
// El estado por toma vive aparte, en TomaMedicamento (ver abajo) — una Actividad Medicamento
// puede tener varias tomas por día, así que NO usa Actividad.estado para eso.

// CITA
Cita(
    lugar?, motivo?, fechaHora,                     // fechaHora se ignora si esCurso = true
    recordatorios: List<RecordatorioCita>,          // { anticipacion, hora } — varios posibles, cada uno a su propia hora
    nivelRecordatorio,
    esCurso: Boolean = false,                       // "curso" = varias sesiones (radioterapia, masajes...)
    diasSemana: Set<Int>,                           // ISO 1=lunes..7=domingo, patrón vigente del curso
    horaSesion?, fechaInicioCurso?,
    cantidadSesionesTotal?,                         // null = curso abierto, sin cantidad fija
)
// El estado de un curso vive en SesionCita (una fila por ocurrencia), no en Actividad.estado.

// FECHA_IMPORTANTE
FechaImportante(
    recurrencia: UNICA|SEMANAL|ANUAL, fechaBase, horaNotificacion,
    anticipacion: MISMO_DIA|UN_DIA_ANTES|UNA_SEMANA_ANTES,
    tipoAviso: ALARMA_SONORA|MENSAJE_SILENCIOSO,
)
```

**Entidades satélite de Actividad** (una fila por ocurrencia puntual, no un campo dentro de
`ActividadDetalle` — porque hay varias por día o por curso):

```kotlin
// Una toma de Medicamento — "uno por horario, no uno por día"
TomaMedicamento(id, actividadId, fecha: Long /* epoch day */, horario, estado)

// Una sesión de una Cita de curso — ej. sesión 7 de 20 de radioterapia.
// numeroSesion es fijo (el orden mostrado) aunque `fecha` cambie por reprogramación puntual;
// fechaOriginal guarda la fecha que le tocaba según el patrón, solo como referencia.
SesionCita(id, actividadId, numeroSesion, fecha: Long /* epoch day */, fechaOriginal, horario, estado)

// Estado de un Hábito en un día puntual — usado por el Calendario sobre un rango de fechas
EstadoActividadEnFecha(actividadId, fecha: Long /* epoch day */, estado)
```

**Regla de estados**: nunca asumir "no marcado" = "no lo hizo". Los 3 estados
(`CONFIRMADO`, `SIN_CONFIRMAR`, `OMITIDO`) existen justamente para eso — crítico en
medicamentos, citas de curso, y en el círculo de cuidado.

**Reprogramar una sesión de curso solo mueve esa sesión** — no cascada al resto del programa
ni al conteo total. Cambiar el patrón de días de un curso (ej. masaje de 3x/semana a 1x/semana)
solo afecta a las sesiones que se generan de ahí en adelante; las ya generadas no se tocan. Ver
`08-decisiones-tecnicas.md`, 2026-08-06.

### ÁREA_DE_VIDA

Ver `Espacio` arriba (`AreaDeVida`). Cada `Actividad` y `Meta` puede vincularse opcionalmente
a un área. No es un módulo de captura propio — es una etiqueta para calcular progreso agregado
por área.

### META

```kotlin
Meta(
    id, espacioId, nombre, areaDeVidaId?, fechaLimite?,
    comoSeMide: POR_HABITO|POR_MONTO|POR_NUMERO|MANUAL,
    valorObjetivo,                                  // POR_HABITO: días objetivo (ventana móvil)
    valorActual,                                    // ignorado en POR_HABITO, se calcula en vivo
    actividadesVinculadasIds: List<String>,
    ultimoHitoCelebrado: Int = 0,                    // 0/25/50/75/100 — evita repetir la tarjeta de celebración
)
```

`POR_MONTO` se calcula solo sumando `Finanzas` con categoría "Ahorro" — no se carga a mano.

### FINANZAS

```kotlin
MovimientoFinanciero(id, espacioId, tipo: INGRESO|EGRESO, monto, categoria, descripcion?, fecha, privacidad)
```

Nace con `privacidad: SOLO_YO`, vive dentro de Zona Privada. `fecha` es editable (registrar un
gasto de un día anterior), no siempre "ahora".

### ENTRADA_DIARIO

```kotlin
EntradaDiario(id, espacioId, propietario, titulo?, texto, areaDeVidaId?, fecha, privacidad, fotos: List<String> = emptyList())
```

`titulo` y `areaDeVidaId` siguen en el modelo pero **ya no se piden ni se muestran en la UI**
(el usuario los sacó del formulario — "tiene un título pero es un diario", ver
`08-decisiones-tecnicas.md`, 2026-07-30) — Diario hoy es solo fecha + texto libre. `fotos[]`
está **descartado a propósito**, no solo sin construir — se intentó y se revirtió el mismo día
(ver `10-pendientes.md`).

### REGISTRO_DIARIO (cierre del día) / REGISTRO_SEMANAL

```kotlin
RegistroDiario(id, espacioId, fecha, actividadesCompletadas, actividadesTotales, puntuacion,
    estadoAnimo?, queLogre?, queCosto?, queAjusto?)

RegistroSemanal(id, espacioId, semana, cumplimientoGeneralPorcentaje, rachaMaxima,
    queLogre?, queNoFunciono?, queAjusto?)
```

### RETO_FAMILIAR

```kotlin
RetoFamiliar(id, espacioId, nombre, objetivo, frecuencia: DIARIA|SEMANAL,
    participantesIds: List<String>, recompensa?)
```

### SOLICITUD_COMPARTIR / CONEXION

```kotlin
SolicitudCompartir(id, de, para, tieneCuenta: Boolean, elementoId, contexto,
    permisos: PUEDE_VER|PUEDE_VER_Y_RECORDAR, estado: PENDIENTE|ACEPTADA|RECHAZADA|ESPERANDO_INSTALACION,
    canalEnvio: CORREO|WHATSAPP|SMS|null, fechaSolicitud, fechaRespuesta?)

// Relación persistente entre dos personas, creada al aceptar una SolicitudCompartir
// (bloqueada por backend — el modelo existe, nada la crea todavía de verdad)
Conexion(id, usuarioA, usuarioB, tipo: FAMILIA|AMIGO|CUIDADOR, origenSolicitudId?, fechaConexion)
```

### LISTA (agregada, no estaba en el diseño original)

Plantilla reutilizable de ítems de texto libre (ej. "Lista de viaje") que se "reinicia"
(desmarca) para la próxima vez, sin duplicar la plantilla — a diferencia de `Rutina`, sus
ítems son texto libre, no Hábitos/Tareas existentes.

```kotlin
Lista(id, nombre)
ListaItem(id, listaId, texto, marcado: Boolean, orden)
ListaEjecucion(id, listaId, fecha, items: List<ItemListaSnapshot>)  // foto de un uso pasado, para su historial
```

### NOTA (agregada)

```kotlin
Nota(id, espacioId, propietario, titulo?, contenido, fechaCreacion, fechaEdicion, orden)
```

Vive en Zona Privada. `orden` es manual (flechas ▲▼), no por fecha.

### PROPOSITO_PERSONAL (agregada)

```kotlin
PropositoPersonal(espacioId, propietario, respuestas: Map<String, String>, fechaEdicion)
```

Respuestas a 8 preguntas guiadas (`PREGUNTAS_PROPOSITO`, en `domain/model/PropositoPersonal.kt`)
para armar Misión/Visión/Propósito de a poco, siempre del espacio Personal. No sintetiza un
párrafo narrativo todavía — el botón "🤖 Armar y presentar con IA" existe pero está
deshabilitado ("próximamente"), sin ningún llamado de red construido.

### HISTORIAL_CAMBIOS (auditoría)

```kotlin
HistorialCambios(id, entidad, entidadId, accion: CREAR|ACTUALIZAR|ELIMINAR,
    valoresAntesJson?, valoresDespuesJson?, usuarioId, timestamp, origen: LOCAL|SYNC)
```

Escrita **dentro de cada método de escritura de los repositorios** (no un caso de uso aparte),
para que sea imposible de omitir al agregar un flujo nuevo — lección de MayiaApp, donde esta
tabla quedó diseñada y nunca conectada.

### AJUSTES (DataStore, no Room)

No es una entidad con id — son preferencias sueltas (`AjustesRepository`/`DataStore`):
sonido al marcar un check, día de Revisión semanal, hora del recordatorio diario "cerrar mi
día" (null = apagado), qué va en cada posición configurable de la barra inferior, id del
Espacio activo (este último a propósito NO persiste entre aperturas de la app — ver
`08-decisiones-tecnicas.md`, 2026-07-30).

## Actividad propia vs. de apoyo vs. compartida

No es un campo nuevo — es una forma de leer los campos `propietario` y `responsables[]`:

```
Actividad "para mí"        → propietario = usuario actual, responsables = [usuario actual]
Actividad "de apoyo"       → propietario = otra persona, responsables incluye al usuario actual
Actividad "compartida"     → responsables[] tiene más de una persona
```

**Estado de implementación**: el modelo soporta esto desde el día 1, pero la distinción visual
en Hoy (ícono/nombre distinto según el caso) no está construida — hoy solo tiene sentido con
una segunda persona real (bloqueado por backend, ver `10-pendientes.md`).

## Ícono / mascota de estado (diseñado, no implementado)

Indicador emocional del estado del día (🙂 normal / ✅ completado / ⏳ pendiente / 💤 días sin
abrir), calculado en tiempo real a partir de `RegistroDiario`, tono siempre positivo. Sigue sin
construirse en ninguna pantalla (ver `09-guia-visual.md`, "pendiente para ir sumando").

## Fórmulas clave

```
Racha = días consecutivos con "Cerrar mi día" hecho Y actividadesCompletadas > 0,
        contando hacia atrás desde HOY (calcularRachaActual)
Constancia = (días activos en los últimos 30) / 30 × 100
Puntuación del día = suma de actividades cumplidas (1 punto cada una, sin máximo fijo)
```

Racha y Constancia son métricas independientes: romper la racha no resetea la constancia.

**Importante para no confundir al usuario**: la racha cuenta hacia atrás **desde hoy**, así
que si hoy todavía no se cerró el día, la racha se muestra en 0 aunque el historial real sea
largo — "se recupera" recién al cerrar. No es un bug, es la definición. Ver
`08-decisiones-tecnicas.md`, 2026-08-07, sobre el recordatorio diario configurable que se
construyó para que esto no se sienta como un castigo por olvido.

### Evolución futura de la puntuación (no implementado)

Sigue siendo 1 punto fijo por actividad en el MVP. Pesos distintos por tipo de actividad quedan
documentados como posible evolución futura, nunca una escala que penalice.

## Sincronización — decisión actualizada (Firebase, no Sheets/n8n)

El diseño original de este documento planteaba Google Sheets vía n8n como backend inicial.
**Esa decisión cambió** (2026-08-01, ver `12-firebase-auth-y-sync.md`): el backend real va a
ser **Firebase (Auth + Firestore)**, mismo ecosistema que la autenticación. Sheets/n8n no se
va a usar para el modelo de datos de Lula.

Estado actual: **sin ningún cliente de red construido todavía** — `data/remote/` no existe,
`SyncStatus` vive en el modelo pero nada lo mueve de `LOCAL`. El diseño completo (qué sube a
Firestore, qué se queda 100% local, modelo de colecciones, reglas de seguridad, orden de
implementación) ya está escrito en `12-firebase-auth-y-sync.md`, bloqueado únicamente porque
falta que el usuario cree el proyecto en Firebase Console y comparta `google-services.json`
(paso que Claude no puede hacer).

```
Dato se crea/edita
   ↓
Se guarda SIEMPRE primero en local (Room) — esto no cambia con Firebase
   ↓
Espacio PERSONAL → nunca toca la nube (mejor privacidad, no solo menos trabajo)
Espacio FAMILIA / Conexion / SolicitudCompartir → se replica en Firestore
   (recién tiene sentido con una segunda persona real, ver `12-firebase-auth-y-sync.md`)
```

Datos sensibles (finanzas, diario, medicamentos, notas privadas) están en espacios `PERSONAL`
por defecto, así que con el diseño actual **ni siquiera suben a la nube** — el cifrado antes de
subir mencionado en el diseño original solo aplicaría si en el futuro alguien decide compartir
contenido sensible desde un espacio Familia, caso todavía no diseñado en detalle.

## Privacidad — regla transversal

Cualquier `Actividad`, `MovimientoFinanciero` o `EntradaDiario` puede tener:

```
privacidad: SOLO_YO | COMPARTIDO[persona_específica] | FAMILIA | GRUPO
```

Compartir siempre es **solicitud + aceptación**, nunca automático — pertenecer a un espacio
`FAMILIA` no da acceso automático a todo lo del espacio personal de cada miembro.

### Invitación a quien no tiene la app (diseñado, bloqueado por backend)

```
SolicitudCompartir con tieneCuenta = false
   ↓
Se envía invitación personalizada (WhatsApp/correo/SMS) mencionando qué se comparte
   ↓
Deep link: si ya instaló → abre la solicitud directo
           si no → dirige a la tienda, recuerda la invitación tras instalar (deferred deep linking)
```

Nada de esto funciona de verdad todavía — depende de Firestore (`12-firebase-auth-y-sync.md`).
Hoy generar el QR/enlace de invitación existe en la UI, pero el otro lado nunca se entera de
la aceptación sin un servidor. Ver `10-pendientes.md` para el detalle completo de qué está
bloqueado y por qué.
