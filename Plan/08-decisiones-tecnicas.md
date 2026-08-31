# Decisiones técnicas — Base de Fase 0.1

Este documento registra decisiones de implementación tomadas al construir el núcleo técnico
de Lula, que completan (no contradicen) `01-arquitectura.md`. Se documentan aquí para no
repetir el error de MayiaApp: decisiones tomadas de forma implícita y nunca registradas.

## Lecciones de MayiaApp aplicadas desde el diseño

Ver `D:\2025\MayiaApp` (proyecto anterior, mismo stack). Aplicado desde el día 1, no como
tarea pendiente:

1. **Una sola fuente de verdad**: Room es la única fuente de verdad. El `UiState` de cada
   pantalla se deriva siempre vía `Flow` (`ActividadRepository.observarActividadesDeEspacio`,
   etc.) — nunca hay una cache manual en memoria en paralelo. Mayia tuvo bugs graves de
   datos desincronizados por mantener cache + BD + UI State por separado.
2. **IDs UUID** generados en el dispositivo (`IdGenerator.newId()`) para toda entidad, nunca
   autoincrement — la app es local-first y las filas se crean offline.
3. **Índices desde el día 1** en `ActividadEntity`: `espacioId`, `tipo`, `fechaCreacion`,
   `estado`, `momentoDelDia`, `areaDeVidaId`, `propietario` — los campos por los que Hoy y
   Progreso filtran. Mayia cargaba tablas completas en memoria y colapsó con 10k filas.
4. **`@Upsert` en vez de `OnConflictStrategy.REPLACE`** en todos los DAOs — Mayia usó
   `REPLACE` (delete+insert) en sync y rompió foreign keys de filas ya referenciadas.
5. **Auditoría desde el MVP**: `HistorialCambiosEntity` + `AuditLogger`
   (`data/repository/AuditLogger.kt`), invocado dentro de cada método de escritura de los
   `*RepositoryImpl` — nunca en un caso de uso aparte, para que sea imposible de omitir al
   agregar un flujo nuevo. En Mayia la tabla de auditoría quedó diseñada y nunca conectada.
6. **Sin RBAC multi-rol**: los campos ya definidos en `Actividad`
   (`propietario`, `responsables[]`, `puedeVer[]`, `puedeRecordar[]`, `privacidad`) alcanzan
   hasta fase 1.5. Mayia diseñó un RBAC de 8 tablas/64 permisos/7 roles que nunca se usó.
7. **3 estados de actividad** (`CONFIRMADO`/`SIN_CONFIRMAR`/`OMITIDO`) — nunca se interpreta
   "no marcado" como "no lo hizo".

## Convenciones de UI para toda pantalla nueva — añadido 2026-07-28

A partir de la tercera ronda de feedback en dispositivo real, estas reglas se aplican **desde
el diseño** de cualquier pantalla nueva, no se agregan después de que el usuario las pida de
nuevo:

1. **`verticalScroll`** en todo `Column` de formulario o detalle — cualquier pantalla con
   `DictationTextField` multilínea, selectores de chips, o un botón de acción al final. Las
   pantallas de lista usan `LazyColumn` (ya scrollea sola) y no lo necesitan.
2. **Todo `Checkbox` reproduce el sonido de check** (si está habilitado en Ajustes) y muestra
   el ítem tachado (`TextDecoration.LineThrough`) cuando está completado — usar
   `hiltViewModel<SonidoCheckViewModel>()` (`core/ui/SonidoCheckViewModel.kt`) en vez de
   duplicar la inyección de `AjustesRepository`.
3. **Ninguna fila con `Checkbox` lleva un `clickable` que envuelva toda la fila** — el check y
   cualquier navegación (ej. ir al detalle) van en elementos separados (el check solo,
   la navegación en el texto/otro elemento), nunca superpuestos.
4. **Toda eliminación pide confirmación** — `core/ui/ConfirmarEliminarDialog.kt`, nunca un
   botón "Eliminar" que borre directo al primer toque. Excepción: quitar una fila de un
   formulario *antes de guardarlo* (ej. un ítem de una lista que se está armando) no es una
   eliminación real todavía, no necesita confirmación.
5. **Toda pantalla "Nueva X"/"Editar X" enlaza a "Ver mis X"** (o la pantalla de resumen
   correspondiente, ej. Finanzas para Gasto/Ingreso) — un `TextButton` corto debajo del
   título, para no obligar a salir y volver a entrar solo para ver qué ya existe.
6. **Ningún método de un ViewModel que dependa de la sesión (`usuarioId`/`espacioId`) debe
   descartar la acción en silencio si `init` todavía no terminó de resolverla** — usar un
   helper `private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }`
   y llamarlo *dentro* de la corrutina de la acción, nunca un `val sesionActual = sesion ?: return`
   síncrono antes de lanzarla. Ver el bug real que causó esto abajo.

## Decisiones que completan `01-arquitectura.md`

1. **`di/` y `navigation/`** son paquetes nuevos, hermanos de `core/data/domain/features`
   (cross-cutting, no pertenecen a ninguna capa del modelo de datos).
2. **`Actividad` gana `areaDeVidaId`** (FK nullable, `SET_NULL` al borrar el área) en la
   tabla base — el modelo de datos ya asumía que toda actividad puede vincularse a un área.
3. **`momentoDelDia` se denormaliza** (nullable) en la tabla base `ActividadEntity`, escrito
   siempre por el mismo caso de uso que escribe el detalle de hábito/rutina
   (`CrearHabitoUseCase`) — evita join con las tablas de detalle en la consulta de Hoy, sin
   crear una segunda fuente de verdad (una sola ruta de escritura).
4. **Pantalla Hoy** suma una sección "Tareas de hoy": tareas sin fecha límite, o con fecha
   límite hoy o vencida (`HomeViewModel.esTareaDeHoyOVencida`).
5. **Modelo de `Actividad`: tabla base + tabla de detalle 1:1 por tipo**
   (`ActividadEntity` + `HabitoDetalleEntity`, `TareaDetalleEntity`, etc., FK = PK =
   `actividadId`, `onDelete = CASCADE`). Se evaluó tabla única con columnas nullable y se
   descartó: con 7 tipos y ~25 campos combinados, cualquier fila tendría ~20 columnas nulas
   de otros tipos. Campos que se filtran/muestran seguido (fecha límite, dosis, fecha/hora)
   son columnas tipadas reales, nunca JSON. Solo las listas que siempre se leen como bloque
   completo (`horariosCalculados`, `comidasRelacionadas`) se guardan como JSON vía
   `kotlinx.serialization`.
6. **`responsables[]`, `puedeVer[]`, `puedeRecordar[]`** quedan como columnas JSON en
   `ActividadEntity` (un solo usuario local esta fase, no se filtran en SQL). Se migran a
   tablas de unión reales en fase 1.0 (círculo de cuidado). `MetaActividadCrossRef` y
   `EspacioMiembroEntity` sí son tablas de unión desde ya, porque sí se usan para calcular
   pertenencia/progreso vía consulta.

## Cumplimiento diario de hábitos (`RegistroActividadEntity`) — añadido 2026-07-25

Al construir las pantallas de lista/detalle de Hábitos (tracker semanal, racha por hábito,
historial de 30 días) apareció un vacío del modelo original: `ActividadEntity.estado` es un
solo valor "actual" por fila — no alcanza para saber si un hábito se cumplió hace 3 días,
porque un hábito es recurrente y "hoy" cambia todos los días.

Solución: `RegistroActividadEntity` (id, actividadId, fecha [epoch day], estado), FK
`actividadId → Actividad` CASCADE, índice único `(actividadId, fecha)`. Reglas:

- **HABITO** (recurrente): el cumplimiento se guarda por día en `registro_actividad`.
  `ActividadRepositoryImpl.marcarEstado` detecta el tipo y escribe ahí, no en
  `ActividadEntity.estado`. El "estado de hoy" que ve la pantalla Hoy se resuelve leyendo el
  registro de la fecha actual (o `SIN_CONFIRMAR` si no existe todavía).
- **TAREA** (no recurrente): sigue usando `ActividadEntity.estado` directamente, como en el
  diseño original — una tarea no se "repite" cada día.
- Racha por hábito y tracker semanal/historial de 30 días se calculan leyendo un rango de
  `registro_actividad` (`ObtenerHistorialHabitoUseCase`), sin tocar la racha global de la app
  (`REGISTRO_DIARIO`, que sigue siendo independiente).

Este cambio de esquema (más el campo `activa: Boolean` en `ActividadEntity` para
pausar/reanudar sin perder historial) subió `LulaDatabase` a `version = 2`. Como el proyecto
todavía no tiene usuarios reales, se usa
`Room.databaseBuilder(...).fallbackToDestructiveMigration(dropAllTables = true)` en
`DatabaseModule` en vez de escribir una migración — borra los datos locales del dispositivo
de prueba en el próximo arranque tras cada cambio de esquema durante desarrollo activo. Se
debe reemplazar por migraciones reales (`Migration`) antes de cualquier release con datos de
usuarios que no se puedan perder.

## Usuario semilla local (sin Firebase todavía)

- `AuthRepository` (interfaz de dominio) no cambia cuando se conecte Firebase Auth — solo su
  implementación. `AuthRepositoryLocalImpl` siempre devuelve el único `UsuarioEntity`
  existente y no conoce Firebase.
- `AsegurarDatosSemillaUseCase` es idempotente: si `UsuarioDao` está vacío, crea el usuario
  local + su espacio personal + las 7 áreas de vida predefinidas en el primer arranque.
- El UUID generado en la semilla se convierte en el id permanente del usuario en toda la
  base (`propietario`, `creadoPor`, etc.). Cuando se conecte Firebase, la implementación real
  de `AuthRepository` **actualiza** esa misma fila (llena correo, cambia `metodoLogin`) en
  vez de crear una nueva — ningún FK necesita reescribirse.
- `ObtenerSesionActualUseCase` es el único punto donde los ViewModels de features resuelven
  `usuarioId`/`espacioId` — evita repetir esa lógica en cada pantalla.

## Dictado de campo (`DictationTextField`) — añadido 2026-07-26

`core/ui/DictationTextField.kt` delega la captura de voz al reconocedor del sistema vía
`Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)` + `rememberLauncherForActivityResult` — la
app **no pide permiso de micrófono propio**, lo maneja la app de reconocimiento de voz
(normalmente Google). Es "dictado de campo": transcribe texto, no interpreta intención (esa
es tarea del asistente conversacional de fase 2.0, ver `03-vocabulario.md`). Aplicado en todo
campo de texto libre existente hasta ahora: nombre de hábito/tarea, categoría/descripción de
movimiento, las 3 reflexiones de Cerrar mi día.

## Zona Privada (PIN + biometría) — añadido 2026-07-26

- **`MainActivity` pasó de `ComponentActivity` a `FragmentActivity`** — requisito de
  `BiometricPrompt` (androidx.biometric). Es un supertipo compatible, no rompe nada de Compose.
- **PIN**: se guarda solo su hash SHA-256 (`PrivacidadRepositoryImpl`, DataStore
  Preferences) — nunca el PIN en texto plano. Es un candado de UI local, no cifrado real de
  los datos; cuando haya sync, los datos sensibles se cifran aparte antes de subir (regla ya
  definida en `01-arquitectura.md`).
- **Biometría** vía `BiometricPrompt` (`core/security/BiometricAuthenticator.kt`) se ofrece
  primero si el dispositivo la soporta; "Usar PIN en su lugar" es el fallback siempre
  disponible, igual que especifica `02-pantallas.md`.
- **Estado de desbloqueo en memoria** (`SesionPrivadaState`, singleton Hilt): se re-bloquea
  siempre al reabrir la app. El auto-bloqueo por inactividad de 2-5 min de `02-pantallas.md`
  queda pendiente — requiere un temporizador de actividad del usuario, se documenta como
  siguiente paso, no se improvisó a medias.
- **Alcance de esta pasada**: solo la sección **Finanzas** queda detrás del gate (único
  módulo con privacidad `solo_yo` que ya tiene pantallas reales). Diario y Notas privadas se
  gatearán del mismo modo cuando se construyan sus pantallas.

## Notificaciones / recordatorios — añadido 2026-07-27

Campo nuevo `horaRecordatorio: String?` (formato "HH:mm") en `HabitoDetalle` y `TareaDetalle`
(en tareas solo tiene efecto si además hay `fechaLimite`) — subió `LulaDatabase` a
`version = 3` (destructiva, como en el cambio anterior).

- **`core/notifications/RecordatorioScheduler`** programa con
  `AlarmManager.setExactAndAllowWhileIdle`; si el sistema no permite alarmas exactas
  (`canScheduleExactAlarms() == false`, posible desde Android 12), cae a `set()` inexacto en
  vez de fallar — un recordatorio con minutos de diferencia es mejor que ninguno. No se
  agregó el flujo de pedir el permiso especial vía Ajustes (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`)
  todavía — queda como mejora.
- **Hábitos (recurrentes) se reprograman a sí mismos**: `RecordatorioReceiver` muestra la
  notificación y, si es de un hábito, vuelve a llamar a `programarHabito` para el día
  siguiente — evita el drift de `setRepeating()` (inexacto en Android moderno) sin depender
  de que la app esté abierta.
- **`BootReceiver` reprograma todo tras reiniciar el teléfono** — `AlarmManager` borra todas
  las alarmas pendientes en cada reinicio; sin este receiver, los recordatorios dejarían de
  sonar en silencio y nadie lo notaría hasta extrañar uno. Es `@AndroidEntryPoint` (Hilt
  soporta inyección directa en `BroadcastReceiver`), usa `goAsync()` porque la relectura de
  Room es asíncrona.
- **Permiso `POST_NOTIFICATIONS`** (obligatorio desde Android 13) se pide una vez al abrir la
  app por primera vez desde `MainActivity`, no bloqueante — si el usuario lo niega, los
  recordatorios simplemente no se ven (no hay reintento automático todavía).
- **Selector de hora**: en la primera pasada eran chips con horas fijas; se reemplazó por un
  `TimePicker` real (ver más abajo, sección del 2026-07-27).

## Bug de actualización en vivo + selectores reales — añadido 2026-07-27

**Bug encontrado por el usuario**: en Hoy, marcar/desmarcar el checkbox de un **hábito** no
se veía reflejado hasta salir de la pantalla y volver a entrar (en Tareas sí se veía al
instante). Causa raíz: `observarActividadesDeEspacio` es un `Flow` de Room atado únicamente a
la tabla `actividad` — Room solo vuelve a emitir cuando esa tabla cambia. Marcar un hábito
escribe en `registro_actividad` (la tabla de tracking diario agregada para el tracker
semanal), una tabla que esa consulta nunca menciona, así que Room no sabía que debía
reemitir. Las tareas no tenían el problema porque su estado vive directamente en `actividad`.

**Fix**: `RegistroActividadDao.observarPorFecha(fecha)` (nuevo `Flow` atado a
`registro_actividad`) se combina con `actividadDao.observarActivasDeEspacio` vía
`kotlinx.coroutines.flow.combine` en `ActividadRepositoryImpl.observarActividadesDeEspacio` y
`observarHabitos` — el valor de ese segundo Flow no se usa directamente, solo sirve de
"señal" para forzar una re-consulta completa (`resolverConDetalle`) cada vez que
`registro_actividad` cambia. **Lección para el futuro**: cuando un Flow de Room necesite
reaccionar a una tabla que su propio `@Query` no menciona, `combine()` con un Flow atado a
esa otra tabla — Room no detecta dependencias implícitas entre tablas por sí solo.

**Selectores de fecha/hora reales**: reemplazo de los chips fijos por componentes de verdad,
a pedido del usuario:
- `core/ui/HoraRecordatorioSelector.kt`: chip "Sin recordatorio" + chip que abre un
  `TimePicker` de Material3 dentro de un `Dialog` propio (Material3 no trae un
  `TimePickerDialog` armado, a diferencia de `DatePickerDialog` que sí existe). Se usa en
  Crear/Editar Hábito y Crear/Editar Tarea.
- `CrearTareaScreen`: se agregó un cuarto chip "📅 Elegir fecha" que abre `DatePickerDialog` +
  `DatePicker` de Material3 — ahora se puede elegir cualquier fecha, no solo Hoy/Mañana. El
  modo edición también se corrigió: antes, una tarea con fecha distinta a "hoy" siempre se
  mostraba como "Mañana" (aproximación documentada); ahora se detecta y muestra la fecha real.
  Nota técnica: el `DatePicker` de Material3 trabaja en milisegundos UTC (medianoche UTC del
  día elegido), no en la zona horaria local — `DateTimeUtils.utcMillisAInicioDeDiaLocal` /
  `inicioDeDiaLocalAUtcMillis` hacen la conversión en los dos sentidos.
- `core/ui/SectionLinkRow.kt`: reemplaza el texto plano "Ver todas las tareas →" por una fila
  con ícono a color (mismo lenguaje visual del bottom nav/menú `+`) — más vistoso y
  diferenciado, a pedido del usuario.

## Categorías de Finanzas + Ahorro destacado — añadido 2026-07-27

`Plan/02-pantallas.md` ya decía "Categoría: [selector]" para Finanzas, pero se había
implementado como texto libre sin sugerencias (el usuario no sabía qué escribir). Se agregó
`features/finances/CategoriasFinanzas.kt` con listas fijas:

- Gasto: Comida, Transporte, Vivienda, Servicios, Salud, Entretenimiento, Educación, Ropa,
  Mascotas, Ahorro, Otros.
- Ingreso: Sueldo, Extra/Freelance, Regalo, Otros.

`CrearMovimientoScreen` las muestra como chips (según Gasto/Ingreso elegido); "Otros" revela
un campo de texto con dictado para escribir una categoría propia.

**Encaje automático de categorías dictadas**: si el texto libre (escrito o dictado) coincide
con una categoría ya conocida ignorando mayúsculas, tildes y plural simple ("comida" =
"Comida" = "comidas"), se guarda la categoría existente en vez de crear una variante suelta —
`CategoriasFinanzas.encajarEnConocida()`. Evita que la lista de categorías se ensucie con
duplicados casi idénticos por decir lo mismo de formas distintas al dictar.

**"Ahorro" no es una entidad nueva**: es una categoría de gasto más (el dinero apartado sale
igual del balance de "lo que gastaste en consumo", aunque siga siendo del usuario). Se
decidió así para no complicar el modelo de datos con una noción de "cuentas" separadas que no
existe todavía. Si más adelante se necesita una meta de ahorro con progreso, ya existe la
entidad `Meta` (`cómo_se_mide: por_monto`) para eso — Fase 0.5, no esta sesión.

**Ahorro destacado en Finanzas**: `FinancesScreen` suma un `StatPill` propio ("🐷 Ahorraste
S/X este mes", color rosado de celebración) cuando el ahorro del mes es mayor a 0 — separado
de la fila de Ingresos/Gastos/Balance, a propósito, para que se sienta como un logro y no como
un gasto más aunque contablemente lo sea.

**Bottom nav más grande**: los círculos de ícono pasaron de 36dp a 44dp (más cerca del
tamaño de toque accesible recomendado, 48dp) — a pedido del usuario, que los notaba chicos.

## Metas (Fase 0.5) — añadido 2026-07-27

Primera pieza de Fase 0.5. `MetaEntity` (creada en la sesión base con CRUD mínimo) ganó dos
campos: `valorObjetivo` y `valorActual` — sin ellos no había forma de calcular
"{progreso} de {objetivo}" como pide `02-pantallas.md`. Subió `LulaDatabase` a `version = 4`
(destructiva, mismo patrón que los cambios de esquema anteriores).

**Cómo se calcula el progreso según `comoSeMide`**:
- `POR_HABITO`: no se guarda — se calcula en vivo contando cuántos de los últimos
  `valorObjetivo` días el hábito vinculado quedó `CONFIRMADO`, reusando
  `ObtenerHistorialHabitoUseCase` que ya existía para el tracker de Hábitos. Evita duplicar
  lógica de historial (una sola fuente de verdad, lección de Mayia aplicada de nuevo aquí).
- `POR_MONTO` / `POR_NUMERO` / `MANUAL`: `valorActual` se actualiza a mano con un botón
  "+ Agregar progreso" en el detalle — no hay vínculo automático con Finanzas todavía (por
  ejemplo, una meta "por monto" no se auto-completa sumando movimientos de categoría
  "Ahorro"; el usuario suma el progreso él mismo). Documentado como posible mejora futura,
  no construido a medias esta sesión.

**Fuera de alcance de esta pasada de Metas**: área de vida, fecha límite, y **editar** una
meta ya creada (solo hay crear/ver/agregar progreso/eliminar) — el formulario de creación ya
reúne todos los campos necesarios; falta la pantalla de edición reutilizándolo, análogo a como
se hizo con Hábito/Tarea/Movimiento.

## Notificación no abría la app al tocarla + 3 niveles de intensidad — añadido 2026-07-27

**Bug encontrado por el usuario**: al sonar un recordatorio de Hábito o Tarea y tocar la
notificación, no pasaba nada — no abría Lula. Causa raíz: `RecordatorioReceiver.mostrarNotificacion`
nunca llamaba a `.setContentIntent(...)`, así que la notificación no tenía ninguna acción de
toque asociada. **Fix**: se arma un `PendingIntent` hacia `MainActivity` con una ruta destino
como extra (`LulaDestinations.habitoDetalle(id)` / `tareaDetalle(id)`). `MainActivity` guarda
esa ruta en `destinoInicial` (`mutableStateOf`, leído en `onCreate` y también en
`onNewIntent` — necesario porque una Activity ya abierta no vuelve a pasar por `onCreate`) y
`LulaNavHost` navega a esa ruta en un `LaunchedEffect(destinoInicial)`, consumiéndola después
para no repetir la navegación en recomposiciones futuras.

**Pedido del usuario en el mismo mensaje**: poder elegir, por cada Hábito/Tarea con
recordatorio, qué tan insistente es el aviso — no todos los recordatorios merecen el mismo
nivel de urgencia. Se definieron 3 niveles (`domain/model/NivelRecordatorio`:
`SILENCIOSO`/`SONIDO`/`ALARMA`), elegidos por el usuario al configurar la hora del
recordatorio, nunca decididos por Lula. Subió `LulaDatabase` a `version = 5`.

**Restricción de Android que definió el diseño**: el sonido/importancia de un
`NotificationChannel` queda fijo apenas se crea por primera vez en el dispositivo — ni
siquiera la propia app puede cambiarlo después en código, solo el usuario desde Ajustes o
creando un canal con un ID nuevo. Por eso se crean **3 canales fijos** desde el arranque
(`recordatorios_silencioso` / `recordatorios_sonido` / `recordatorios_alarma`), no un canal
dinámico por recordatorio:
- 🔇 **Silencioso**: `IMPORTANCE_LOW`, `setSound(null, null)` — solo aparece en la lista de
  notificaciones, sin sonido ni vibración.
- 🔔 **Sonido** (default, igual que antes de este cambio): `IMPORTANCE_HIGH` + tono de
  notificación por defecto del sistema (`RingtoneManager.TYPE_NOTIFICATION`).
- ⏰ **Alarma**: `IMPORTANCE_HIGH` + vibración + tono de alarma
  (`RingtoneManager.TYPE_ALARM`, `AudioAttributes.USAGE_ALARM`) + `CATEGORY_ALARM` +
  `setFullScreenIntent(..., true)` (permiso `USE_FULL_SCREEN_INTENT` en el manifiesto) para
  que aparezca como pantalla completa/heads-up incluso con el teléfono bloqueado. **Alcance
  honesto**: es una notificación de un solo disparo más insistente, no una alarma tipo
  despertador con sonido en loop hasta apagarla manualmente — eso requeriría una `Activity`/
  `Service` de "sonando" dedicada, fuera de alcance de esta pasada, documentado como posible
  mejora futura si el usuario lo pide.
- `NotificationChannels.canalPara(nivel)` mapea el enum al ID de canal correspondiente;
  `RecordatorioReceiver` arma el `NotificationCompat.Builder` sobre ese canal y ajusta
  prioridad/categoría/full-screen-intent según el nivel.

**Personalizar el sonido desde la app**: como el sonido de un canal ya creado no se puede
tocar en código, se agregó un acceso directo "🔊 Sonido de mis recordatorios" en Hoy
(`core/utils/abrirAjustesDeNotificaciones`) que abre
`Settings.ACTION_APP_NOTIFICATION_SETTINGS` — desde ahí Android deja al usuario entrar a cada
uno de los 3 canales y elegir su propio sonido/tono. No se agregó (por ahora) un archivo mp3
personalizado empaquetado en la app — si el usuario provee uno, sí viaja con el APK a
cualquier teléfono y se puede referenciar como `raw resource` en vez del tono del sistema;
queda como mejora futura, no construida a medias esta sesión.

**Selector en UI**: `core/ui/NivelRecordatorioSelector.kt` (3 `FilterChip`, mismo patrón que
`HoraRecordatorioSelector`), visible en Crear/Editar Hábito y Crear/Editar Tarea únicamente
cuando ya hay una hora de recordatorio elegida (si no hay recordatorio, elegir su intensidad
no tiene sentido). El nivel se propaga por toda la cadena: entidad → mapper → los 5 casos de
uso que programan/reprograman alarmas (`CrearHabitoUseCase`, `ActualizarHabitoUseCase`,
`CrearTareaUseCase`, `ActualizarTareaUseCase`, `PausarReanudarActividadUseCase`) →
`RecordatorioScheduler` → `RecordatorioReceiver` → `BootReceiver` (para que sobreviva a un
reinicio del teléfono con el mismo nivel elegido).

## Rutinas (Fase 0.5) — añadido 2026-07-27

Segunda pieza de Fase 0.5. `RutinaDetalleEntity`/`ActividadDetalle.Rutina` ya existían como
stub desde la sesión base (creados junto con el resto del modelo completo para que la base de
datos compilara), sin repositorio, casos de uso ni pantallas conectadas — esta pasada los
conecta. No subió la versión de la base de datos (el esquema no cambió, solo el código que lo
usa).

**Qué es una Rutina**: una `Actividad` más (`tipo = RUTINA`) cuyo detalle es una lista de IDs
de Hábitos/Tareas ya existentes (`actividadesIncluidasIds`) más un `momentoDelDia`. No
duplica esos ítems — los agrupa por referencia. Eliminar una Rutina borra solo su fila de
agrupación (`rutina_detalle`, `CASCADE` desde `actividad`); los hábitos/tareas incluidos
siguen existiendo igual que antes.

**Cómo se calcula "completada"**: no se guarda un estado propio de la Rutina — se deriva en
vivo contando cuántos de sus `actividadesIncluidasIds` están `CONFIRMADO` ahora mismo
(`obtenerDetalleActividadUseCase` ya resuelve correctamente el estado "de hoy" para hábitos,
reutilizado tal cual). Mismo principio que el progreso "por hábito" de Metas: una sola fuente
de verdad, nunca un segundo estado que se pueda desincronizar del real.

**"Marcar rutina completa" es un atajo, no una entidad de estado nueva**: el botón en
`RoutineDetailScreen` simplemente llama a `MarcarActividadUseCase` una vez por cada ítem
incluido — no hay una tabla ni un flag que diga "esta rutina está completa", evitando el
mismo tipo de fuente de verdad duplicada que la lección de Mayia advierte.

**Fuera de la pantalla Hoy a propósito**: a diferencia de Hábitos/Tareas, las Rutinas no
aparecen agrupadas dentro de Hoy — tienen su propia lista/detalle accesible desde "🧩 Ver mis
rutinas" en Hoy y desde el menú `+`, mismo patrón que Metas. Insertarlas dentro de Hoy
mezclado con hábitos y tareas individuales habría requerido decidir cómo evitar duplicar la
visualización de un hábito que ya aparece suelto en Hoy Y dentro de su rutina — no se
resolvió a medias esta sesión, queda documentado como posible mejora si el usuario lo pide.

**Selector de actividades incluidas**: `CrearRutinaScreen` combina Hábitos + Tareas activos
del espacio en una sola lista de `FilterChip` de selección múltiple (a diferencia del selector
de un solo hábito que usa Metas) — el usuario arma la rutina tocando cada ítem que la compone.

## Revisión semanal (Fase 0.5) — añadido 2026-07-27

Tercera pieza de Fase 0.5. `RegistroSemanalEntity`/`RegistroSemanal` ya existían como stub
completo desde la sesión base; esta pasada conecta repositorio, casos de uso y pantalla.

**Clave de semana sin ambigüedad de año**: en vez de un número de semana ISO (`"2026-W30"`,
que tiene casos borde en el cambio de año), `semana` se guarda como la fecha ISO del lunes de
esa semana (ej. `"2026-07-20"`, vía `DateTimeUtils.claveSemana()`) — únivoca, ordenable como
texto, y sin necesidad de resolver a qué año pertenece una semana que cruza el 31 de
diciembre.

**Cálculo del día ISO sin `java.time.DayOfWeek`**: `LocalDate.dayOfWeek` de kotlinx-datetime
delega en `java.time.DayOfWeek`, que requiere API 26+ sin *core library desugaring* — y este
proyecto tiene `minSdk = 24` sin desugaring habilitado en `app/build.gradle.kts`. Se
verificó decompilando el `.jar` de `kotlinx-datetime-jvm` antes de usar la API (misma
disciplina que con los `DatePicker`/`TimePicker` de Material3). En vez de arriesgar un crash
en dispositivos API 24-25, `DateTimeUtils.numeroDiaIso()` calcula el día ISO (1=lunes..7=domingo)
por aritmética de módulo sobre epoch day (epoch day 0 = jueves 1970-01-01) — cero dependencias
nuevas de `java.time`. **Lección para el futuro**: revisar `minSdk` antes de agregar cualquier
API de fecha/hora nueva, no asumir que kotlinx-datetime evita `java.time` en Android.

**Cómo se calcula el resumen de la semana** (todo en vivo en `WeeklyReviewViewModel`, sin
casos de uso "orquestadores" — mismo patrón que `MetaDetailViewModel`/`GoalsListViewModel`,
que también combinan varios casos de uso de una sola responsabilidad directamente en el
ViewModel en vez de crear una capa intermedia):
- **Cumplimiento general**: `sum(actividadesCompletadas) * 100 / sum(actividadesTotales)` de
  los `RegistroDiario` (cierres de día) que caen dentro de la semana en curso (lunes a
  domingo). Si todavía no se cerró ningún día esta semana, es 0% — no se interpola.
- **Racha máxima de la semana**: la corrida consecutiva más larga de días con
  `actividadesCompletadas > 0` dentro de esa misma ventana lunes-domingo — distinta de la
  racha global de la app (`ObtenerProgresoDeHoyUseCase.calcularRachaActual`, que no se
  resetea cada semana).
- **"Lo que mejor funcionó" / "Lo que costó más"**: por cada Hábito activo, % de días
  `CONFIRMADO` en los últimos 7 días (reusa `ObtenerHistorialHabitoUseCase.ultimosDias`, la
  misma función que ya usan Metas y el tracker de Hábitos — otra vez, una sola fuente de
  verdad). Se muestra el de mayor y menor porcentaje; si solo hay un hábito, se omite "lo que
  costó más" para no duplicar la misma línea con otro rótulo.

**Sin gating al domingo, a propósito**: `02-pantallas.md` especifica que la Revisión semanal
"se activa el día configurado, por defecto domingo". Esta pasada la deja accesible en
cualquier momento desde "🗓️ Ver mi revisión semanal" en Hoy, mostrando siempre el resumen de
la semana en curso — permite revisar el progreso a mitad de semana también. Guardar la
revisión de una semana que ya se guardó antes actualiza el mismo registro (`@Upsert` sobre
`(espacioId, semana)` único), no crea duplicados. El gating automático al día configurado
queda como mejora futura si el usuario lo pide, no se construyó a medias esta sesión.

**Sin pantalla "Progreso" propia todavía**: `02-pantallas.md` describe una pantalla Progreso
más amplia (cumplimiento, racha máxima, Constancia, puntos, con un enlace a la Revisión
semanal completa) — no existe todavía (es la siguiente pieza pendiente de Fase 0.5, junto con
Constancia). Esta pasada solo construye la Revisión semanal en sí, accesible directo desde Hoy.

## Ronda de feedback de uso real — añadido 2026-07-27

El usuario probó la app en su dispositivo (no emulador) y reportó 6 problemas de una sola vez.
Se investigó cada uno con el dispositivo conectado por `adb` en vez de adivinar — el `logcat`
del buffer de crashes fue decisivo para el punto más grave.

**Bug crítico encontrado por `adb logcat -b crash`**: crear una Meta o guardar una Revisión
semanal crasheaba la app entera (`kotlinx.serialization.SerializationException: Serializer for
class 'MetaEntity'/'RegistroSemanalEntity' is not found`). Causa: `AuditLogger.registrar<T>`
serializa la entidad con `kotlinx.serialization.json.Json.encodeToString(it)` vía reified
generics — **toda** entidad pasada a `auditLogger.registrar<T>(...)` necesita `@Serializable`,
y `MetaEntity`/`RegistroSemanalEntity` no lo tenían (quedó sin la anotación desde que eran
stubs sin repositorio conectado). Se agregó `@Serializable` a ambas.
**Lección para el futuro, igual de importante que la de Hilt/`@Provides`**: antes de conectar
el repositorio de cualquier entidad nueva a `AuditLogger`, verificar que la entidad tenga
`@Serializable` — ni `compileDebugKotlin` ni `assembleDebug` detectan este error (es un fallo
en tiempo de ejecución, no de compilación), así que hay que revisarlo a mano o probar el flujo
en un dispositivo real. Se debería considerar un test unitario que recorra todas las entidades
usadas con `AuditLogger` y falle si a alguna le falta la anotación.

**Notificación mandaba a "editar" en vez de a una acción rápida**: `RecordatorioReceiver`
llevaba al formulario de Crear/Editar Hábito o Tarea al tocar la notificación — confuso, el
usuario esperaba una acción rápida, no un formulario. Se creó `features/reminders/` con
`RecordatorioAccionScreen`: pantalla centrada con emoji grande, nombre de la actividad, y 3
botones — "✅ Ya lo hice" (marca `CONFIRMADO`), "⏰ Recuérdame en 15 minutos" (pospone sin tocar
la hora fija diaria guardada) y "Ver en Hoy" (solo cierra). `RecordatorioScheduler.posponer()`
programa una alarma de un solo disparo a `ahora + minutos`, reutilizando el mismo
`PendingIntent` (mismo `requestCode` = `actividadId.hashCode()`) — al sonar, si es hábito, el
receiver se reprograma solo para su hora habitual del día siguiente, así que el snooze no
desconfigura el horario permanente. Se descartó agregar un cuarto estado "en proceso" a
`EstadoActividad` (el modelo de 3 estados es una regla no negociable de
`01-arquitectura.md`) — "empecé y termino después" se resuelve con el mismo botón de posponer,
sin tocar el modelo de datos.

**Notificación "seca" + pantalla oscura en el nivel Alarma**: se agregó emoji + texto más
humano al título/cuerpo de la notificación (`"✅ ¡Hora de tu hábito!"` en vez de solo
`"Lula"`). Para la pantalla oscura al sonar la Alarma con el teléfono bloqueado: el
`fullScreenIntent` abría `MainActivity` pero sin `setShowWhenLocked`/`setTurnScreenOn` (API
27+, con fallback a los flags de ventana `FLAG_SHOW_WHEN_LOCKED`/`FLAG_TURN_SCREEN_ON`/
`FLAG_DISMISS_KEYGUARD` en versiones más viejas), la Activity podía quedar detrás del lockscreen
sin mostrarse. Se agregó un extra `MainActivity.EXTRA_MOSTRAR_SOBRE_BLOQUEO`, puesto solo por
`RecordatorioReceiver` cuando el nivel es `ALARMA` — nunca al abrir la app normalmente, para no
saltarse el bloqueo de pantalla en un uso común.

**"Sonido de mis recordatorios" no debía estar en Hoy**: el usuario planeaba una sección de
Ajustes/configuración personal aparte. Se creó `features/settings/SettingsScreen.kt` (por
ahora solo con ese enlace) accesible desde un menú "⋮" nuevo en la esquina superior de Hoy
(`DropdownMenu` de Material3) — no ocupa espacio en el flujo diario.

**Enlaces a listas vacías en Hoy**: "Ver mis metas" y "Ver mis rutinas" aparecían siempre,
aunque el usuario no hubiera creado ninguna todavía — a pedido del usuario, ahora solo se
muestran si `hayMetas`/`hayRutinas` (booleanos nuevos en `HomeUiState`, calculados observando
`ObtenerMetasUseCase`/`ObtenerRutinasUseCase` igual que el resto de Hoy). Como esto los oculta
antes de que exista el primer ítem, **"Meta" se agregó al menú `+`** (antes solo se podía crear
una Meta entrando primero a una lista que, sin este cambio, hubiera sido invisible — un
problema de "huevo y gallina" que había que resolver junto con el ocultamiento). "Ver mi
revisión semanal" se dejó siempre visible a propósito: el usuario lo pidió explícitamente
porque es un ritual, no una lista de datos que pueda estar vacía.

**Confusión "Metas por hábito" y "Rutina vs Hábito"**: ambas eran de redacción, no de lógica.
Se agregó texto explicativo antes del selector de hábito en `CrearMetaScreen` (aclara que hay
que elegir un hábito **ya existente**, no escribir uno nuevo, y qué hacer si no existe
todavía) y un texto en `CrearRutinaScreen` que aclara que una Rutina no es un hábito nuevo,
sino un agrupador de hábitos/tareas ya existentes.

## Segunda ronda de feedback de uso real — añadido 2026-07-28

El usuario siguió probando en su dispositivo y reportó 8 puntos más. Resumen de lo corregido y
por qué:

1. **¿Los hábitos se repiten todos los días?** Sí — confirmado leyendo el código
   (`RecordatorioScheduler`/`RecordatorioReceiver` ya reprograman solos para el día siguiente,
   ver la sección de Notificaciones más arriba). No era un bug, era falta de feedback visible;
   se agregó una línea aclaratoria en `CrearHabitoScreen` ("Se repetirá todos los días a esta
   hora") para no dejarlo en duda.
2. **El sonido de Alarma seguía sonando después de tocar una acción**: `RecordatorioReceiver`
   arma la notificación con `setContentIntent`, pero el `fullScreenIntent` de nivel Alarma
   lanza la Activity directo, sin pasar por el flujo normal de "tocar la notificación" que
   dispara `setAutoCancel`. La notificación se quedaba viva (y sonando, según el dispositivo)
   aunque el usuario ya estuviera interactuando con la pantalla. Fix: `RecordatorioAccionViewModel`
   cancela la notificación (`NotificationManagerCompat.cancel(actividadId.hashCode())`) apenas
   se abre la pantalla, no al tocar un botón específico.
3. **Sonido de check al marcar en Hoy**: nuevo `AjustesRepository` (DataStore propio,
   `ajustes_prefs`) con `sonidoCheckHabilitado` (default `true`), editable desde un `Switch` en
   la nueva pantalla Ajustes. El sonido en sí usa `android.media.ToneGenerator` (tono del
   sistema, `TONE_PROP_BEEP2`) — sin bundlear ningún archivo de audio, una sola instancia
   reutilizada mientras la app viva (`core/utils/SonidoUtils.kt`).
4. **Dictado que a veces no arrancaba**: no se pudo reproducir en un dispositivo (se desconectó
   de `adb` a mitad de esta sesión) — en vez de adivinar una causa, se blindó
   `DictationTextField` para que cualquier resultado que no sea `RESULT_OK` con texto (falla o
   cancelación real, no un cierre normal del diálogo) muestre un `Toast`, así deja de fallar en
   silencio. Si el problema persiste, hace falta el `logcat` del momento exacto para diagnosticar
   la causa real.
5. **Teclado alfabético en campos numéricos**: se revisaron los 4 campos que faltaban
   `KeyboardOptions(keyboardType = ...)` (duración de hábito, monto de movimiento, objetivo y
   progreso de Meta) y se les agregó `Number`/`Decimal` según si aceptan punto decimal. Los
   campos de PIN de Zona Privada ya lo tenían (`NumberPassword`) desde que se construyeron.
6. **Tareas recurrentes** (pagar luz, agua, etc.): nuevo enum `RecurrenciaTarea`
   (`SIN_REPETIR`/`DIARIA`/`SEMANAL`/`QUINCENAL`/`MENSUAL`/`BIMESTRAL`/`TRIMESTRAL`/`ANUAL`),
   deliberadamente separado del `Recurrencia` que ya existía (ese es solo para Fecha
   importante, con semántica distinta — no se reutilizó para no arriesgar romper ese flujo).
   Ver detalle completo más abajo.
7. **Listas reutilizables** (viaje, compras, etc.): nueva entidad `Lista`/`ListaItem`, fuera
   del modelo polimórfico de `Actividad` (mismo precedente que `Meta`, que tampoco es un
   `TipoActividad`). Ver detalle completo más abajo.
8. **Revisión semanal no se podía volver a ver/editar**: `WeeklyReviewViewModel` ya consultaba
   si existía una revisión guardada de la semana (`guardada: Boolean`) pero nunca leía su
   contenido — los 3 campos de texto siempre arrancaban vacíos, así que "actualizar" en
   realidad sobrescribía en blanco si el usuario no recordaba lo que había escrito. Fix: el
   `RegistroSemanal` guardado se precarga en los 3 `DictationTextField` (editable, no de solo
   lectura) y el botón cambia a "Actualizar revisión" cuando ya existe una. Un historial de
   revisiones semanales pasadas (más allá de la semana en curso) queda pendiente si se pide
   más adelante — no se construyó a medias esta sesión.

### Tareas recurrentes

`ActividadDetalle.Tarea` ganó `recurrencia: RecurrenciaTarea = SIN_REPETIR`. Al marcar una
Tarea recurrente como `CONFIRMADO`:

- `ActividadRepository.reprogramarTareaRecurrente(actividadId, nuevaFechaLimite, usuarioId)`
  registra la ocurrencia actual en `registro_actividad` (mismo mecanismo que ya usan los
  Hábitos para su historial — reutilizado, no una tabla nueva), actualiza `fechaLimite` en
  `TareaDetalleEntity` y vuelve a dejar `ActividadEntity.estado` en `SIN_CONFIRMAR`.
- `MarcarActividadUseCase` orquesta: llama a `marcarEstado` normal, y si es una Tarea con
  `recurrencia != SIN_REPETIR`, calcula la próxima fecha (`core/utils/RecurrenciaTareaUtils.kt`,
  aritmética con `kotlinx.datetime.DatePeriod`) y llama a `reprogramarTareaRecurrente` +
  reprograma la alarma si tenía `horaRecordatorio` — mismo patrón que un Hábito, pero la
  orquestación vive en el caso de uso (no en el repositorio) porque necesita
  `RecordatorioScheduler`, que el repositorio no conoce.
- **Deliberadamente sin un cuarto estado** en `EstadoActividad` — el usuario preguntó "¿pagar
  la luz es una Tarea o una Rutina?" y la respuesta se documentó y se mostró en la propia app
  (texto en `CrearRutinaScreen`): es una Tarea (recurrente), nunca una Rutina — Rutina agrupa
  actividades ya existentes, no encaja con "una sola cosa que se repite".
- Selector `core/ui/RecurrenciaTareaSelector.kt` (mismo patrón de `FilterChip` que
  `NivelRecordatorioSelector`), visible en `CrearTareaScreen` solo si ya hay fecha límite.
  `TaskDetailScreen` muestra "🔁 Se repite: {intervalo}" cuando aplica.
- **Fuera de alcance de esta pasada**: el vínculo explícito con Medicamentos/Citas
  (mencionado por el usuario como un caso futuro de "cuidado de familia por cierto tiempo") —
  esas entidades ya existen como stubs en el modelo pero no están conectadas todavía (fuera de
  Fase 0.1/0.5).

### Listas reutilizables

`Lista` (nombre) + `ListaItem` (texto, marcado, orden) — plantilla reutilizable de cosas por
chequear, no ligada a Hábito/Tarea/Rutina. Tablas `lista`/`lista_item` (FK `CASCADE` de
`lista_item` hacia `lista`). "Reiniciar lista" (`ReiniciarListaUseCase`) desmarca todos los
ítems en el sitio — **se descartó a propósito** el modelo de "plantilla + ejecuciones
históricas separadas" que el usuario exploró con ideas de otra IA (una `ListaEjecucionEntity`
por cada uso, con snapshot de ítems) por ser mucho más trabajo de modelo de datos para el
mismo beneficio inmediato; el historial de usos pasados de una lista queda documentado como
posible mejora futura, no construido a medias ahora.

- `ListaDao.observarConConteo(espacioId)`: una sola consulta con `LEFT JOIN` +
  `GROUP BY lista.id` para traer "cuántos ítems tiene y cuántos están marcados" por lista sin
  N+1 — usada en la pantalla de lista de Listas.
- `ListaRepository.observarConItems(listaId)` combina (`kotlinx.coroutines.flow.combine`) un
  `Flow<ListaEntity?>` y un `Flow<List<ListaItemEntity>>` en un solo `Flow<ListaConItems?>`
  para la pantalla de detalle — si la lista se elimina, el `Flow` emite `null` y la pantalla de
  detalle lo interpreta como "salir", sin necesitar un flag de "eliminada" seteado a mano.
- **Lección de auditoría aplicada desde el diseño esta vez** (evitando repetir el bug de
  `MetaEntity`/`RegistroSemanalEntity`): `ListaEntity` y `ListaItemEntity` se marcaron
  `@Serializable` desde que se escribieron, antes de conectarlas a `AuditLogger`.
- Accesible desde "📋 Ver mis listas" en Hoy (solo si ya hay al menos una, mismo patrón de
  Metas/Rutinas) y desde el menú `+`.

## Tercera ronda de feedback de uso real — añadido 2026-07-28

1. **Pantallas "estáticas" que ocultaban el botón de guardar**: ningún formulario tenía scroll
   — con el teclado abierto, varios chips seleccionados o texto largo (ej. las 3 reflexiones de
   Revisión semanal), el contenido crecía más que la pantalla y el botón de acción quedaba fuera
   de vista, inalcanzable. Se revisaron **todas** las pantallas con un `Column` de contenido
   dinámico (formularios de crear/editar y pantallas de detalle — 13 archivos:
   `CerrarDiaScreen`, `CrearMovimientoScreen`, `FinancesScreen`, `CrearMetaScreen`,
   `MetaDetailScreen`, `CrearHabitoScreen`, `HabitDetailScreen`, `CrearRutinaScreen`,
   `RoutineDetailScreen`, `SettingsScreen`, `CrearTareaScreen`, `TaskDetailScreen`,
   `WeeklyReviewScreen`) y se les agregó `.verticalScroll(rememberScrollState())`. Las
   pantallas de lista (`LazyColumn`) y `PrivacyGateScreen` (contenido fijo corto) no lo
   necesitaban. **Lección para el futuro**: todo formulario nuevo con `DictationTextField`
   multilínea, selectores de chips que puedan crecer, o un botón de acción al final, debe
   llevar `verticalScroll` desde que se escribe — no esperar a que se reporte.
2. **Menú "⋮" abría el `DropdownMenu` en el lugar equivocado**: `DropdownMenu` se ancla al
   composable que lo contiene *directamente* — estaba como hijo de un `Row` de ancho completo
   (`fillMaxWidth` + `Arrangement.End`), así que su ancla real era todo ese `Row` (la pantalla
   entera), no el ícono. El ícono se veía en la esquina por el `Arrangement.End`, pero el menú
   se posicionaba relativo al `Row` completo, no al botón. Fix: `IconButton` + `DropdownMenu`
   ahora viven juntos dentro de su propio `Box` (alineado con `Alignment.CenterEnd` dentro de
   un `Box` exterior de ancho completo), así el ancla real es exactamente el ícono.
   **Lección**: `DropdownMenu`/`Popup` siempre ancla a su padre de composición inmediato, no al
   elemento visualmente más cercano — hay que envolver ancla + menú en su propio contenedor
   ajustado, nunca dejarlos sueltos dentro de un `Row`/`Column` más grande.
3. **"Aproximadamente N notificaciones por día/semana" en Ajustes de Android**: es texto que
   escribe el propio sistema operativo en la pantalla de ajustes de notificaciones de la app
   (una estimación de Android según el uso, no algo que Lula controle ni calcule). Se agregó
   una aclaración en pantalla para que no se preste a confusión.
4. **Sin acceso rápido a "ver todas mis tareas/hábitos" desde Crear Tarea/Hábito**: antes había
   que guardar o cancelar, volver a Hoy, y recién ahí buscar la lista. Se agregó un
   `TextButton` ("📝 Ver todas mis tareas" / "✅ Ver mis hábitos") justo debajo del título en
   `CrearTareaScreen`/`CrearHabitoScreen`, que navega directo a la lista correspondiente.
5. **Micrófono que "parpadea" en Nueva Tarea/Nueva Meta pero funciona bien sin cable**: el
   propio usuario aisló la causa probable al probarlo — funcionaba normal apenas desconectó el
   cable USB. Es coherente con una interferencia del modo depuración/USB (no infrecuente en
   ciertos dispositivos/fabricantes cuando hay una sesión de `adb` activa) y no con un bug de
   Lula — no se tocó código de dictado por este punto; ya tenía manejo de fallos desde la ronda
   anterior (ver más arriba).
6. **Checkbox de Tareas que no se veía marcar**: `TasksListScreen` envolvía la fila **entera**
   (incluido el `Checkbox`) en un solo `.clickable { onTareaClick(...) }` para ir al detalle —
   dos gestos superpuestos sobre el mismo `Checkbox` (su propio toggle interno + el click de la
   fila) competían por el toque, y a veces el marcado no se reflejaba visualmente o se
   interpretaba como "ir al detalle". Fix: el `Checkbox` quedó sin ningún `clickable` que lo
   envuelva (solo su propio `onCheckedChange`), y el `clickable` hacia el detalle se movió
   únicamente al `Text` del nombre — mismo patrón ya usado sin problemas en Hoy
   (`HomeScreen.seccionActividades`, que nunca tuvo esta clase de bug porque el `Row` ahí nunca
   fue clickeable como un todo). Se revisó el resto de listas con checklist
   (`RoutineDetailScreen`, `ListDetailScreen`) y no tienen este problema — nunca envolvieron la
   fila completa en un `clickable`.

## Cuarta ronda de feedback de uso real — añadido 2026-07-28

1. **Accesos rápidos que faltaban**: ya existían en Crear Hábito/Tarea; se agregaron también
   en Crear Rutina ("Ver mis rutinas"), Crear Meta ("Ver mis metas"), Crear Lista ("Ver mis
   listas") y Crear Movimiento ("Ver Finanzas y resumen") — ver regla 5 de la sección de
   convenciones arriba.

2. **Bug real de fondo detrás de "el check no cambia de color"**: no era un problema del
   `Checkbox` en sí — era una condición de carrera. 12 ViewModels seguían el mismo patrón:
   `private var sesion: SesionActual? = null`, resuelto una vez dentro de `init { launch {
   sesion = obtenerSesionActualUseCase() } }`, y cada acción hacía
   `val sesionActual = sesion ?: return` **de forma síncrona, fuera de la corrutina**. Si el
   usuario tocaba el check apenas se abría la pantalla (antes de que esa resolución async
   terminara — dos lecturas de Room encadenadas, `authRepository.usuarioActualId()` +
   `espacioRepository.obtenerEspacioPersonal(...)`), la acción se descartaba en silencio: el
   `Checkbox` de Compose es un componente controlado por `checked = tarea.estado == CONFIRMADO`,
   así que si el estado real nunca cambia en la base de datos, no hay nada que recomponer — el
   toque simplemente no hacía nada, sin ningún error visible. Se corrigió en los 12 archivos
   (ver regla 6 arriba): cada acción ahora resuelve la sesión *dentro* de su propia corrutina
   con un helper `sesionActual()` que la cachea la primera vez que la necesita, sin depender de
   que `init` haya terminado antes. Ya se había arreglado el problema de superposición de
   `clickable` en `TasksListScreen` en la ronda anterior — este es un segundo bug distinto que
   coexistía con el mismo síntoma reportado.

3. **Sonido de check no sonaba fuera de Hoy**: solo estaba conectado en `HomeScreen`. Se creó
   `core/ui/SonidoCheckViewModel.kt` (`AjustesRepository` expuesto como `StateFlow<Boolean>`,
   reutilizable con `hiltViewModel()` sin duplicar la inyección) y se conectó en
   `TasksListScreen`, `RoutineDetailScreen` y `ListDetailScreen`. De paso, todos los ítems
   completados ahora se ven tachados (`TextDecoration.LineThrough`), en Hoy también — antes
   solo cambiaba el color del `Checkbox`, poco perceptible.

4. **Eliminar sin confirmar**: `core/ui/ConfirmarEliminarDialog.kt` (un `AlertDialog` genérico
   con Eliminar/Cancelar) se conectó a las 7 acciones de eliminar de la app: Hábito, Tarea,
   Meta, Rutina, movimiento de Finanzas, y las 2 de Lista (ítem individual y lista completa —
   esta última fue el ejemplo concreto que reportó el usuario). Quitar una fila de un
   formulario *antes de guardarlo* (ej. un ítem de `CrearListaScreen` mientras se arma la
   lista) no pide confirmación a propósito — todavía no es una eliminación real, es editar un
   borrador.

5. **Semana: lunes, no se borra, y ahora se puede leer**: la semana de Lula empieza el
   **lunes** (ISO-8601, ver `DateTimeUtils.claveSemana`) — se puede cambiar a domingo si se
   prefiere, es una constante en un solo lugar. Ninguna semana guardada se borra: cada una
   vive en `registro_semanal` con su propia fila (clave única `(espacioId, semana)`), para
   siempre. Lo que faltaba era una pantalla para **leerlas** — se agregó
   `RegistroSemanalRepository.observarHistorial()` (el `Flow` ya existía en el DAO desde el
   stub original, solo faltaba exponerlo) + `ObtenerHistorialSemanalUseCase` +
   `WeeklyReviewHistoryScreen` ("📜 Ver semanas anteriores" desde Revisión semanal), de más
   reciente a más antigua, con las 3 reflexiones de cada semana visibles — el mismo patrón que
   ya usa `HistoryScreen` para los días cerrados.

## Rachas y Constancia, Matriz de Eisenhower, Hábitos progresivos — añadido 2026-07-28

Las 3 piezas restantes de Fase 0.5.

**Constancia**: `ObtenerProgresoDeHoyUseCase.calcularConstancia()` — % de días con
`actividadesCompletadas > 0` en los últimos 30 días (fórmula de `01-arquitectura.md`,
independiente de la racha, no se resetea si se rompe una racha). Se muestra como `StatPill`
en `HistoryScreen` (no se construyó la pantalla "Progreso" completa de `02-pantallas.md` —
sigue fuera de alcance, ver más abajo).

**Matriz de Eisenhower**: no necesitó cambios de esquema — `Tarea.importante`/`urgente` ya
existían. Es una vista/filtro nueva sobre `TasksListScreen` (chips "Lista" / "🗂️ Matriz"),
que agrupa las tareas ya cargadas en 4 secciones (HACER/PROGRAMAR/DELEGAR/POSPONER) en vez de
la lista plana. Se adaptó el layout 2x2 de `02-pantallas.md` a 4 secciones apiladas
verticalmente — dos columnas angostas con nombres de tarea largos no eran legibles en una
pantalla de celular.

**Hábitos progresivos**: `ActividadDetalle.Habito` ganó `duracionActualMin` (la duración que
se usa hoy, arranca en `duracionInicialMin`) y `proximaRevisionEpochDay` (cuándo toca
preguntar "¿aumentamos?") — subió `LulaDatabase` a `version = 7`. Un hábito es "progresivo"
(`esProgresivo`) solo si el usuario llenó los 4 campos (inicial/objetivo/incremento/frecuencia
de revisión) en `CrearHabitoScreen`, detrás de un chip "📈 Aumentar con el tiempo" — nunca
activado por Lula.

- Al guardar (`CrearHabitoUseCase`/`ActualizarHabitoUseCase`),
  `core/utils/calcularProgresionInicial()` calcula el estado inicial. Al **editar** un hábito
  progresivo sin tocar la configuración de progresión, `ActualizarHabitoUseCase` preserva el
  progreso ya hecho (no lo reinicia) — solo recalcula desde cero si la configuración cambió o
  se activa por primera vez.
- La tarjeta "¿Aumentamos?" (`02-pantallas.md`) aparece en Hoy cuando
  `hoy >= proximaRevisionEpochDay` y todavía no se alcanzó el objetivo — calculada en
  `HomeViewModel` reutilizando `ObtenerHistorialHabitoUseCase.ultimosDias` para "cumpliste X
  de Y días", la misma función que ya usan Metas y el tracker de Hábitos. 3 botones exactos
  del documento: Subir / Mantener / Recordarme después (pospone 1 día) —
  `ResponderRevisionHabitoUseCase` + `ActividadRepository.actualizarProgresionHabito()`.
- **Decisión deliberada de reactividad**: escribir en `habito_detalle` no invalida el `Flow`
  de Hoy (que solo referencia `actividad`/`registro_actividad`, mismo patrón ya documentado
  arriba) — en vez de extender el `combine()` de Hoy para trackear también `habito_detalle`
  (afectaría todas las lecturas de hábito, no solo esta tarjeta efímera), la tarjeta se quita
  con una actualización local del `UiState` al responder. Si en el futuro se necesita
  reactividad completa de `habito_detalle` en Hoy, ahí sí se justifica el `combine()`.
- **Lección de auditoría aplicada de nuevo**: `HabitoDetalleEntity` nunca se había usado con
  `AuditLogger` (solo se auditaba `ActividadEntity` al crear/editar un hábito) — al agregar
  `actualizarProgresionHabito()`, que sí la audita directamente, se le agregó `@Serializable`
  **antes** de conectarla, no después de un crash (ver la lección original más arriba).

## Fuera de alcance de esta sesión (base sólida, Fase 0.1 núcleo)

Sin Firebase Auth real, sin sync a n8n/Sheets (Room 100% local, `syncStatus` existe en el
modelo pero sin cliente de red), sin onboarding, sin auto-bloqueo por inactividad en Zona
Privada, sin fases posteriores a 0.1 más allá de Metas (medicamentos, círculo de cuidado,
familia, asistente) — sus entidades y DAOs ya existen con CRUD mínimo para que la base de
datos completa compile, pero sin pantallas ni lógica de negocio todavía.

## Dependencias añadidas

Room 2.7.2 (KSP), Hilt 2.54 (KSP) + hilt-navigation-compose 1.2.0, Navigation Compose 2.8.5,
DataStore Preferences 1.1.1, kotlinx-datetime 0.6.1, kotlinx-serialization-json 1.7.3,
androidx.biometric 1.1.0. Sin Retrofit/OkHttp (no hacen falta sin sync).
`room.schemaLocation` exportado desde el día 1 (`app/schemas/`) para migraciones futuras.

## Fase 0.8 — Medicamentos y Citas, añadido 2026-07-28

Subió `LulaDatabase` a `version = 8` (nueva tabla `toma_medicamento`, nuevos campos en
`medicamento_detalle`/`cita_detalle`/`usuario`).

**Estado por toma, no por día**: a diferencia de un Hábito (un estado por día), un Medicamento
puede tener varias tomas en el mismo día — se necesitaba una clave `(actividadId, fecha,
horario)`, que `RegistroActividadEntity` (única por `(actividadId, fecha)`) no soporta. Se
creó `TomaMedicamentoEntity`/`TomaMedicamentoDao`/`TomaMedicamento` como tabla independiente
en vez de forzar el modelo existente.

**Dos modos de frecuencia calculan `horariosCalculados[]` en el momento de guardar** (no en
cada lectura): `core/utils/HorariosMedicamentoUtils.kt` —
`calcularHorariosPorIntervalo(horaPrimeraDosis, intervaloHoras)` reparte dosis uniformes en 24h;
`calcularHorariosPorComida(comidasRelacionadas, horaDesayuno, horaAlmuerzo, horaCena)` mapea 1:1
y **en el mismo orden** que `comidasRelacionadas`, para que `instruccionParaHorario(detalle,
index)` pueda reconstruir "Después del almuerzo" junto a la hora calculada sin guardar un mapa
aparte. Si el usuario todavía no guardó la hora de esa comida, esa comida se omite del cálculo
(la pantalla se lo pide inline, ver abajo) — nunca se inventa una hora.

**Horarios de comida en el perfil, sin pantalla de Perfil dedicada**: `Usuario.horaDesayuno/
horaAlmuerzo/horaCena` (nullable) se piden **inline** en `CrearMedicamentoScreen` la primera
vez que el usuario elige "según las comidas" y esa comida en particular no tiene hora guardada
— se persisten de inmediato vía `ActualizarHorariosComidaUseCase` (no solo al guardar el
medicamento), así quedan disponibles para el próximo medicamento sin repetir la pregunta.
Construir una pantalla "Perfil" completa quedó fuera de alcance.

**Recordatorios reutilizan la infraestructura existente en vez de duplicarla**: en lugar del
"Alarma | Notificación" binario que sugería el roadmap, Medicamento y Cita usan el mismo
`NivelRecordatorio` de 3 niveles (🔇/🔔/⏰) que Hábito/Tarea — mismos canales, mismo
`RecordatorioScheduler`, mismo `RecordatorioReceiver`. Para soportar que un Medicamento tenga
**varias alarmas independientes por día**, `RecordatorioScheduler` ganó una clave de
`PendingIntent`/notificación compuesta `"actividadId:horario"` (antes solo `actividadId`) —
`crearPendingIntent`/`programar`/`cancelar` aceptan un `horario: String?` opcional. El extra
`EXTRA_ES_HABITO: Boolean` del `Intent` se reemplazó por `EXTRA_TIPO` (el `TipoActividad.name`
existente), para poder distinguir HABITO/TAREA/MEDICAMENTO/CITA con un solo campo en vez de ir
agregando booleans. `BootReceiver.reprogramarTodo()` ganó los mismos dos loops que ya tenía
para Hábito/Tarea.

**Bug encontrado y corregido al conectar Eliminar**: `EliminarActividadUseCase` cancelaba la
alarma con `recordatorioScheduler.cancelar(actividadId)` (clave simple) — para un Medicamento
con varias tomas, eso dejaba sonando las alarmas de las demás dosis, cuya clave es compuesta.
Se corrigió consultando el detalle antes de eliminar y cancelando cada `horario` por separado
cuando el tipo es Medicamento.

**Reactividad de Hoy**: se creó `ObtenerMedicamentosDeHoyUseCase` (domain), que combina
`observarMedicamentos` + `observarTomasDeHoy` con `flatMapLatest` y resuelve, para cada
medicamento activo vigente hoy (`fechaInicio <= hoy <= fechaFin`), su lista de
`TomaDeHoy(horario, instruccion, estado)`. Se usa tal cual desde **dos** ViewModels (`Home` y
`Health`) para no duplicar esta lógica — a diferencia de la tarjeta de revisión de Hábitos
progresivos (que usa una actualización local de `UiState`), acá sí valía la pena la
reactividad completa porque el mismo caso de uso alimenta dos pantallas distintas.

**3 botones, nunca un castigo**: cada toma se muestra con `core/ui/TomaAccionRow.kt` — 3 chips
`⏳ Pendiente / ✅ Tomada / ⏭️ Omito`, todos igualmente válidos, reutilizado sin cambios en Hoy,
"Mi salud" y la pantalla de acción al tocar la notificación (`AccionTomaScreen`, misma familia
que `RecordatorioAccionScreen` pero con 2 acciones en vez de "posponer" — no aplica posponer
porque la toma ya vive en una lista siempre visible, no en una notificación efímera).

**Asimetría deliberada Medicamento/Cita**: Medicamento tiene pantalla de detalle propia
(`MedicamentoDetailScreen`, con historial de tomas de 7 días, pausar/reanudar) porque es una
actividad recurrente con seguimiento a lo largo del tiempo — Cita es un evento puntual, se
edita/elimina directo desde una fila de "Mi salud" sin pantalla de detalle intermedia.

## Feedback en dispositivo real sobre Fase 0.8, añadido 2026-07-28

Seis correcciones tras probar la app en el celular (subió `LulaDatabase` a `version = 9` por
el cambio de esquema de `cita_detalle`):

1. **Menú "⋮" (Ajustes) solo vivía en Hoy** — estaba dentro de `HomeScreen`, así que en
   cualquier otra pantalla no había forma de llegar a Ajustes. Se movió a
   `navigation/LulaTopBar.kt`, montado en el `topBar` del `Scaffold` que envuelve todo el
   `NavHost` — ahora está disponible sin importar la pantalla activa.

2. **Dictado "parpadea" y no arranca** — `core/ui/DictationTextField.kt`. Con la pantalla
   bloqueada (ej. al volver de una notificación sin haber puesto el PIN/huella todavía), el
   reconocedor no llegaba a abrirse y no había ningún aviso — se agregó una verificación con
   `KeyguardManager.isKeyguardLocked` antes de lanzar el intent, con un `Toast` explicando qué
   hacer. **Intento fallido en el camino**: se probó agregar `EXTRA_PREFER_OFFLINE = true`
   pensando en el caso sin conexión, pero eso fuerza un modelo de voz descargado en el
   dispositivo que la mayoría de teléfonos no tiene — el reconocedor pasó a fallar de
   inmediato ("la búsqueda por voz no está disponible") **incluso con internet andando**, una
   regresión peor que el bug original. Se revirtió; el caso sin conexión queda sin resolver
   por ahora (el reconocedor del sistema decide, la app no puede forzar un modo que no rompa
   ningún dispositivo).

3. **Chips de toma de Medicamento, todos igual de coloridos**: los 3 estados
   (pendiente/tomada/omitida) eran `FilterChip` del mismo peso visual, costaba distinguir de
   un vistazo cuál ya se hizo. Rediseñado en `core/ui/TomaAccionRow.kt`: mientras está
   `SIN_CONFIRMAR` se muestran dos `TextButton` neutros ("Tomada"/"Omito", sin color) — al
   elegir uno, se reemplaza por un solo `AssistChip` con color (el estado activo) más un
   "↩️ Deshacer" también neutro. Solo el estado en el que está la toma lleva color.

4. **Texto invisible en las tarjetas de "Mi salud" y la revisión de Hábito progresivo en
   Hoy**: bug real de contraste en modo oscuro. `Card(colors = CardDefaults.cardColors(containerColor = LulaXxxContainerLight))`
   usaba siempre la variante "Light" del color de marca sin importar el tema del sistema — en
   modo oscuro, el `contentColor` que hereda del tema es claro, así que quedaba texto claro
   sobre un fondo también claro. Se creó `ui/theme/ThemedContainer.kt`
   (`lulaCardColors(claro, oscuro)` / `lulaContainerColor` / `lulaContentColorSobreContainer`)
   que elige la variante correcta según `isSystemInDarkTheme()` y fija un `contentColor` que
   sí contrasta — aplicado en `HealthScreen` y en la tarjeta de revisión de `HomeScreen`. Regla
   general para toda superficie nueva con color de marca y texto: usar este helper, nunca
   `XxxContainerLight` a secas.

5. **Recordatorio de Cita sonaba a la misma hora que la cita**: `AnticipacionRecordatorio`
   era un solo offset que se restaba de `fechaHora` (ej. "un día antes" = 24h antes, a la
   misma hora exacta de la cita) — no permitía, por ejemplo, "un día antes a las 20:00 **y**
   el mismo día a las 7:00" con una cita a las 12:00. Se reemplazó
   `Cita.recordatorioAnticipacion: AnticipacionRecordatorio` por
   `Cita.recordatorios: List<RecordatorioCita>`, donde cada `RecordatorioCita(anticipacion,
   hora)` tiene su **propia hora**, independiente de la hora de la cita. `CrearCitaScreen`
   ahora muestra las 3 opciones de anticipación como chips activables, cada una con su propio
   `HoraSelector` cuando está activa (0, 1 o los 3 a la vez). `RecordatorioScheduler.programarCita`
   programa una alarma independiente por recordatorio (clave compuesta
   `actividadId:cita:anticipacion`, mismo patrón que las tomas de Medicamento).

6. **Mismo bug de alarmas huérfanas que Medicamento, pero en Cita**: al tener varios
   recordatorios por cita, `EliminarActividadUseCase`/`ActualizarCitaUseCase` necesitaban
   cancelar los 3 posibles (`AnticipacionRecordatorio.entries`), no solo la clave simple —
   como son solo 3 valores fijos, cancelar los 3 siempre (sin importar cuáles estaban
   configurados) es más simple y robusto que ir a buscar cuáles había antes; cancelar una
   alarma que no existía es un no-op seguro.

### Segunda vuelta: "Presiono Crear y no pasa nada"

`CrearMedicamentoViewModel.guardar()`/`CrearCitaViewModel.guardar()` tenían validaciones
(nombre vacío, falta la hora de la primera dosis en modo intervalo, ninguna comida elegida en
modo "según las comidas") que hacían `return` sin ningún aviso — desde la UI se veía
exactamente como un botón roto. Se agregó `mensajeError: StateFlow<String?>` a ambos
ViewModels; la pantalla lo consume con `LaunchedEffect` + `Toast` y llama a
`viewModel.errorMostrado()` para limpiarlo. Regla para toda pantalla de Crear nueva: **una
validación que bloquea el guardado siempre debe avisar por qué**, nunca fallar en silencio.

Aclaración (no era un bug): una Cita no aparece dentro de la lista de Hoy — a propósito, solo
vive en "Mi salud". Hoy solo muestra el enlace "Ver mi salud" una vez que hay al menos un
medicamento o cita (mismo patrón que "Ver mis metas"/"Ver mis rutinas").

## Fase 1.0 — Círculo de cuidado, base local (2026-07-29)

**Por qué "base local" y no Fase 1.0 completa**: el objetivo de esta fase es activar
`SOLICITUD_COMPARTIR` con consentimiento explícito **entre dos personas en dispositivos
distintos** — pero la app sigue sin autenticación real (`AuthRepositoryLocalImpl`, usuario
semilla) y sin ningún cliente de sync (Room 100% local, `syncStatus` sin conectar). Sin eso,
no hay forma de que una solicitud "llegue" al teléfono de otra persona ni de que su
aceptación "vuelva" al mío. Antes de construir sobre ese vacío, se decidió (con el usuario)
construir todo lo que **sí** puede funcionar hoy, en un solo dispositivo, dejando el resto
listo para conectarse en cuanto exista un proyecto de Firebase — ver la decisión ya registrada
sobre Onboarding en `Plan/04-roadmap-fases.md`.

**Qué es honesto construir ahora vs. qué sería UI vacía**: de las dos secciones de "Mi
círculo de cuidado" (`Plan/02-pantallas.md`), solo una es real sin backend:
- **"Quién me acompaña a mí"** (personas a quienes YO compartí algo) — es sobre MIS propios
  datos, que sí viven en este dispositivo. Completamente funcional: enviar solicitud,
  verla listada con su estado, cancelarla/revocarla.
- **"Personas que acompaño"** (lo que otros comparten CONMIGO) — necesita los datos de la
  OTRA persona sincronizados a mi dispositivo. Sin backend es literalmente imposible, así que
  se dejó como estado vacío honesto en vez de simular datos falsos.

**Modelo de datos**: ya existía un stub de Fase 0.1 (`SolicitudCompartirEntity`/
`SolicitudCompartirDao` con `upsert`/`observarPendientesPara`, y un `SolicitudCompartir` de
dominio con campos `String` sin tipar, viviendo — por descuido de esa sesión — dentro de
`RetoFamiliar.kt`). Esta ronda: se le dio su propio archivo
(`domain/model/SolicitudCompartir.kt`), y sus campos de texto libre pasaron a enums reales
(`PermisoCompartir`, `EstadoSolicitud`, `CanalEnvio`) — mismo estándar que el resto del
dominio. Se completó el DAO (`obtenerPorId`, `eliminar`, `observarEnviadasPor`) y se corrigió
un bug en la query ya existente `observarPendientesPara` (comparaba contra `'pendiente'` en
minúscula, pero todo el resto de la app guarda enums con `.name`, es decir en mayúsculas —
nunca hubiera encontrado una fila). No hizo falta subir la versión de la base de datos: la
tabla ya existía con las mismas columnas, `@Serializable` es metadata de compilación, no de
esquema.

**Regla de negocio preservada**: `CompartirActividadUseCase` crea la solicitud en estado
`PENDIENTE` y **no** toca `puedeVer[]`/`puedeRecordar[]`/`privacidad` de la `Actividad` —
esos campos solo deben poblarse cuando la otra persona **acepta de verdad** (ver
`Plan/01-arquitectura.md`: "compartir siempre es solicitud + aceptación, nunca automático").
Como esa aceptación real todavía no existe, el código deliberadamente no simula un
"auto-aceptar" para poder demostrar la función — sería falsear el estado del sistema.

**Dónde se conecta "Compartir seguimiento"**: se agregó un botón en el detalle de Hábito,
Tarea y Medicamento (los tres casos de uso más claros de cuidado/acompañamiento — ej. "avisar
a mi hijo si no tomé la pastilla"). Rutina, Meta y Cita quedan pendientes de la misma
integración (es un diálogo reusable, `core/ui/CompartirActividadDialog.kt`, agregarlo a una
pantalla nueva es un cambio de 3 líneas) — no se hizo esta ronda por acotar el alcance, no por
dificultad técnica. Cita en particular no tiene pantalla de detalle propia todavía (ver
decisión de Fase 0.8 más arriba), así que necesitaría resolverse ahí primero.

**Punto de entrada**: "👥 Mi círculo de cuidado" se agregó al menú "⋮" (`LulaTopBar`, el mismo
que se movió al `Scaffold` esta sesión) en vez de ocupar una posición del bottom bar — la
personalización de posiciones 2-4 de `02-pantallas.md` no está construida todavía, y ocupar
una posición fija hardcodeada hubiera desplazado a Hábitos o Finanzas sin que el usuario lo
pueda ajustar.

## Ronda de feedback en dispositivo real, 7 puntos (2026-07-29)

1. **Bug real: el menú "⋮" no se veía en ninguna pantalla.** Causa: `enableEdgeToEdge()` está
   activo en `MainActivity` desde antes de esta sesión — con eso, el contenido de
   `Scaffold.topBar` es responsable de su propio inset de la barra de estado (así es como
   funciona `TopAppBar` de Material3, aplicando `TopAppBarDefaults.windowInsets`
   internamente). `LulaTopBar` es un `Box` a medida sin ese inset, así que el ícono "⋮" se
   dibujaba literalmente detrás del reloj/batería del sistema — invisible. Antes de esta
   sesión el menú vivía dentro del `Column` de `HomeScreen`, que sí heredaba el padding
   superior correcto porque `Scaffold` no tenía `topBar` definido todavía (sin `topBar`, el
   inset de la barra de estado se empuja al contenido en vez de dejárselo a la topBar). Se
   corrigió agregando `.statusBarsPadding()` a `LulaTopBar`. **Lección**: cualquier
   composable custom usado como `Scaffold.topBar`/`bottomBar` en una app con
   `enableEdgeToEdge()` necesita manejar sus propios insets — no es automático como con los
   componentes `TopAppBar`/`NavigationBar` de Material3.

2. **Chips de toma de Medicamento seguían sin distinguirse bien** (segunda vuelta sobre el
   mismo punto de la ronda anterior): la etiqueta del botón pendiente decía literalmente
   "✅ Tomada" — el emoji de visto ya estaba en el texto aunque nada se hubiera marcado
   todavía, y por eso parecía premarcado. Rediseñado en `core/ui/TomaAccionRow.kt` para usar
   un `Checkbox` real (vacío hasta tocarlo, igual que cualquier otro check de la app) en vez
   de un botón de texto con un emoji de check incrustado. "Omitir" queda aparte, como texto
   plano que se convierte en chip gris solo cuando está activo.

3. **Detail screens con texto viejo después de editar y volver.** Causa raíz: Navigation
   Compose no recrea el `ViewModel` al volver de una pantalla de Editar con `popBackStack()`
   — reutiliza la misma instancia (ligada al `NavBackStackEntry` que nunca se destruyó), y su
   `_uiState` seguía teniendo la foto de datos cargada la primera vez, en `init`. Se corrigió
   en los 5 ViewModels de detalle (Hábito, Tarea, Rutina, Meta, Medicamento) exponiendo un
   `recargar()` público, llamado desde la pantalla con `LaunchedEffect(Unit)` — como la
   pantalla sí se vuelve a componer al reaparecer (a diferencia del ViewModel), esto refresca
   los datos cada vez que la pantalla vuelve a quedar activa. Regla general para todo detalle
   nuevo: si tiene un flujo de editar, necesita este patrón.

4. **Historial de Finanzas sin fecha.** `MovimientoUi.fecha` ya existía pero no se mostraba en
   ningún lado de la fila. Se agregó al costado de cada movimiento: fecha completa
   "aaaa-mm-dd" (`DateTimeUtils.formatearFechaCorta`) y la inicial del día de la semana
   (`DateTimeUtils.letraDiaSemana` — L, M, m, J, V, S, D; "m" minúscula para miércoles,
   distinto de "M" de martes, tal como lo pidió el usuario).

5. **"Mañana" es ambiguo en español** (momento del día vs. "tomorrow") — un hábito para
   "Mañana" se leía como si fuera para el día siguiente. Se agregó
   `core/ui/MomentoDelDiaLabel.kt` (`etiquetaMomentoDelDia`) con "Por la mañana"/"Por la
   tarde"/"Por la noche", aplicado en `CrearHabitoScreen`, `CrearRutinaScreen` y los títulos de
   sección de Hoy — reemplaza todo uso suelto de `MomentoDelDia.name.lowercase()...`.

6. **Flecha "→" de `SectionLinkRow` casi invisible** (afecta a todas las filas "Ver mis X",
   presentes y futuras, porque es un componente compartido): pasó de
   `typography.bodyLarge` (mismo tamaño que el texto de la fila) a `headlineSmall`, con un
   poco más de espacio.

7. **Meta "Manual": ¿el campo Objetivo es texto o número?** Aclarado con mejor etiqueta
   ("Objetivo (cantidad a alcanzar)") y un texto explicativo que aparece solo en modo Manual:
   siempre es un número que el usuario va incrementando a mano (ej. "12 de 20 libros") — la
   descripción de la meta en sí va en el campo "Nombre", más arriba. **Alcance**: no se separó
   en dos campos (texto/número) porque el modelo de `Meta` (y su barra de progreso) es
   inherentemente numérico en las 4 formas de medir — una meta puramente narrativa sin ningún
   número no tiene dónde mostrar progreso hoy; eso sería un cambio de modelo de datos más
   grande, no una aclaración de UI.

**Pregunta del usuario, sin cambio de código**: "¿las tareas ya terminadas [no recurrentes]
se guardan con su fecha de completado?" — Parcialmente. `TareaDetalleEntity`/`Actividad` no
tienen un campo `fechaCompletada` propio (solo `estado: CONFIRMADO/SIN_CONFIRMAR/OMITIDO`),
pero cada cambio de estado sí queda registrado con su `timestamp` en `historial_cambios` vía
`AuditLogger` (ver la lección de auditoría al principio de este documento) — el dato existe,
pero hoy no hay ninguna pantalla que lo muestre. Si se quiere ver "cuándo se completó" una
tarea puntual en la UI, hace falta construir esa vista (o agregar el campo directo a la
entidad) — no está hecho todavía.

## Compartir "moderno" (QR + selector nativo) y stats globales en el topBar (2026-07-29)

**1. Compartir por QR / WhatsApp / Telegram — qué es honesto construir sin backend.** El
pedido incluía 3 mecanismos: (a) QR que la otra persona escanea y "se activa el permiso
solo", (b) enviar por WhatsApp/Telegram/cualquier app, (c) buscar entre personas que ya
tienen Lula por nombre/correo. Mismo límite estructural que la base local de Fase 1.0 (ver
más arriba): sin cuenta real ni servidor, **(c) es imposible** — no hay ningún directorio de
"quién más usa Lula" fuera del propio teléfono. **(a) tampoco puede completar el round-trip
real**: aunque el QR encoding es 100% local, si el teléfono que escanea creara un registro de
"acceso concedido", el teléfono que generó el QR nunca se enteraría (no hay servidor que
avise) — la sección "Quién me acompaña a mí" del que compartió se quedaría en "Pendiente"
para siempre aunque el otro lado haya "aceptado". Por eso se construyó solo la mitad
honestamente útil de (a): generar y mostrar el QR (con `com.google.zxing:core`, nueva
dependencia — solo generación, sin cámara ni permisos nuevos) codificando el mismo texto de
invitación que se manda por WhatsApp, para el caso de tener los dos teléfonos juntos —
**no** se construyó el lado de escanear/leer el QR, sería trabajo real sin ningún beneficio
funcional hasta que exista backend. (b) sí es 100% viable hoy: `Intent.ACTION_SEND` +
`Intent.createChooser` — el selector nativo de Android, no hace falta integrar el SDK de
WhatsApp ni de Telegram, cualquier app instalada que acepte texto aparece sola en la lista.
Ambos quedaron en `core/ui/InvitacionEnviadaDialog.kt`, que reemplazó el `Toast` de
confirmación en los 3 flujos de "🤝 Compartir seguimiento" (Hábito/Tarea/Medicamento).

**2 y 4. Racha/gastos de hoy se movieron de Hoy al `topBar`.** Antes vivían dentro del
`LazyColumn` de `HomeScreen`, así que en cualquier otra pantalla quedaba un espacio vacío al
lado del menú "⋮" — se movieron a `LulaTopBar` (nuevo `TopBarStatsViewModel`, mismo patrón
que `SonidoCheckViewModel`: un ViewModel liviano inyectado directo en el composable
compartido). Al vivir en el `Scaffold` en vez de dentro de un destino del `NavHost`, este
ViewModel no se recrea al navegar — la racha (que no tiene una fuente `Flow` reactiva,
`calcularRachaActual` es un cálculo puntual) se volvería a mostrar vieja después de "Cerrar
mi día" si no se refrescara sola; se agregó `LaunchedEffect(currentRoute) { refrescar() }`
para que se vuelva a pedir cada vez que cambia de pantalla. Gastos de hoy sí es reactivo
(mismo `Flow` de Room que ya usaba `HomeViewModel`). De paso quedó resuelto el punto 4: el
ícono 💰 ahora navega a Finanzas (antes no tenía `onClick`); el ícono 🔥 sigue yendo a
Historial, como ya hacía.

**3. Aviso de invitación pendiente.** Se conectó `SolicitudCompartirRepository.observarPendientesPara`
(la query ya existía desde Fase 0.1, con el bug de mayúsculas corregido la ronda anterior) a
través de `ObtenerSolicitudesRecibidasUseCase` nuevo, hasta un ícono "📩" en `LulaTopBar` que
solo aparece si `solicitudesPendientes > 0`. **Siempre va a estar en 0 hoy**: como ya se
documentó en la base local de Fase 1.0, `para` guarda un contacto de texto libre, no un
`usuarioId` real, así que ninguna solicitud creada en otro teléfono puede aparecer "pendiente
para mí" sin un servidor que empareje ambos lados. Queda conectado a propósito para
activarse solo en cuanto eso exista, en vez de tener que acordarse de conectarlo después.

## Fechas importantes — construido completo (2026-07-29)

Era la única de las 3 opciones sin flujo real del menú `+` que ya tenía modelo de dominio y
entidad Room armados desde la base de Fase 0.1 (`ActividadDetalle.FechaImportante`,
`FechaImportanteDetalleEntity`/`Dao`) — solo faltaban repositorio, casos de uso y pantallas,
así que se completó siguiendo el mismo patrón que Medicamento/Cita en Fase 0.8.

Dos particularidades nuevas frente a lo ya construido:

- **`recurrencia` semanal/anual, no solo diaria.** Hasta ahora todo lo recurrente en la app
  reprogramaba su propia alarma para el día siguiente (Hábito) o el mismo horario del día
  siguiente (Medicamento). Fecha importante necesita "cada semana" o "cada año" — se resolvió
  con `RecordatorioScheduler.proximaOcurrenciaFechaImportante()`, que **nunca muta
  `fechaBase`** en la base de datos: siempre recalcula desde la fecha original guardada,
  avanzando de a 7 días o de a 1 año hasta encontrar la primera ocurrencia que ya no pasó —
  mismo principio que `proximoTrigger` ya aplicaba para la hora de un Hábito, extendido a
  días/años en vez de horas.
- **Reprogramarse a sí misma necesita releer de la base, no alcanza con los extras del
  `Intent`.** Hábito/Medicamento reprograman con la hora fija que ya viaja en el `Intent` de
  la alarma; Fecha importante necesita `recurrencia`+`fechaBase`+`anticipacion`+`tipoAviso`
  completos, y mandar los 4 por extras hubiera inflado el `Intent` genérico de
  `RecordatorioReceiver` con campos que solo usa este tipo. Se optó por que
  `RecordatorioReceiver` vuelva a leer el detalle completo desde `ActividadRepository` (con
  `goAsync()`, mismo patrón que `BootReceiver`) solo para este caso.

`TipoAviso` (🔔 Alarma con sonido / 💬 Solo notificación, ya existía en el modelo desde la
sesión base) se traduce a `NivelRecordatorio.ALARMA`/`SILENCIOSO` al programar la alarma —
deliberadamente **no** se usó el `NivelRecordatorio` de 3 niveles en la pantalla de crear,
para respetar el binario exacto que pide `02-pantallas.md` en vez de generalizarlo sin que se
pidiera. Tampoco hay pantalla de detalle separada (mismo criterio que Cita): se edita/elimina
directo desde la lista "Fechas importantes".

## Notas — construido completo (2026-07-29)

Última de las 3 opciones sin flujo real del menú `+`. A diferencia de Fecha importante, no
tenía nada armado de antes — modelo, entidad, DAO, repositorio, casos de uso y pantallas son
todos nuevos esta ronda. `LulaDatabase` sube de versión 9 a 10.

Decisiones de alcance, ya confirmadas con el usuario (`AskUserQuestion`): **solo texto por
ahora**. El usuario preguntó por un modo de dibujo tipo pizarra/Paint para bosquejar a mano
alzada; se recomendó no meterlo en esta ronda (Compose no tiene un componente de dibujo
libre listo, sería una superficie `Canvas` + persistencia de trazos aparte, con su propio
diseño de UI) y quedó anotado en `10-pendientes.md` como función futura separada, no como
parte de Notas.

- **Sin campo `titulo` en el modelo.** `Nota` solo guarda `contenido: String`; el título que
  se ve en la lista es la primera línea de `contenido` (con fallback "(sin texto)" si está
  vacía), calculado en `NotesListViewModel`. Evita mantener dos campos que se pueden
  desincronizar (usuario edita el texto pero no el título) por un dato 100% derivable.
- **Editor sin `verticalScroll`, la única pantalla de formulario que rompe esa convención a
  propósito.** Todo el resto de la app envuelve su `Column` de formulario en
  `verticalScroll` (ver "Convenciones de UI"). Acá el campo de texto necesita crecer con
  `weight(1f)` dentro de una `Column` de altura acotada por la pantalla, para poder hacer
  scroll interno él solo cuando el texto es largo; envolver todo en `verticalScroll` rompe
  esa distribución de peso (el `Column` exterior pasaría a tener altura infinita). Documentado
  con un comentario en el propio `NoteEditorScreen.kt` para que no se "corrija" por error en
  una limpieza futura.
- **Copiar y compartir son del `contenido` completo**, vía `LocalClipboardManager` e
  `Intent.ACTION_SEND` respectivamente — mismo mecanismo nativo de Android que ya usa
  "🤝 Compartir seguimiento" en otras pantallas, sin librería nueva.

## Calendario — construido completo (2026-07-29)

El usuario pidió explícitamente un calendario "tipo Google Calendar", con Día/Semana/Mes
intercambiables y grilla mensual completa — rechazó la alternativa más simple de una sola
vista de Agenda (día/semana en lista, sin grilla) que se le había recomendado primero por ser
menos trabajo. Se construyó el alcance completo que pidió.

- **Agregación por rango, no por día.** `ObtenerAgendaDelRangoUseCase` es el corazón de la
  función: trae cada tipo de actividad (Hábito/Tarea/Medicamento/Cita/Fecha importante) **una
  sola vez por tipo** para todo el rango visible (1 día, 7 días, o hasta 42 días en la grilla
  de Mes), no una consulta por día — evita N consultas × N días que hubieran sido lentas en
  la vista de Mes. Reutiliza queries de rango que ya existían
  (`RegistroActividadDao.obtenerPorActividadIdsYRango`) o son variantes chicas de una que ya
  existía (`obtenerTomasDeRango` recorre `obtenerPorActividadIdYRango` por cada medicamento —
  no se agregó una query plural nueva porque la cantidad de medicamentos por espacio es
  chica). El resultado se arma en memoria como `Map<LocalDate, List<ItemAgenda>>`.
- **Fecha importante ubicada con la misma lógica que su reprogramación de alarma.** Se extrajo
  `ocurreEnFecha()` a `core/utils/RecurrenciaUtils.kt` para que el Calendario pueda preguntar
  "¿esta Fecha importante cae en este día puntual?" sin duplicar el cálculo de
  `proximaOcurrenciaFechaImportante()` del scheduler — misma regla (nunca mutar `fechaBase`,
  siempre recalcular desde la fecha original) aplicada como comprobación en vez de como
  "próxima ocurrencia".
- **3 modos, 1 sola fuente de datos.** `CalendarViewModel` no tiene lógica separada por modo
  más allá de qué rango de fechas pedir (`rangoVisible`): Día pide 1 día, Semana pide la
  semana ISO completa (lunes a domingo, reutilizando
  `DateTimeUtils.inicioDeSemanaEpochDias`/`finDeSemanaEpochDias` que ya existían para Revisión
  semanal), Mes pide la grilla completa (todas las semanas parciales que tocan el mes, para
  que la grilla de 7 columnas no tenga huecos). Cambiar de modo o navegar ◀/▶/Hoy siempre
  vuelve a pedir la agenda del nuevo rango — no se cachea entre modos, para no arriesgar datos
  desactualizados después de marcar algo y volver.
- **Semana como secciones apiladas, no grilla de horas.** Se decidió no construir una grilla
  de horas tipo agenda de citas (con huecos libres entre eventos) — cada día de la semana se
  muestra como una mini-lista de Día, con su fecha como encabezado. Alcanza para "qué hay
  programado cada día de la semana" sin el costo de una grilla de horas con superposición de
  eventos, que ningún tipo de actividad de la app necesita hoy (nada dura más de un instante,
  a diferencia de un evento de calendario con hora de inicio y fin). Anotado en
  `10-pendientes.md` como limitación conocida.
- **Es de solo lectura, no duplica acciones.** Tocar una fila del Calendario navega a la
  pantalla real de esa actividad (`habitoDetalle`, `tareaDetalle`, `accionToma` para
  Medicamento con su horario puntual, `SALUD` para Cita —no tiene pantalla de detalle propia—,
  y editar Fecha importante) en vez de reimplementar marcar/editar ahí mismo — evita mantener
  dos lugares con la misma lógica de estados.
- **Entrada desde Hoy: siempre visible, no gateada por "hay algo".** A diferencia de "Ver mis
  fechas importantes"/"Ver mis notas" (que solo aparecen si ya hay al menos una), "Ver
  calendario" aparece siempre — un calendario vacío sigue siendo útil para planear. También se
  agregó como botón secundario en el estado vacío de Hoy ("Todavía no tienes actividades para
  hoy"), que hasta ahora no mostraba ningún enlace secundario.

## Diario — construido completo (2026-07-29)

Era el único ítem que quedaba entero en la sección 3 de `10-pendientes.md`. A diferencia de
Notas, ya tenía `EntradaDiarioEntity`/`EntradaDiario`/`EntradaDiarioDao` armados desde la
sesión base de Fase 0.1 (puro scaffolding para que la base compilara completa desde el día 1)
— pero **sin `espacioId` ni `propietario`**, los dos campos que todo el resto del modelo usa
para filtrar por espacio y auditar. Se agregaron ambos (rompe el esquema, `LulaDatabase` sube
de versión 10 a 11) y se completó repositorio + casos de uso + pantallas siguiendo el mismo
patrón que Notas/Fecha importante.

- **Vive detrás de Zona Privada, a diferencia de Notas.** El modelo `EntradaDiario` ya traía
  el campo `privacidad: Privacidad` diseñado desde la base, y tanto `02-pantallas.md` como la
  convención de `CLAUDE.md` ("todo dato sensible... nace con `privacidad: solo_yo`") dejan
  claro que el Diario debe quedar detrás del PIN/biometría. `CrearEntradaDiarioUseCase` fija
  `Privacidad.SOLO_YO` siempre — no es un campo que el usuario elija en el formulario, como
  tampoco lo es en la pantalla que especifica `02-pantallas.md`. Se generalizó el mecanismo de
  gate que hasta ahora solo protegía Finanzas: nueva ruta `ZONA_PRIVADA_GATE_DIARIO` (en vez
  de parametrizar `PrivacyGateScreen` con un destino genérico, que hubiera tocado el flujo de
  Finanzas ya probado) que reutiliza el mismo `PrivacyGateScreen` sin cambios y redirige a
  `DIARIO` tras desbloquear. Entrada nueva en el menú "⋮" ("📓 Diario"), junto a Ajustes y Mi
  círculo de cuidado — no en el menú "+" ni como enlace desde Hoy, porque una entrada nueva se
  crea desde dentro de la propia pantalla Diario (su "+ Nueva entrada"), no como captura
  rápida global. Queda pendiente en `10-pendientes.md` que Notas, pese a la misma convención
  de "privacidad: solo_yo" en el papel, **no** quedó gateada — se priorizó como captura rápida
  de uso frecuente en esta ronda, a revisar si el usuario la quiere detrás del gate también.
- **Área de vida: primer selector real de la app para esa tabla.** `AreaDeVida` es una tabla
  (7 filas sembradas en el primer arranque: Salud, Finanzas, Aprendizaje, Hogar, Trabajo,
  Familia, Personal/espiritual — ver `AsegurarDatosSemillaUseCase`), no un enum, pero hasta
  ahora ninguna pantalla de "Crear" la exponía como selector aunque `Meta`/`Actividad` ya
  tienen la FK. El selector de Diario (`FlowRow` de `FilterChip`, tocar de nuevo deselecciona)
  reutiliza `EspacioRepository.observarAreasDeVidaActivas()` tal cual, sin caso de uso nuevo —
  mismo patrón que ya usaba `CrearMetaScreen` para elegir el hábito vinculado a una meta.
- **Fecha editable con el mismo `DatePicker` que Fecha importante/Cita**, no un campo fijo a
  "hoy" — la sesión con el usuario dejó claro que quiere poder anotar un día distinto al
  actual (`02-pantallas.md`: "Fecha: hoy (editable)").
- **Sin fotos por ahora.** El modelo ya trae `fotos: List<String>` (mapeado a JSON, mismo
  helper `encodeStringList`/`decodeStringList` que ya existía para otras listas de texto), y
  la entidad Room ya tenía la columna `fotosJson`, pero no se construyó la UI de cámara/
  galería — mismo criterio que el dibujo diferido de Notas: es una pieza de trabajo aparte
  (permisos, `FileProvider`, selector, miniaturas) desproporcionada frente al resto del MVP de
  esta ronda. Anotado en `10-pendientes.md`.
- **Sin campo de "estado de ánimo"/mood.** Ni `02-pantallas.md` ni `03-vocabulario.md`
  mencionan uno — se respetó el modelo tal cual está especificado en vez de agregarlo sin que
  se pidiera.

## Lote de la sección 2 de pendientes (2026-07-29)

El usuario eligió (de una lista de ~15 ítems dispares de `10-pendientes.md`) 4 grupos para
esta ronda: gatear Notas detrás de Zona Privada, editar Metas, auto-bloqueo de Zona Privada
por inactividad, y un paquete de "cosas chicas" (fecha de completado de tarea, botón
Compartir seguimiento en Rutina/Meta, gating de Revisión semanal al día configurado). Se
construyeron los 6 ítems resultantes.

**1. Notas detrás de Zona Privada.** Reutiliza el mecanismo recién armado para Diario:
nueva ruta `ZONA_PRIVADA_GATE_NOTAS` → `PrivacyGateScreen` → `NOTAS` tras desbloquear. Los
3 puntos de entrada a Notas (enlace "Ver mis notas" en Hoy, opción "Nota" del menú "+")
pasan ahora por el gate — antes iban directo a la lista/editor sin PIN. Corrige la
inconsistencia anotada en `10-pendientes.md`: el modelo `Nota` no tiene campo `privacidad`
(a diferencia de `EntradaDiario`), pero la sesión con el usuario dejó claro que el gate en sí
(no el campo) es lo que importa acá.

**2. Fecha de completado de una Tarea.** Se investigó usar `historial_cambios` (la tabla de
auditoría) para esto, pero se descartó: `AccionAuditoria` no distingue "se marcó completada"
de cualquier otro `ACTUALIZAR`, así que habría que decodificar JSON de cada fila y filtrar por
transición de estado — fràgil, y las Tareas recurrentes generan varias filas de auditoría por
"vuelta" que hacen aún más ruidoso ese enfoque. Se optó por lo simple y confiable: columna
nueva `fechaCompletado: Long?` en `ActividadEntity`/`Actividad`, escrita en
`ActividadRepositoryImpl.marcarEstado()` (nueva query `actualizarEstadoYFechaCompletado`) —
se pone `ahoraEpochMillis()` al pasar a `CONFIRMADO`, se limpia a `null` en cualquier otro
estado. `LulaDatabase` sube a versión 12. Mostrado en `TaskDetailScreen` solo cuando
`estado == CONFIRMADO`.

**3. Botón "🤝 Compartir seguimiento" en Rutina y Meta.** Copia exacta del patrón ya usado en
Hábito/Tarea/Medicamento (`CompartirActividadDialog` + `InvitacionEnviadaDialog` +
`CompartirActividadUseCase`) — para Meta, `actividadId` del caso de uso en realidad recibe el
`metaId` (el campo es genérico `elementoId` en `SolicitudCompartir`, no una FK real a
`Actividad`, así que reutilizarlo para Meta no rompe nada). Cita queda pendiente porque
todavía no tiene pantalla de detalle propia (se edita/elimina desde "Mi salud").

**4. Revisión semanal: gating automático al día configurado.** No existía ninguna preferencia
de "día de revisión" en la app — se agregó `AjustesRepository.observarDiaRevisionSemanal()`
(DataStore, default 7=domingo, mismo patrón que el toggle de sonido ya existente) con su
selector (chips L-D) en Ajustes. `DateTimeUtils` suma `numeroDiaIsoDeHoy()` (wrapper público
de la función privada `numeroDiaIso` ya usada para hallar el lunes/domingo de la semana) y
`nombreDiaIso()`. `WeeklyReviewViewModel` compara `numeroDiaIsoDeHoy() >= diaConfigurado` — si
es antes de ese día, corta el `init` temprano (no calcula nada del resto de la semana) y
`WeeklyReviewScreen` muestra un `EmptyState` "se activa los {día}" en vez del formulario.
Como el formulario no es alcanzable antes del día configurado, `guardada` nunca puede ser
`true` prematuramente — no hace falta lógica extra para combinar ambos estados.

**5. Zona Privada: auto-bloqueo por inactividad.** Se midió tiempo en segundo plano, no un
timer corriendo todo el rato: `SesionPrivadaState` (ya `@Singleton`) se registra como
`DefaultLifecycleObserver` de `ProcessLifecycleOwner` (dependencia nueva,
`androidx.lifecycle:lifecycle-process`, mismo tren de versión que `lifecycle-runtime-ktx` ya
usado) — en `onStop` (app pasa a segundo plano) guarda el momento; en `onStart` (vuelve a
primer plano), si pasaron ≥3 minutos (dentro del rango 2-5 min pedido), llama a la nueva
`bloquear()`. Se prefirió este enfoque a un `delay()` corriendo en un scope propio porque un
timer en curso puede morir junto con el proceso si Android lo mata en segundo plano — medir
al volver es el patrón estándar de "PIN lock" en apps Android y no depende de que el proceso
siga vivo todo el tiempo. `AppViewModel` (el gate de arranque ya existente, `@Activity`-scoped
vía Hilt) expone `zonaPrivadaDesbloqueada` reenviando el `StateFlow` de `SesionPrivadaState`;
`LulaNavHost` lo colecta y, si se re-bloquea sola mientras el usuario sigue parado en una
pantalla de Zona Privada (`RUTAS_ZONA_PRIVADA`: Finanzas, Diario, Notas y sus sub-rutas), lo
saca a Hoy — sin esto la pantalla se quedaba abierta mostrando datos privados con el candado
ya cerrado.

**6. Metas: editar (nombre, área de vida, fecha límite).** `Meta`/`MetaEntity` ya traían
`areaDeVidaId`/`fechaLimite` desde la sesión base, y `MetaRepository.actualizar()` y
`ActualizarMetaUseCase` ya existían completos — pura scaffolding sin conectar, igual que pasó
con `EntradaDiarioEntity` antes de construir Diario. Se conectó: `CREAR_META` pasa de ruta fija
a `crear_meta?metaId={metaId}` (mismo patrón opcional que el resto de pantallas Crear/Editar
combinadas), `CrearMetaViewModel` pasa a cargar el estado inicial si hay `metaId`, y
`CrearMetaScreen` gana selector de área de vida (mismo patrón `FlowRow` que Diario) y de fecha
límite (mismo `DatePicker`, con un chip "Sin fecha límite" para poder dejarla en null). **A
propósito no se permite cambiar `comoSeMide` una vez creada la meta** — mezclar tipos de
medición a mitad de camino dejaría `valorActual`/el hábito vinculado en un estado
inconsistente (ej. pasar de "por hábito" a "manual" con `valorActual` calculado en vivo hasta
ese momento, ahora congelado); en modo edición se muestra como texto fijo en vez de chips.

## Diario: orden, calendario y entrada desde "+" (2026-07-29)

El usuario probó Diario y pidió 3 ajustes puntuales sobre lo ya construido:

- **Orden por fecha, no por inserción.** `EntradaDiarioDao.observarPorEspacio` ya ordenaba
  `ORDER BY fecha DESC` (el campo que el usuario elige, no cuándo se guardó la fila) — se
  agregó `id DESC` como criterio de desempate secundario (dos entradas del mismo día) para que
  el orden sea estable entre recomposiciones, pero el comportamiento pedido ("si cargo fechas
  salteadas o fuera de orden, se ven ordenadas igual") ya estaba cubierto por la consulta SQL
  desde que se construyó Diario.
- **Calendario mensual propio del Diario**, no el Calendario general — pantalla nueva
  (`DiaryCalendarScreen`/`ViewModel`) con la misma grilla de 7 columnas que `CalendarScreen`
  (mismo look, sin reusar el componente porque agrega un tipo de dato distinto: acá cada celda
  es "¿hay una entrada esta fecha?", no una lista de `ItemAgenda`). Se extrajeron 3 funciones
  puras que antes vivían privadas y duplicadas dentro de `CalendarViewModel` a
  `DateTimeUtils` (`agregarMeses`, `secuenciaFechas`, `rangoGrillaMes`) para que
  `DiaryCalendarViewModel` las reutilice sin repetir la aritmética de fechas — `CalendarViewModel`
  se dejó intacto (sigue con sus copias privadas) para no arriesgar una regresión en una
  función ya construida y probada por un cambio que no aporta nada visible al usuario.
  Los días con entrada se marcan con fondo `secondaryContainer` + 📝; hoy se marca con
  `primaryContainer` (tiene prioridad visual sobre "tiene entrada"); tocar un día con entrada
  abre esa entrada (la más reciente, si hubiera más de una — caso raro, no se construyó un
  selector para eso); tocar un día vacío abre el editor de una entrada nueva **con la fecha de
  ese día precargada**, no la de hoy. Para esto último, `DIARIO_ENTRADA` ganó un segundo
  parámetro opcional `fecha` (`diario_entrada?entradaId={entradaId}&fecha={fecha}`) que
  `DiaryEntryEditorViewModel` expone como `fechaPreset` — la pantalla lo usa como valor
  inicial del campo Fecha en vez de "hoy" cuando se llega desde el calendario en modo crear
  (en modo editar, el valor cargado de la entrada real siempre gana).
- **Diario en el menú "+", pero sin saltarse la clave.** Se agregó "📓 Diario" a
  `AddMenuSheet` (antes solo estaba accesible desde el menú "⋮"), pero su `onClick` navega a
  `ZONA_PRIVADA_GATE_DIARIO` — el mismo gate de siempre — en vez de ir directo a la lista,
  igual que ya se hizo con "Nota". Es un atajo de descubrimiento, no un bypass de seguridad:
  Diario sigue pidiendo PIN/biometría sin importar desde dónde se entre. `DIARIO_CALENDARIO`
  se sumó también a `RUTAS_ZONA_PRIVADA` en `LulaNavHost`, para que el auto-bloqueo por
  inactividad saque al usuario a Hoy si se re-bloquea sola estando ahí parado.

## Segunda tanda de la sección 2 de pendientes (2026-07-29)

Cuatro ítems más de `10-pendientes.md`, investigados en paralelo con subagentes antes de tocar
código (cada uno resultó tener una forma distinta de estar "a medio construir").

**1. Finanzas: filtro por otros periodos.** La capa de datos ya era genérica —
`FinanzasDao.observarEntrePeriodo`/`FinanzasRepository.observarMovimientosEntrePeriodo` nunca
asumieron "mes en curso", ese supuesto vivía solo en cómo `FinancesViewModel` los llamaba. Se
creó `FinancesHistoryViewModel` (antes `FinancesHistoryScreen` reusaba `FinancesViewModel`)
con navegación mes a mes (mismo patrón `anterior()`/`siguiente()`/`irAHoy()` que
`CalendarViewModel`, reutilizando `DateTimeUtils.agregarMeses` — ya público desde la ronda del
calendario del Diario). Deliberadamente **no** se tocó `FinancesScreen`: su widget "Este mes"
sigue fijo al mes en curso siempre, tal como lo pide `02-pantallas.md` — separar el ViewModel
evita que navegar el historial mueva ese widget sin querer. `inicioDeMesEpochMillis`/
`finDeMesEpochMillis` ganaron un parámetro `fecha: LocalDate = hoy()` (con default, no rompe
las llamadas existentes) para poder pedir el balance de cualquier mes, no solo el actual.

**2. Notificaciones: permiso de alarmas exactas + reintento de `POST_NOTIFICATIONS`.**
`RecordatorioScheduler` ya revisaba `canScheduleExactAlarms()` defensivamente (cae a una
alarma inexacta si no hay permiso, nunca falla) — lo que faltaba era un lugar para que el
usuario *actúe* sobre eso. Se agregaron `notificacionesPermitidas()`/
`alarmasExactasPermitidas()`/`abrirAjustesDeAlarmasExactas()` a `NotificationSettingsUtils.kt`
(mismo archivo que ya tenía `abrirAjustesDeNotificaciones`), y dos banners condicionales en
`SettingsScreen` (solo aparecen si falta el permiso correspondiente) que llevan a Ajustes del
sistema. Android no deja "reintentar" el diálogo nativo de `POST_NOTIFICATIONS` después de una
negación — la única vía real es Ajustes, así que eso *es* el reintento. Como estos permisos
solo cambian desde fuera de la app, `SettingsScreen` es la primera pantalla de todo el proyecto
en usar un `DisposableEffect` + `LifecycleEventObserver` (`ON_RESUME`) para re-revisarlos cada
vez que el usuario vuelve a la pantalla — antes nunca hizo falta este patrón en Lula.

**3. Metas "por monto": auto-completar sumando Finanzas.** "Ahorro" ya era una categoría fija
existente en `CategoriasFinanzas` (con normalización de mayúsculas/acentos/plural) y ya se
usaba para calcular "ahorrado este mes" en `FinancesViewModel` — no hubo que inventar nada,
solo generalizar ese mismo filtro a **todo el histórico**, no solo el mes en curso (se
reutilizó `ObtenerBalanceMesUseCase` con rango `0L..ahoraEpochMillis()`, ya que pese a su
nombre nunca impuso semántica de "mes" — eso vivía en quien lo llamaba, igual que en el punto
1). Mismo patrón que ya existía para `POR_HABITO` (progreso calculado en vivo, nunca leído de
`valorActual`): se agregó una rama `esPorMonto` en `MetaDetailViewModel.cargar()`. El botón
manual "+ Agregar progreso" ahora se oculta también para `POR_MONTO` (antes solo se ocultaba
para `POR_HABITO`) — no tendría sentido dejarlo si el número se recalcula solo y lo pisaría en
la próxima carga. Se hardcodeó la categoría `"Ahorro"` en vez de agregar un selector de
categoría a `Meta` — no hay campo para eso hoy, y el propio `08-decisiones-tecnicas.md` ya
había decidido que "Ahorro" es la única categoría de ahorro por ahora (ver entrada anterior
"Categorías de Finanzas + Ahorro destacado").

**4. Listas: historial de usos pasados.** Este era el único de los cuatro que, según una
decisión ya registrada en este documento, se había descartado a propósito antes por ser "mucho
trabajo de modelo de datos para el mismo beneficio inmediato" — pero ahora se pidió
explícitamente, así que se construyó. Nueva tabla `lista_ejecucion` (FK `CASCADE` desde
`lista`, `LulaDatabase` sube a versión 13): cada vez que se toca "🔄 Reiniciar lista", antes de
desmarcar todo se guarda una foto (`ListaEjecucionEntity.itemsJson`, lista de
`{texto, marcado}` serializada — mismo mecanismo `kotlinx.serialization` que ya usaban
`RecordatorioCita`/`ComidaRelacionada`) de cómo habían quedado los ítems. Nueva pantalla
`ListHistoryScreen` (ruta `lista_historial/{listaId}`), accesible con "📜 Ver historial de
usos anteriores" desde el detalle de la lista: cada fila muestra fecha + "X de Y completados",
y se puede tocar para expandir y ver qué ítems específicos quedaron marcados. A diferencia del
resto de `Lista`/`ListaItem` (que nunca tuvieron funciones `toEntity()`/`toDomain()` en
`Mappers.kt`, se construyen inline en `ListaRepositoryImpl`), `ListaEjecucion` sí las tiene —
se siguió el patrón general del archivo en vez de perpetuar la excepción original.

## Sonidos propios de Lula para recordatorios (2026-07-29)

El usuario grabó/consiguió 2 sonidos propios en `.wav` (carpeta `Sonidos/` en la raíz del
repo, fuera de `app/`) para reemplazar el tono del sistema: `lula_mensaje.wav` (nivel Sonido)
y `lula_alarma_gorrion_habla_ventana.wav` (nivel Alarma). Copiados a
`app/src/main/res/raw/` — Android soporta `.wav` para sonidos de notificación sin conversión
(no hace falta `.mp3`), y los nombres de archivo ya venían en snake_case válido para nombre de
recurso, así que se copiaron tal cual (el nombre elegido por el usuario para el de alarma
describe el propio sonido — un gorrión hablando en la ventana — coherente con el tono cálido y
no punitivo de la marca).

- **`NotificationChannels.crearCanales()`** pasa de usar
  `RingtoneManager.getActualDefaultRingtoneUri(...)` (tono del sistema, con `?.let` porque
  puede devolver null) a `setSound(uriRecursoRaw(context, R.raw.lula_mensaje), ...)` /
  `R.raw.lula_alarma_gorrion_habla_ventana` — un recurso empaquetado en el APK nunca es null,
  así que se simplificó sin el null-check.
- **El problema real a resolver era que Android nunca deja cambiar el sonido de un canal ya
  creado** — el propio comentario del archivo ya lo documentaba desde antes. Como el
  dispositivo de prueba ya había creado `recordatorios_sonido`/`recordatorios_alarma` con el
  tono de sistema en rondas anteriores, cambiar el código sin más no hubiera tenido efecto
  visible: Android ignora cambios de sonido sobre un canal existente. Se resolvió dándole
  sufijo `_v2` a los IDs de esos dos canales (Silencioso no cambia, no tiene sonido que
  reemplazar) — como son solo consumidos a través de `canalPara()`, ningún otro archivo tuvo
  que tocarse. `crearCanales()` además borra los canales `_v1` viejos con
  `deleteNotificationChannel()` antes de crear los nuevos, para que no queden huérfanos en
  Ajustes del sistema con el tono de sistema y una descripción desactualizada.
- Sigue pendiente (sin tocar en esta ronda): el nivel Alarma sonando en loop tipo despertador
  — hoy sigue siendo un disparo único, igual que antes de este cambio; cambiar qué sonido sí
  suena no resuelve cuántas veces suena. Ver `10-pendientes.md`.

## Nivel Alarma en loop hasta detenerla (2026-07-29)

El pendiente que había quedado anotado arriba mismo, en la misma sesión: el usuario probó el
sonido de alarma y pidió explícitamente que suene en loop hasta cortarla, "porque es alarma no
mensaje nomás". Se construyó el Service dedicado que ya se había anticipado como necesario.

- **Un `NotificationChannel` de Android solo reproduce su sonido una vez por notificación** —
  no hay forma de pedirle que repita. Por eso se sacó el sonido del canal Alarma por completo
  (`setSound(null, null)`, tercera vuelta de ese canal: tono de sistema → sonido propio de Lula
  una vez → sin sonido de canal — de ahí el sufijo `_v3`) y se creó `AlarmaSonidoService`, un
  `Service` en primer plano con su propio `MediaPlayer` (`isLooping = true`,
  `AudioAttributes.USAGE_ALARM`) que es quien controla el loop de principio a fin.
  `RecordatorioReceiver` lo arranca con `ContextCompat.startForegroundService()` justo después
  de publicar la notificación normal del recordatorio, solo para nivel `ALARMA`.
- **Foreground service, no un `MediaPlayer` suelto** — Android mata procesos en segundo plano
  agresivamente; sin `startForeground()` el loop podría cortarse solo a los pocos segundos.
  Requiere el permiso `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (nuevo en el manifiesto, más
  `android:foregroundServiceType="mediaPlayback"` en la declaración del `<service>` —
  obligatorio desde que `targetSdk` pasó de 33) y termina mostrando una segunda notificación
  mínima ("🔔 Alarma sonando") mientras el Service está vivo — es el costo inevitable de
  cualquier foreground service en Android, no un efecto colateral evitable.
- **3 formas de detenerla, todas terminan en el mismo `ACTION_DETENER`**: el botón
  "🔕 Detener alarma" (en la notificación del recordatorio y en la del Service), tocar la
  notificación del recordatorio para abrir la app (`MainActivity` ahora revisa un extra
  `EXTRA_DETENER_ALARMA` en `onCreate`/`onNewIntent` y manda el mismo `Intent` de detener), o
  deslizar la notificación para descartarla (`setDeleteIntent`). La notificación del
  recordatorio pasa a `setOngoing(true)` para que no se pueda perder por error con un swipe
  accidental sin querer cortar la alarma — aun así se dejó `setDeleteIntent` conectado por si
  algún fabricante permite descartar notificaciones "ongoing" igual.
- **`START_NOT_STICKY`** — si Android mata el proceso igual, el Service no se reinicia solo
  (evitaría un loop de alarma "zombie" sin notificación asociada visible). Cada llamada de
  `iniciar()` primero detiene cualquier reproducción anterior — si dos alarmas caen casi juntas,
  gana la última en vez de sonar las dos superpuestas.

## Tercera tanda de la sección 2 de pendientes (2026-07-30)

Últimos 4 ítems de esta ronda de `10-pendientes.md`, investigados en paralelo con subagentes.

**1. Pantalla de detalle de Cita + Compartir seguimiento.** `Cita` no tenía pantalla propia —
"Mi salud" iba directo a editar, y el borrado vivía inline ahí mismo. Se construyó
`CitaDetailScreen`/`CitaDetailViewModel` mirando exactamente el patrón ya usado por
Medicamento/Tarea/Rutina/Meta (nombre, fecha/hora, lugar, motivo, botones Editar/Eliminar +
"🤝 Compartir seguimiento", mismos 3 diálogos reusables). A diferencia de Medicamento, no tiene
concepto de "pausar/reanudar" ni historial — se omitieron esos bloques en vez de copiarlos sin
que aplicaran. `HealthScreen` perdió su `IconButton` de borrado inline y su `ConfirmarEliminarDialog`
propio (ahora viven en el detalle); `HealthViewModel.eliminarCita()`/`EliminarActividadUseCase`
se sacaron de ahí por quedar sin uso. El ítem "Cita" al tocarlo desde Hoy también pasa a abrir
el detalle en vez de mandar genéricamente a "Mi salud".

**2. Personalización del bottom bar.** `02-pantallas.md` especifica 7 opciones posibles para
las posiciones 2-4 (Asistente/Hábitos/Progreso/Finanzas/Tareas/Metas/Círculo de cuidado) — se
creó `OpcionBottomBar` (enum con `id`/`emoji`/`etiqueta`, más funciones de extensión
`rutaDeNavegacion()`/`color()`/`estaSeleccionada()`) en la capa de navegación, no en dominio:
`AjustesRepository` solo guarda el `id` como string plano (3 `stringPreferencesKey` nuevas,
mismo patrón que `dia_revision_semanal`), sin conocer rutas — dominio no debe depender de
navegación. `SettingsScreen` gana 3 filas de `FilterChip` (una por posición), mismo patrón que
el selector de día de Revisión semanal. Detalle de implementación no trivial: una función
`@Composable` privada separada (`ItemConfigurable`) que envolvía la llamada a
`NavigationBarItem` para las 3 posiciones hacía fallar la compilación con
`Unresolved reference 'NavigationBarItem'` — un problema de resolución del compilador de
Compose de este proyecto con esa forma específica de factorizar, no un error de sintaxis. Se
resolvió escribiendo las 5 llamadas a `NavigationBarItem` en línea, dentro de `LulaBottomBar`
directamente (mismo patrón que ya usaban Hoy y `+`, que sí compilaban) — con más líneas
repetidas pero sin el problema. Documentado en el propio archivo para que no se "limpie" por
error reintroduciendo la factorización rota.

**3. Pantalla "Progreso" unificada.** El mockup de `02-pantallas.md` trata "Progreso" y
"Revisión semanal" como una sola pantalla scrolleable (resumen arriba, formulario completo
abajo, unidos por "[Revisión semanal completa — ver abajo]"). En vez de fusionar el contenido
de `WeeklyReviewScreen` (gating por día configurado, reflexión, guardar) dentro de una pantalla
nueva —duplicando esa lógica ya construida y probada—, se optó por una versión más simple y de
menor riesgo: `ProgresoScreen` nueva, con las 4 métricas del mockup ("Tu semana": Cumplimiento,
🔥 Racha máxima, 📊 Constancia 30 días, Puntos esta semana) y un botón
"Revisión semanal completa →" que navega a la pantalla de Revisión semanal ya existente, sin
tocarla. Cumplimiento/Racha máxima reutilizan el cálculo exacto de `WeeklyReviewViewModel`
(ventana de la semana ISO en curso); Constancia reutiliza `ObtenerProgresoDeHoyUseCase.
calcularConstancia()` (la misma fórmula de Historial); Puntos esta semana es nuevo — suma de
`RegistroDiario.puntuacion` de los días cerrados de la semana. El único enlace que existía a
Revisión semanal (desde Hoy, "Ver mi revisión semanal") ahora apunta a Progreso en su lugar
("📊 Ver mi progreso") — Historial y Revisión semanal siguen intactas y alcanzables como
siempre, Progreso es la nueva puerta de entrada que las conecta.

**4. Diario: adjuntar fotos.** Sin librería de carga de imágenes en el proyecto — se agregó
Coil (`coil-compose`, la opción estándar para Compose, ninguna alternativa ya presente). Selector
de galería vía `ActivityResultContracts.PickMultipleVisualMedia` (Photo Picker del sistema,
sin pedir permiso de almacenamiento en la enorme mayoría de dispositivos, ni declarar
`READ_MEDIA_IMAGES` — a diferencia de la cámara, que hubiera necesitado `FileProvider` +
permiso `CAMERA` desde cero) — se descartó cámara por desproporcionado frente al resto de esta
ronda, mismo criterio que el dibujo diferido de Notas. Las fotos elegidas se copian a
almacenamiento interno de la app (`filesDir/diario_fotos/`, nuevo `core/utils/ImagenUtils.kt`)
en vez de guardar el `content://` tal cual — ese URI puede dejar de servir si el usuario borra
la foto original de su galería. Limpieza de archivos huérfanos en 2 puntos: al quitar una foto
de la lista en el editor (antes de guardar) y al actualizar una entrada (fotos que salieron de
la lista nueva respecto a la guardada), además de al eliminar la entrada completa
(`EntradaDiarioRepositoryImpl.eliminar`) — todo con `runCatching`, best-effort.

## Revertido el mismo día: adjuntar fotos en Diario (2026-07-30)

El usuario se tomó un momento fuera del flujo de construir features para pensar en el rumbo
general de la app, y decidió sacar esto — no dejarlo "para después", sacarlo del todo por
ahora. Razonamiento: Lula no tiene ni necesita multimedia para su propósito central (ayudar a
construir hábitos, ahorrar y llegar a metas), y una vez que exista sync real a la nube (hoy
100% local, sin backend — ver sección "Bloqueado por backend" de `10-pendientes.md`), texto ya
pesa lo suyo por usuario; imágenes pesarían mucho más y complicarían la administración de
almacenamiento a futuro. Ver memoria de proyecto guardada sobre este enfoque.

Se revirtió por completo lo agregado unas horas antes en la entrada de arriba: se sacó la
dependencia Coil (`gradle/libs.versions.toml` y `app/build.gradle.kts`), se borró
`core/utils/ImagenUtils.kt`, se sacó el parámetro `fotos` de `CrearEntradaDiarioUseCase`/
`ActualizarEntradaDiarioUseCase` (vuelven a su firma original), se sacó la limpieza de
archivos de `EntradaDiarioRepositoryImpl.eliminar`, y `DiaryEntryEditorScreen`/`ViewModel`
volvieron a la versión sin selector de fotos ni miniaturas. **No se tocó** el campo
`fotos: List<String>`/`fotosJson` del modelo/entidad de `EntradaDiario` — ya existía desde la
sesión base de Fase 0.1 antes de este feature, siempre queda vacío sin una UI que lo llene, y
no vale la pena una migración de esquema solo para sacar una columna nunca usada por nadie.

## Restos chicos de sección 2: Perfil, vínculo Tarea↔salud, Hoy reorganizado (2026-07-30)

Tras la decisión de enfoque "solo texto y tablas" de la entrada anterior, el usuario pidió
terminar los últimos restos chicos de la sección 2 de `10-pendientes.md` antes de pasar a Fase
1.5. Tres piezas, en orden:

**1. Mi perfil (horarios de comida).** Toda la lógica de guardado ya existía de punta a punta
(`UsuarioRepository.actualizarHorariosComida`, `ActualizarHorariosComidaUseCase`) — solo estaba
alcanzable de paso, dentro de "Crear medicamento". `02-pantallas.md` separa el menú "⋮" en
CUENTA (Mi perfil · Cerrar sesión · Eliminar cuenta) y CONFIGURACIÓN (Ajustes ya construido:
notificaciones, sonido, revisión semanal, bottom bar) — se creó `features/profile/` como
pantalla nueva y separada en vez de sumar una sección más a `SettingsScreen`, siguiendo esa
separación del mockup. Nombre/correo se muestran de solo lectura (sin onboarding real todavía,
usuario semilla local). Importante para el usuario: cambiar un horario acá **no** recalcula
`horariosCalculados` de medicamentos ya creados — esos quedan con la foto de horarios que tenían
al guardarse, hay que reabrir y volver a guardar cada uno para que tomen el horario nuevo (se
avisa en la propia pantalla).

**2. Vínculo Tarea↔Medicamento/Cita, con cierre automático.** La primera respuesta del usuario
pedía solo una referencia informativa ("Vinculada a: X"), pero al pedir una segunda opinión
sobre el alcance, el usuario aclaró que necesita algo más: la Tarea debe **cerrarse sola**
cuando el Medicamento/Cita vinculado ya cumplió su ciclo de vida (caso real: "cuidar a alguien
por un tiempo" — la tarea de cuidado no debe quedar pendiente para siempre después de que el
tratamiento o la cita ya pasaron). Implementación:
- `ActividadDetalle.Tarea.actividadVinculadaId: String?` nuevo, mismo patrón que
  `Meta.actividadesVinculadasIds` (Meta↔Hábito) pero como campo simple en vez de tabla de unión
  aparte, porque acá la cardinalidad es 1 Tarea → como máximo 1 actividad vinculada (al revés,
  varias Tareas sí pueden apuntar al mismo Medicamento/Cita — "comprar la medicina", "llamar
  para confirmar la cita" — por eso el lookup inverso devuelve una lista).
- `tarea_detalle` gana la columna `actividadVinculadaId` con una segunda `ForeignKey` hacia
  `ActividadEntity`, `onDelete = SET_NULL` (a propósito distinto del `CASCADE` de
  `actividadId`): si se borra el Medicamento/Cita vinculado, la Tarea no debe desaparecer con
  él, solo perder el vínculo. `LulaDatabase` versión 13 → 14.
- "Ciclo de vida terminado" se define por tipo: Cita, cuando `fechaHora` ya pasó; Medicamento,
  cuando `fechaFin` no es null y ya pasó (un Medicamento sin fecha de fin es indefinido, nunca
  cierra la Tarea solo por eso).
- `CerrarTareasVinculadasVencidasUseCase` hace el chequeo — sin WorkManager ni tarea en segundo
  plano, se corre una vez cada vez que se abre Hoy (`HomeViewModel.init`), barato porque solo
  mira Tareas que tienen vínculo y todavía no están `CONFIRMADO`. Reutiliza
  `MarcarActividadUseCase` para el cierre, así que si la Tarea vinculada fuera además recurrente
  seguiría el mismo camino que marcarla a mano (avanza a la siguiente fecha en vez de quedar
  cerrada) — combinación posible pero no es el caso de uso típico de este vínculo.
- Selector en `CrearTareaScreen` (chips "Sin vincular" + un chip por Medicamento/Cita del
  espacio, mismo patrón `FlowRow` que el selector de Hábito de `CrearMetaScreen`) solo aparece
  si hay al menos un Medicamento o Cita creados. Se muestra en ambos sentidos:
  `TaskDetailScreen` dice "🔗 Vinculada a: X"; `MedicamentoDetailScreen`/`CitaDetailScreen`
  listan sus "📝 Tareas vinculadas" (`ObtenerTareasVinculadasUseCase`, nuevo).

**3. Hoy: pendientes primero, completados aparte.** Pedido explícito del usuario tras ver el
vínculo con cierre automático: que lo pendiente resalte y lo ya hecho no estorbe visualmente,
sin que desaparezca ni se sienta un castigo (principio transversal del producto). Se evaluaron
dos formas de "ver otros días" que el usuario mencionó (pestaña "mañana" nueva, o el Calendario
ya existente) — se optó por el Calendario (Vista Día/Semana/Mes con ◀/▶/Hoy, ya construido) para
no duplicar esa función con una pestaña nueva. El cambio en Hoy quedó acotado a la capa de
presentación: `HomeScreen.kt` filtra localmente qué mostrar (pendientes en sus secciones de
siempre por momento del día; completados — hábitos, tareas y tomas de medicamento resueltas —
movidos a una sola sección nueva "✅ Ya hechos hoy (n)" al final, colapsada por defecto,
expandible, con los mismos `Checkbox`/`TomaAccionRow` de siempre para poder desmarcar). No se
tocó `HomeViewModel` ni `HomeUiState` — siguen exponiendo las listas completas sin filtrar, el
recuento de "Progreso de hoy" tampoco cambió. Al desmarcar algo dentro de "Ya hechos hoy", el
mismo estado derivado hace que reaparezca arriba, en su sección de pendientes, sin lógica
adicional.

## Fase 1.5 — base local de Familia/Equipo (2026-07-30)

Investigación previa a construir: `Plan/04-roadmap-fases.md` define Fase 1.5 con 7 piezas
(crear espacio, invitar miembros, selector de espacio, tareas del hogar con múltiples
responsables, retos familiares, calendario compartido, gastos compartidos, roles admin/
miembro), pero el modelo de datos (`EspacioEntity`, `EspacioMiembroEntity`,
`RetoFamiliarEntity`, `RetoFamiliarParticipanteEntity`) ya existía desde Fase 0.1 sin ninguna
lógica ni pantalla — "CRUD mínimo... sin lógica de negocio", literalmente el estado en que se
dejó a propósito. Se le presentó al usuario el mismo criterio ya usado en Fase 1.0 (Círculo de
cuidado): construir solo lo que es 100% dato propio, sin simular una segunda persona — y eligió
construir esa base local ahora.

**Qué se construyó:**

- `EspacioRepository` ganó métodos genéricos (`crearEspacio`, `observarEspaciosDeUsuario`,
  `obtenerEspacioSiEsMiembro`, `observarMiembros`) — `crearEspacioPersonal` (usado por
  `AsegurarDatosSemillaUseCase`) ahora delega en `crearEspacio`, mismo comportamiento, sin
  duplicar lógica.
- **Espacio activo conmutable**: antes `ObtenerSesionActualUseCase` resolvía `espacioId`
  siempre al espacio Personal, hardcodeado. Ahora `AjustesRepository` guarda un
  `espacio_activo_id` opcional (DataStore, mismo patrón que `bottomBarPosicionN`) y
  `ObtenerSesionActualUseCase` lo usa si existe, **validando siempre** que el usuario siga
  siendo miembro de ese espacio (`obtenerEspacioSiEsMiembro`) antes de confiar en él — si el
  espacio ya no existe o el usuario dejó de pertenecer, cae de nuevo a Personal en vez de
  fallar. Como casi todas las pantallas ya resuelven `espacioId` llamando a
  `ObtenerSesionActualUseCase` (Hoy, Tareas, Metas, Rutinas, Listas, Medicamentos, Citas,
  Notas, Fechas importantes, Finanzas, Calendario), cambiar el espacio activo re-escopea toda
  la app sin tocar ninguna de esas pantallas — el único cambio real está en el punto único de
  resolución de sesión, exactamente la razón de ser de ese caso de uso.
- **`CrearEspacioFamiliaUseCase`**: crea el `Espacio` tipo FAMILIA con el usuario actual como
  único `EspacioMiembro` ADMIN, y de una lo deja como espacio activo — no tendría sentido
  crearlo y seguir viendo Personal.
- **Retos familiares**: `RetoFamiliarRepository`/`RetoFamiliarRepositoryImpl` nuevos (no
  existían). Se agregó `RetoFamiliarRegistroEntity` (mismo patrón que
  `RegistroActividadEntity`: una fila por participante/reto/día, estado
  `CONFIRMADO`/`SIN_CONFIRMAR` reusando `EstadoActividad`) porque `RetoFamiliarParticipanteEntity`
  solo modela membresía estática, no cumplimiento diario — sin esa tabla nueva no había forma
  de calcular "x de y ya cumplieron hoy" del mockup. `LulaDatabase` versión 14 → 15.
  `RetoFamiliar.frecuencia` pasó de reusar (mal) `FrecuenciaHabito` —que ni siquiera tiene
  `SEMANAL`— a un enum nuevo y deliberadamente separado `FrecuenciaRetoFamiliar { DIARIA,
  SEMANAL }`, mismo criterio que `RecurrenciaTarea` vs `Recurrencia`.
- **UI** (`features/family/`): `FamiliaScreen` (crear espacio Familia si no existe, listar
  espacios del usuario como chips seleccionables para cambiar de espacio activo — navega de
  vuelta a Hoy con `popUpTo(HOY){inclusive=true}` para forzar que `HomeViewModel` se recree
  con el nuevo `espacioId`, mismo patrón que el re-lock de Zona Privada—, mostrar miembros del
  espacio Familia), `RetosFamiliaresScreen` (lista con progreso + checkbox "Yo ya cumplí hoy"),
  `CrearRetoFamiliarScreen` (nombre/objetivo/frecuencia/recompensa + selector de participantes,
  oculto si solo hay un miembro real). Alcanzable desde "⋮" → "👨‍👩‍👧 Familia / Espacios"
  (`LulaTopBar`, sección CONFIGURACIÓN del menú). Hoy muestra "👨‍👩‍👧 Hoy en {espacio}" (con
  link "Cambiar") cuando el espacio activo no es Personal, siguiendo el mockup "Hoy en
  {espacio}" de `02-pantallas.md`.

**Qué se dejó deliberadamente afuera** (mismo criterio que "Personas que acompaño" en Círculo
de cuidado — nunca simular una segunda persona real):
- Invitar miembros de verdad — necesita el mismo directorio de cuentas que no existe hoy.
- Que aparezca un segundo `EspacioMiembro` real, roles admin/miembro con sentido (hoy todos
  son admin porque solo hay un miembro), progreso de un Reto familiar con más de un
  participante real, calendario/gastos "compartidos" mostrando más de una persona.
- Selector "Responsables" (checkboxes de miembros) y "Se completa cuando: Cualquiera/Todos"
  en Tareas del hogar — con un solo miembro real no hay nada que elegir todavía; se deja para
  cuando exista la invitación real, junto con esa misma pieza.
- Solo un espacio Familia por usuario en esta pasada (no equipo/varios espacios familiares) —
  simplifica bastante la UI de `FamiliaScreen` sin perder nada útil hoy.

## Ronda de feedback tras probar Fase 1.5 en dispositivo real (2026-07-30)

El usuario probó la base local de Fase 1.5 recién construida y reportó 7 puntos. Los primeros
4 son directamente sobre cómo se sintió el cambio de espacio; los últimos 3 son bugs de Hoy/
Tareas/Metas/Revisión semanal que no tienen que ver con Fase 1.5 pero salieron a la luz en la
misma ronda de prueba.

**1. Espacio activo persistido en disco → confusión real.** El usuario cambió a Familia, cerró
la app y la volvió a abrir esperando ver Personal de nuevo — como seguía en Familia, pensó por
varios minutos que sus datos personales se habían borrado. La causa: `AjustesRepositoryImpl`
guardaba `espacio_activo_id` en DataStore (persistente entre reinicios), igual que el resto de
sus preferencias. Se sacó de DataStore y se reemplazó por un `MutableStateFlow` privado dentro
del mismo singleton (`AjustesRepositoryImpl` ya está `@Singleton`, ahora explícito con la
anotación) — dura lo que dura el proceso: sobrevive navegando dentro de la app, pero se
resetea solo la próxima vez que se abre. Nunca hacer esto con una preferencia de verdad
duradera (sonido, bottom bar, etc.) — acá es intencional porque el usuario espera que
"cerrar la app" sea equivalente a "volver a mi espacio de siempre".

**2. Indicador de espacio activo invisible fuera de Hoy.** La banda "estás en Familia" solo
vivía en `HomeScreen`, con el mismo color que el resto de la UI — al navegar a otra pantalla
(Tareas, Finanzas) desaparecía todo indicio, y el usuario se "perdía" pensando que ya no había
forma de volver. Se movió a `LulaTopBar` (visible en cualquier pantalla, mismo nivel que racha/
gastos) con un color nuevo y exclusivo, `LulaFamiliaContainerLight/Dark` (turquesa — ningún otro
uso en la app, a propósito, para que sea inconfundible). `TopBarStatsViewModel` ahora también
resuelve el nombre del espacio activo (mismo patrón que ya tenía para la racha) y se refresca
en cada cambio de pantalla (`refrescar()`, ya se llamaba desde `LaunchedEffect(currentRoute)`).
Tocar la banda navega a "Familia / Espacios" para cambiar. El indicador de `HomeScreen` se
retiró (quedaría duplicado) — Hoy conserva en su lugar el aviso de "pendientes en Familia"
(punto 5), que es un mensaje distinto (no "dónde estoy", sino "algo necesita tu atención"),
con el mismo color para mantener el lenguaje visual consistente.

**3. Notas/Diario no deberían "seguir" al espacio activo.** El usuario dudó si Notas, Diario y
Finanzas debían vivir en Familia. Criterio aplicado: Notas y Diario son privados por
naturaleza (ya viven detrás de Zona Privada/biometría) — no tiene sentido que cambien de
contenido según el espacio activo, siempre deben ser los del usuario. Finanzas, en cambio, si
sigue al espacio activo a propósito: el roadmap de Fase 1.5 pide explícitamente "gastos
compartidos" reusando `FINANZAS` con `espacio_id = familia`, y ya funcionaba así antes de esta
ronda. Implementación: `SesionActual` ganó un segundo campo, `espacioPersonalId` (siempre el
Personal, resuelto en `ObtenerSesionActualUseCase` igual que antes), separado de `espacioId`
(el activo). Los 5 ViewModels de Notas/Diario (`NotesListViewModel`, `NoteEditorViewModel`,
`DiaryListViewModel`, `DiaryCalendarViewModel`, `DiaryEntryEditorViewModel`) pasaron de usar
`sesion.espacioId` a `sesion.espacioPersonalId`. El resto de la app (Hábitos, Tareas, Metas,
Rutinas, Listas, Calendario, Finanzas, Medicamentos, Citas, Fechas importantes) sigue usando
`espacioId` sin cambios — esas sí tiene sentido que se re-escopeen con el espacio activo.

**4. Sin forma de editar/borrar el espacio Familia.** `EspacioRepository` ganó
`renombrarEspacio`/`eliminarEspacio`. Renombrar es trivial (`UPDATE espacio SET nombre`).
Eliminar es el punto delicado: `ActividadEntity`/`MetaEntity`/`ListaEntity`/`FinanzasEntity`
tienen FK `onDelete = RESTRICT` hacia `EspacioEntity` **a propósito** (evita borrar un espacio
con datos por accidente en el resto de la app) — así que había que limpiar esas 4 tablas a
mano, en orden, antes de poder borrar la fila de `espacio`. Se agregó una query
`eliminarPorEspacio(espacioId)` a cada uno de esos 4 DAOs, orquestadas dentro de
`LulaDatabase.withTransaction { }` en `EspacioRepositoryImpl.eliminarEspacio` (dependencia
nueva: `androidx.room:room-ktx`, ya estaba en el proyecto). `espacio_miembro` y
`reto_familiar` (+ sus tablas hijas) sí tienen `onDelete = CASCADE` hacia `EspacioEntity`
desde que se crearon, así que esas se limpian solas al borrar `espacio`. Notas/Diario no
necesitan limpieza porque, tras el punto 3, nunca llegan a tener filas en un espacio que no
sea Personal. `EliminarEspacioFamiliaUseCase` además limpia `espacio_activo_id` si apuntaba al
espacio borrado, para no quedar apuntando a un id que ya no existe. UI en `FamiliaScreen`:
"✏️ Renombrar"/"🗑️ Eliminar" con `ConfirmarEliminarDialog` (mismo patrón que el resto de la
app), listado explícito de qué se pierde (tareas, hábitos, metas, listas, movimientos,
retos).

**5. Sin forma de saber si hay algo pendiente en Familia desde Personal.** El usuario planteó
si Hoy debería mostrar junto lo pendiente de ambos espacios, o si cambiar de espacio debería
ser solo para administrar. Se optó por un término medio, más chico que fusionar Hoy de los dos
espacios (que sería un cambio de arquitectura más grande — ver nota en `10-pendientes.md`): un
aviso en Hoy Personal, "👨‍👩‍👧 Tienes N pendiente(s) en tu espacio Familia" (mismo color
turquesa que la banda del punto 2), visible solo si existe un espacio Familia y tiene hábitos/
tareas de hoy sin confirmar. `HomeViewModel` reutiliza `ObtenerActividadesDeHoyUseCase` sobre
el `espacioId` de Familia (no el activo) para contar, y `esTareaDeHoyOVencida` (ya existía)
para el mismo criterio de "tarea de hoy" que usa el resto de Hoy. Tocar el aviso navega a
Familia (no cambia de espacio automáticamente — el usuario decide si quiere cambiarse).

**6. Fecha límite de Tarea/Meta invisible fuera del formulario de editar.** `TareaListItemUi`
ya traía `fechaLimite` desde antes, pero `FilaTarea` en `TasksListScreen` nunca la mostraba —
ahora se ve debajo del nombre ("📅 {fecha}"), en rojo (`MaterialTheme.colorScheme.error`) si
está vencida y la tarea no está completada. `TaskDetailScreen` tampoco mostraba la fecha límite
en ningún lado (ni siquiera en el detalle) — se agregó igual, con "(vencida)" en el texto.
`MetaListItemUi` ganó el campo `fechaLimite` (ya existía en `domain.model.Meta`, solo faltaba
pasarlo a la UI) y se muestra en `GoalsListScreen` debajo del progreso. Citas y Fechas
importantes ya mostraban su fecha en sus listas — no necesitaron cambios.

**7. Hoy no mostraba Citas ni Fechas importantes programadas para hoy.** Un hallazgo real: si
hoy tocaba una cita o una fecha importante, no aparecía en ningún lado de Hoy — solo en "Mi
salud"/"Fechas importantes" por separado, y el usuario tenía que acordarse de entrar ahí. En
vez de reimplementar el cálculo de recurrencia de Fecha importante (que ya existe y está
probado en `ObtenerAgendaDelRangoUseCase`, el motor del Calendario), `HomeViewModel` ahora
llama a ese mismo caso de uso con un rango de un solo día (hoy) y filtra por
`TipoActividad.CITA`/`FECHA_IMPORTANTE`. `HomeUiState` ganó `citasDeHoy`/
`fechasImportantesDeHoy: List<ItemAgenda>` (reutiliza el modelo del Calendario tal cual, mismo
precedente que `medicamentosDeHoy: List<MedicamentoDeHoy>`). Nuevas secciones de solo lectura
en `HomeScreen` (sin checkbox, a diferencia de Hábitos/Tareas — tocar navega al detalle en vez
de "completarse" desde Hoy).

**Aparte — Revisión semanal sin confirmación de guardado.** El usuario llenó "Tu semana",
tocó "Guardar" varias veces sin ver ningún cambio visible (el botón solo cambia de texto a
"Actualizar revisión", nada más), y no sabía si había funcionado. `WeeklyReviewUiState` ganó
`guardadoExitoso: Boolean`, transitorio (distinto de `guardada`, que persiste "ya existe una
revisión guardada esta semana"), puesto en `true` justo al terminar `guardarRevision()`.
`WeeklyReviewScreen` ahora navega atrás (`onGuardada`) apenas eso pasa — mismo patrón que
`CrearTareaScreen`/`CrearMetaScreen`/etc., que ya navegan tras guardar; esta pantalla era la
excepción que se quedaba en el mismo lugar sin avisar.

## Roadmap desactualizado, Notas (título/orden), deuda técnica y Mi propósito (2026-07-30)

**Roadmap.** El usuario notó que `04-roadmap-fases.md` seguía mostrando Fase 1.5 con su spec
original (7 bullets sin marcar), sin el bloque "Estado al..." que sí tienen Fase 0.8/1.0 —
quedó desactualizado en la sesión anterior. Se corrigió con el mismo formato (✅/🟡/⬜ por
punto + párrafo de cierre), reflejando lo que ya existe (`08-decisiones-tecnicas.md`, entrada
"Fase 1.5 — base local de Familia/Equipo") vs. lo que sigue bloqueado por backend.

**Notas — título y orden manual.** `NotaEntity` ganó `titulo: String?` (si es null, la lista
sigue derivando el título de la primera línea de `contenido`, comportamiento anterior intacto)
y `orden: Int` (reemplaza el orden por `fechaEdicion DESC`). Reordenar es **solo flechas ▲▼**,
a pedido explícito del usuario ("lo más fácil y entendible posible") — se descartó
arrastrar/soltar porque ningún otro lado de la app lo usa y es más gesto para explicar sin
ganar nada. Mecánica: `NotesListViewModel.mover()` toma la lista ya ordenada que se ve en
pantalla, ubica el vecino inmediato (anterior si ▲, siguiente si ▼) e intercambia sus `orden`
— dos escrituras (`NotaRepository.actualizarOrden`, cada una auditada). Una nota nueva nace
con `orden = mínimo existente - 1` (aparece primera), calculado en `CrearNotaUseCase` vía
`NotaRepository.obtenerOrdenParaNota`.

**Deuda técnica — migraciones reales de Room.** Reemplaza `fallbackToDestructiveMigration`
(borraba toda la base en cada cambio de esquema). Se creó `core/database/Migrations.kt` con
`MIGRATION_15_16` (Notas: `ALTER TABLE nota ADD COLUMN titulo`/`orden`) y `MIGRATION_16_17`
(tabla nueva `proposito_personal`), registradas en `DatabaseModule` vía `.addMigrations(...)`.
**No se escribieron migraciones retroactivas para las versiones 1 a 15** — esa historia es
solo de dispositivos de prueba del propio usuario durante esta misma sesión de desarrollo, sin
datos reales de nadie que proteger; escribir 15 migraciones reconstruidas desde los JSON de
`app/schemas/` para un caso que nunca va a ocurrir en producción no tenía sentido. En su lugar,
`.fallbackToDestructiveMigrationFrom(dropAllTables = true, *(1..15).toList().toIntArray())`
deja el borrado disponible **solo** para quien todavía esté en una versión 1-15 — de la 16 en
adelante, un cambio de esquema sin su `Migration` real hace que la app truene al abrir. Es a
propósito: fuerza la disciplina de ahora en más, en vez de perderla de nuevo silenciosamente.

**Deuda técnica — test de `@Serializable` para `AuditLogger`.** `AuditLoggerSerializableTest`
(nuevo, `app/src/test/`) escanea el código fuente en texto plano — no reflection, no depende
del classpath/retention de la anotación en tiempo de ejecución. Dos pasadas: (1) recorre
`data/repository/*.kt` con el regex `auditLogger\.registrar<(\w+)>` para juntar qué entidades
se usan ahí; (2) recorre `data/local/entity/*.kt`, separa cada archivo por línea en blanco
(cada entidad vive en su propio bloque `@Serializable`? + `@Entity(...)` + `data class
Nombre(`, separado del resto por una línea vacía — patrón consistente en todo el proyecto) y
verifica que el bloque de cada entidad usada contenga `@Serializable`. Deliberadamente **no**
es una regla general "toda `@Entity` debe ser `@Serializable`" — entidades como
`HistorialCambiosEntity` (el propio log de auditoría), `EspacioMiembroEntity`,
`AreaDeVidaEntity`, `RutinaDetalleEntity`, `RetoFamiliarParticipanteEntity` o
`MetaActividadCrossRef` nunca se pasan a `auditLogger.registrar<T>()` hoy, así que forzarles
la anotación sería ruido sin ningún beneficio — el test solo exige la anotación donde
realmente hace falta, seleccionable dinámicamente según el uso real, no una lista fija.

## Mi propósito (Misión/Visión/Propósito) — 2026-07-30

Construido siguiendo la estrategia guardada el mismo día en `10-pendientes.md` (preguntas
aportadas por el usuario, ya probadas por él mismo para armar sus propias metas).

- **Modelo**: `PropositoPersonal(espacioId, propietario, respuestas: Map<String, String>,
  fechaEdicion)` — `respuestas` es un mapa `id de pregunta → texto`, guardado como JSON
  (`respuestasJson`, mismo patrón que `responsablesJson`/`horariosCalculadosJson` en otras
  entidades) en vez de 13 columnas nullable, para no tener que tocar el esquema si algún día
  se agregan o quitan preguntas. `PropositoPersonalEntity` tiene `@PrimaryKey val espacioId`
  (una fila por espacio, upsert siempre sobre la misma) — nunca hace falta un id generado ni
  buscar "la" fila de alguien, siempre es la de su espacio.
- **Siempre el espacio Personal**, igual que Notas/Diario (`sesion.espacioPersonalId`, nunca
  `espacioId`) — es sobre la persona, no sobre el espacio activo; no tendría sentido que
  "cambiara" al entrar a Familia.
- **13 preguntas fijas** en `domain/model/PropositoPersonal.kt`
  (`PREGUNTAS_PROPOSITO`) — 7 de Propósito (reflexión: qué me apasiona, valores, habilidades,
  qué quiero lograr, qué tipo de vida, qué impacto, qué me hace feliz) y 6 de Visión (qué
  quiero hacer/ser/ver/tener, adónde ir, qué deseo compartir), más `CONSEJO_REDACCION_VISION`
  (presente, afirmativas, "yo") que se muestra solo en la sección Visión, nunca en Propósito.
- **No arma un párrafo narrativo** a partir de las respuestas — las muestra tal cual,
  ordenadas por pregunta, cada una con su propia edición. Redactar una síntesis en primera
  persona a partir de respuestas sueltas necesitaría generación de texto (un LLM/asistente),
  que todavía no existe (Fase 2.0) — anotado en `10-pendientes.md` como candidato natural para
  esa fase, no un vacío de esta.
- **UI**: `PropositoPersonalScreen` (alcanzable desde Mi perfil → "🧭 Mi propósito") lista las
  13 preguntas en dos secciones con progreso ("X de 13 respondidas") y una barra; cada fila
  muestra la respuesta actual o "Sin responder — toca para escribir". Tocar navega a
  `EditarRespuestaPropositoScreen` (una sola pregunta, un `DictationTextField`, Guardar) — el
  patrón real terminó siendo "lista resumen + pantalla de edición de a una" en vez del stepper
  de una-pregunta-por-pantalla-con-flechas-siguiente/atrás que se había descrito en
  `10-pendientes.md`; logra lo mismo (progresivo, resumible, de a una) sin construir un
  componente de stepper nuevo que no existe en ningún otro lado de la app.
- `LulaDatabase` versión 16 → 17 (`MIGRATION_16_17`, tabla nueva).

## Bug real: la app no abría tras las migraciones (2026-08-01)

**Síntoma reportado por el usuario**: desinstaló la app pensando que el problema era de
instalación; al reinstalarla desde cero, seguía sin abrir — pantalla en blanco, error, se
cerraba sola.

**Diagnóstico**: en vez de adivinar, se conectó el dispositivo real del usuario por `adb` y se
leyó el log de crash directamente (`adb logcat`, filtrando `FATAL EXCEPTION`). El error exacto:

```
java.lang.IllegalArgumentException: Inconsistency detected. A Migration was supplied to
addMigration() that has a start or end version equal to a start version supplied to
fallbackToDestructiveMigrationFrom(). Start version is: 15
```

**Causa**: al armar la deuda técnica de migraciones (2026-07-30, entrada de arriba),
`DatabaseModule.provideLulaDatabase` quedó con `.fallbackToDestructiveMigrationFrom(dropAllTables
= true, *(1..15).toList().toIntArray())` — un rango que **incluía la versión 15** — mientras
`MIGRATION_15_16` también empieza en la versión 15. Room valida esto al construir la base
(`RoomDatabase.Builder.build()`) y lo rechaza como inconsistencia: un mismo número de versión no
puede estar a la vez "cubierto por una migración real" y "permitido para borrar y recrear". No
es un error de compilación — Kotlin/Room no lo detectan hasta que la app corre de verdad, por
eso compilaba y pasaba `assembleDebug` sin ningún aviso, pero crasheaba al abrir en cualquier
dispositivo (nuevo o viejo, instalación limpia o no — el error ocurre construyendo el objeto
`LulaDatabase`, antes de tocar ninguna fila).

**Corrección**: el rango de `fallbackToDestructiveMigrationFrom` pasó de `1..15` a `1..14` —
regla general para no repetir esto: **el límite superior de ese rango siempre debe terminar un
número antes de la versión de inicio de la migración real más vieja registrada**. Cada vez que
se agregue una migración nueva con una versión de inicio más vieja que la actual, hay que
revisar este rango también.

**Verificación**: no alcanza con que compile — se instaló el APK de verdad en el dispositivo
del usuario (`gradlew installDebug`) y se abrió la app vía `adb shell am start`, confirmando
por `adb logcat` que el proceso queda corriendo sin `FATAL EXCEPTION`. Lección para la
disciplina de este proyecto: un cambio en `DatabaseModule`/migraciones de Room es exactamente
el tipo de cambio que "compila bien" pero puede fallar solo en tiempo de ejecución — verificar
con la app corriendo de verdad (dispositivo/emulador conectado), no solo con
`compileDebugKotlin`/`assembleDebug`, cuando se toque esa zona del código.

## Mi propósito — borrable (2026-08-01)

Faltaba la parte de "borrable" del acuerdo original (2026-07-30). Se agregó:
- `PropositoPersonalRepository.eliminarRespuesta(espacioId, propietario, preguntaId)` — saca
  esa sola clave del mapa `respuestas` y re-guarda (el resto de respuestas queda intacto).
- `PropositoPersonalRepository.eliminarTodo(espacioId, propietario)` — borra la fila completa
  (`DELETE FROM proposito_personal`).
- UI: `EditarRespuestaPropositoScreen` ganó un botón "Borrar" (solo visible si esa pregunta ya
  tiene respuesta) junto a "Guardar"; `PropositoPersonalScreen` ganó "🗑️ Borrar mi propósito"
  (solo visible si hay al menos una respuesta) arriba de las secciones. Ambos con
  `ConfirmarEliminarDialog`, mismo patrón de confirmación que el resto de la app — nada se
  borra sin que el usuario confirme explícitamente qué va a perder.

## Mi propósito — preguntas corregidas, ayuda de Metas separada, tabla de planes (2026-08-01)

El usuario revisó las preguntas que había compartido originalmente y corrigió cómo se habían
mapeado — quedaron mezcladas: las 7 preguntas "personales" no eran de Propósito solo, y las 6
de "armar objetivos" no eran de Visión — en realidad eran ayuda para Metas, algo completamente
distinto.

**1. Preguntas de Mi propósito, corregidas.** `PREGUNTAS_PROPOSITO` pasó de 13 preguntas en 2
secciones (Propósito 7 / Visión 6) a **8 preguntas en 2 secciones nuevas**:
`SeccionProposito.MISION_VISION` (las 7 "personales": apasiona, valores, habilidades, qué
quiero lograr, tipo de vida, impacto, feliz — arman Misión y Visión **juntas**, no cada una por
su lado) y `SeccionProposito.PROPOSITO` (1 pregunta nueva, sugerida por Claude y aceptada:
"¿Cuál siento que es mi propósito de vida?" — las 7 de autoconocimiento solas no garantizan
llegar al "para qué", por eso se sumó una pregunta directa). Se sacó `CONSEJO_REDACCION_VISION`
del dominio de Propósito por completo (no aplicaba ahí). Nota técnica: las respuestas viejas
guardadas con los ids de las 6 preguntas que se sacaron (`hacer`, `ser`, `ver`, `tener`,
`adonde_ir`, `compartir`) quedan huérfanas en el mapa `respuestas` de quien ya las hubiera
contestado — no se escribió limpieza para esto porque nadie tiene datos reales todavía (mismo
criterio que en la entrada de deuda técnica de arriba: no vale la pena una migración para un
caso que no existe en la práctica).

**2. Las preguntas de "armar objetivos" + consejos de redacción, movidas a Metas.** Vivían mal
ubicadas dentro de Mi propósito. Se sacaron de ahí (no eran del dominio `PropositoPersonal` en
absoluto) y se agregaron como texto de referencia, sin guardar nada, en `CrearMetaScreen`: una
sección colapsable ("💡 ¿No sabes cómo definirla? Ver ideas", cerrada por defecto para no
ensuciar el formulario) con las 6 preguntas ("¿Qué quiero hacer/ser/ver/tener?", "¿Adónde
quiero ir?", "¿Qué deseo compartir?") y los 3 consejos de redacción (presente, afirmativas,
"yo"). Deliberadamente **no** es un campo de formulario ni genera datos — es solo ayuda para
pensar, la persona sigue escribiendo el nombre de su Meta en el campo de siempre.

**3. Botón "Armar y presentar con IA" — deshabilitado a propósito.** El plan original decía
que, con las preguntas llenas, un botón mandaría todo a n8n para que arme y presente la
Misión/Visión/Propósito ya redactados. Antes de construir esa conexión real, se confirmó con
el usuario: **todavía no existe ningún workflow de n8n** — decidió esperar a terminar de
estabilizar el resto del modelo de datos (empezando por "usuarios pendientes", ver
`10-pendientes.md`) antes de armar n8n, para no tener que modificarlo dos veces. Tampoco existe
ningún cliente de red en la app hoy (decisión de Fase 0.1: sin Retrofit/OkHttp hasta que hiciera
falta de verdad — sigue sin hacer falta). Se agregó el botón igual, en `PropositoPersonalScreen`,
con `enabled = false` y el texto "🤖 Armar y presentar con IA (próximamente)" — dejando clara la
intención en la UI sin prometer algo que no funciona todavía. Cuando exista el workflow, esto
es lo que falta del lado de la app: un cliente HTTP (Retrofit/Ktor, a decidir), el `POST` con
las respuestas, mostrar el resultado, y manejar error/sin conexión.

**4. Modelo de negocio — tabla de planes con límite de IA.** `05-modelo-negocio.md` pasó de
listas con viñetas por plan a una sola tabla comparativa (Gratis / Premium Individual / Premium
Familia por fila de función) — más fácil de leer de un vistazo, mismo formato que las tablas de
precios que se ven en otras apps. Decisión nueva: "armar y presentar con IA" tiene su **propio**
límite en el plan Gratis (1-2 usos, después pide Premium), distinto del resto de "Mi propósito"
(las preguntas en sí siguen gratis e ilimitadas, porque guardar texto no cuesta nada). Motivo:
es la primera función de la app que va a costar dinero real por cada uso (llamada a un modelo de
lenguaje) — a diferencia de todo lo demás en el plan Gratis, que es local y no escala su costo
con la cantidad de usuarios. Se documentó también la idea de evaluar IA local (on-device) más
adelante para reducir la dependencia de pagar por llamada.

## Cuentas y conexiones — pasos 1 a 3 implementados (2026-08-01)

Siguiendo el orden de `11-cuentas-y-conexiones.md`, se construyó la parte local-first completa
(pasos 1-3 de ese documento; los pasos 4-5 — borradores legales y evaluar Firebase — siguen
pendientes, ver `10-pendientes.md`).

**1. `Usuario` ampliado + `MIGRATION_17_18`.** Tres campos nuevos: `confirmoMayorDe13: Boolean`
(default `false`), `terminosAceptadosEn: Long?`, `consentimientoDatosSaludEn: Long?` — mismo
patrón que el `privacidadAceptadaEn` ya existente. `UsuarioRepository.actualizarConsentimientos`
recibe los tres como parámetros nullable con default `null` (solo pisa el campo que se pasa,
igual que `actualizarHorariosComida`), auditado como `ACTUALIZAR`. Migración agrega las 3
columnas a `usuario` y crea la tabla `conexion` en el mismo paso (ver punto 2) — Room no exige
una migración por tabla, y separar hubiera sido una versión extra sin necesidad real.

**2. Tabla `Conexion` — schema listo, sin trigger activo.** `ConexionEntity`
(`usuarioA`/`usuarioB`/`tipo`/`origenSolicitudId`/`fechaConexion`), sin FK hacia `usuario`
—decisión deliberada: `usuarioB` va a ser casi siempre una persona distinta, cuyo id no existe
en la fila local de `usuario`; una FK ahí haría la tabla inutilizable para su propósito real.
`ConexionRepository.crearSiNoExiste(usuarioA, usuarioB, tipo, origenSolicitudId)` busca en
ambos sentidos antes de insertar (`ConexionDao.obtenerEntre`) para no duplicar el par. Igual
que el resto de "compartir", esto no tiene ningún disparador activo todavía (no existe flujo de
aceptar `SolicitudCompartir` — sigue bloqueado por Firebase) — es infraestructura construida por
adelantado, mismo criterio ya usado con Círculo de Cuidado y Espacio Familia.

**3. Sección "Privacidad y legal" en Mi perfil + "Eliminar mi cuenta".** `ProfileScreen` ganó
una sección con: checkbox "Confirmo que soy mayor de 13 años" (se puede marcar, no desmarcar —
una vez confirmado no tiene sentido dejar volver atrás), y dos filas de consentimiento
(Términos, Datos de salud) con botón "Aceptar" que desaparece y muestra la fecha una vez
aceptado — mismo patrón visual para el `privacidadAceptadaEn` ya existente, que ahora también
se muestra ahí aunque no tenga botón (ya se aceptó en la semilla). Debajo, una "Zona de
peligro" con "🗑️ Eliminar mi cuenta", protegida por `ConfirmarEliminarDialog` (mismo patrón de
confirmación de toda la app). `UsuarioRepository.eliminarCuenta()` llama
`LulaDatabase.clearAllTables()` — se descartó replicar el borrado en cascada manual que sí se
construyó para Espacio Familia (tocaría cada tabla de la app, no solo las de un espacio) porque
hoy es fundamentalmente una base de un solo usuario local; `clearAllTables()` es más simple y
igual de correcto para ese caso. Deliberadamente **no** auditado (ver comentario en el código:
la propia tabla de auditoría queda vacía después, no tiene sentido una fila "se borró todo" en
un historial que ya no existe). Tras borrar, `reiniciarApp()` (nuevo,
`core/utils/ReiniciarAppUtils.kt`) mata el proceso y relanza la Activity desde cero —
necesario porque ViewModels ya en memoria pueden tener cacheado un `usuarioId`/`espacioId` de
filas que `clearAllTables()` acaba de borrar; al reiniciar, `AppViewModel` vuelve a correr
`AsegurarDatosSemillaUseCase` y crea todo de nuevo.

**Verificación en dispositivo real** (obligatoria para cualquier cambio de Room, ver la lección
de la migración 15→16/2026-08-01): `installDebug` + `adb logcat` sin `FATAL`/`Exception` al
abrir; se probaron los tres consentimientos (checkbox + 2 botones "Aceptar") y se confirmó que
persisten después de `am force-stop` + relanzar (no solo en memoria). El diálogo de "Eliminar
mi cuenta" se probó hasta la confirmación (se canceló a propósito para no borrar los datos
reales del dispositivo de prueba) — el flujo de borrado en sí (`clearAllTables` +
`reiniciarApp`) no se ejecutó de punta a punta todavía porque hubiera borrado datos reales del
usuario; queda pendiente probarlo con datos descartables antes de considerar esto 100% cerrado.

## Cuentas y conexiones — paso 4: textos legales + pantalla para leerlos (2026-08-01)

Los botones "Aceptar" de Términos y Datos de salud (construidos en la entrada anterior)
guardaban la fecha sin mostrar ningún texto — legalmente débil (no tiene sentido "aceptar" algo
que no se puede leer) y además dejaba sin resolver el paso 4 de `11-cuentas-y-conexiones.md`
(borrador de los documentos legales). Se resolvieron los dos juntos.

**1. `domain/legal/TextosLegales.kt` (nuevo).** `TipoDocumentoLegal` enum (`PRIVACIDAD`,
`TERMINOS`, `DATOS_SALUD`) con `id`/`titulo`, y `TextosLegales.textoPara(tipo)` con el borrador
completo de cada documento (Política de Privacidad, Términos de Servicio, y un texto corto
específico de consentimiento de datos de salud). Los tres mencionan la Ley N° 29733 (Ley de
Protección de Datos Personales del Perú) y dejan placeholders entre `[corchetes]` para la
identidad legal real (nombre del responsable, correo de contacto, fecha de publicación) — **son
un punto de partida, no el texto final**; sigue en pie la recomendación de que alguien con
conocimiento legal real los revise antes de publicar en Play Store (ya señalado en
`11-cuentas-y-conexiones.md`).

**2. Pantalla `LegalTextScreen` + `LegalTextViewModel` (nuevo, `features/legal/`).** Ruta
`texto_legal/{tipo}` (`LulaDestinations.TEXTO_LEGAL`). Muestra el texto completo con scroll y,
debajo: si ya está aceptado, la fecha; si no, y el documento lo permite (`permiteAceptar` — solo
Términos y Datos de salud), un botón "Aceptar" ahí mismo. Política de Privacidad es de solo
lectura: se acepta una sola vez, en la semilla (`AsegurarDatosSemillaUseCase`), no hay acción de
usuario que la vuelva a aceptar — mostrarla acá es solo para que la persona pueda leerla cuando
quiera, no un nuevo flujo de consentimiento.

**3. `ProfileScreen` — de "Aceptar" directo a "leer primero".** Se sacó el atajo que aceptaba
sin mostrar texto (los métodos `aceptarTerminos`/`aceptarConsentimientoDatosSalud` se borraron
de `ProfileViewModel` — la lógica de aceptar ahora vive solo en `LegalTextViewModel`, un único
lugar en vez de duplicada en dos). Cada fila de `ConsentimientoRow` es ahora clickeable
completa (`Modifier.clickable`) y navega a `LegalTextScreen` con el `tipo` correspondiente,
mostrando "Leer y aceptar →" si está pendiente o "Ver →" si ya se aceptó — la política de
privacidad usa el mismo patrón de fila aunque no tenga botón de aceptar en la pantalla destino.
El checkbox de mayoría de edad (`confirmarMayorDe13`) se quedó como estaba, sin pantalla propia
— no hay ningún texto que leer ahí, es una confirmación de un solo hecho, distinta de aceptar un
documento.

Verificado: `compileDebugKotlin` y `installDebug` sin errores. La verificación visual completa
en dispositivo (abrir cada uno de los 3 textos, aceptar, confirmar que navega bien) quedó a
medias esta sesión — el celular de prueba pasó a estar en uso normal (otra app en primer plano)
mientras se probaba, y no tiene sentido seguir mandando comandos `adb` a un teléfono que la
persona está usando de verdad. Sin riesgo de regresión grande porque este cambio no toca Room
(no hay migración nueva, la versión de la base sigue en 18) — pero falta el click-through visual
antes de darlo por 100% cerrado.

## Ronda de feedback de uso real — 5 puntos (2026-08-05)

El usuario probó la app varios días y reportó 5 problemas concretos. Se investigó cada uno
leyendo el código real (no se adivinó ninguna causa) antes de tocar nada.

**1. Finanzas — no se podía registrar un gasto/ingreso de una fecha pasada.**
`MovimientoFinanciero.fecha` ya existía en el modelo, pero `RegistrarMovimientoUseCase` siempre
escribía `DateTimeUtils.ahoraEpochMillis()` a ciegas — no había ningún campo de fecha en
`CrearMovimientoScreen`. Se agregó un selector de fecha (mismo patrón `FilterChip` +
`DatePickerDialog` que ya usan Diario/Cita/Medicamento), y `fecha` ahora viaja por
`RegistrarMovimientoUseCase`/`ActualizarMovimientoUseCase`/`FinanzasRepository.actualizarMovimiento`
en vez de asumir "ahora". Sin migración — el campo ya estaba en la tabla.

**2. Medicamentos — calcular el fin por cantidad de dosis, no solo por fecha.** Causa raíz
encontrada en `calcularHorariosPorIntervalo`: los horarios de un medicamento son "las tomas de
un día completo" (ej. cada 8 horas = 3 tomas/día), y se repiten completos todos los días entre
`fechaInicio` y `fechaFin` — no hay forma de expresar "el último día solo cuenta 1 toma, no 3".
Por eso el ejemplo que dio el usuario (7am, cada 8h, 4 tomas recetadas) daba 6 tomas en vez de
4 si elegía manualmente el día 2 como fecha de fin: ese día traía sus 3 tomas completas, no solo
la primera. Arreglado con:
- `ActividadDetalle.Medicamento` gana `cantidadDosisTotal: Int?` (`MIGRATION_18_19`, versión 19).
- `HorariosMedicamentoUtils.calcularFechaFinPorCantidadDosis` calcula sola la fecha de fin
  contando cuántos días completos de `horariosCalculados` caben antes de la dosis N.
  `indiceUltimaDosisEnDiaFinal` calcula qué tomas del último día sí cuentan.
- `CrearMedicamentoUseCase`/`ActualizarMedicamentoUseCase`: si llega `cantidadDosisTotal`,
  calculan `fechaFin` solos (ignoran el parámetro `fechaFin` — mutuamente excluyentes en la UI).
- `ObtenerMedicamentosDeHoyUseCase`: en el día final, recorta `horariosCalculados` al índice de
  corte — ya no aparecen tomas de más en Hoy/Mi salud.
- `RecordatorioReceiver`: bug relacionado encontrado de paso — reprogramaba la alarma de un
  Medicamento **para siempre**, sin revisar nunca `fechaFin` ni si estaba pausado (`activa`).
  Ahora `reprogramarMedicamentoSiVigente` relee el detalle actual antes de reprogramar mañana y
  no lo hace si ya pasó `fechaFin` (o el índice de corte, en el último día) o si está pausado.
- `CrearMedicamentoScreen`: "¿Cuándo termina?" pasó de 2 a 3 chips (Sin fin / Elegir fecha /
  Cantidad de dosis), con vista previa en vivo ("Termina el 6 de agosto") al escribir la
  cantidad, calculada con la misma función que usa el caso de uso — sin contar días a mano.

**3. Diario — quitar título y "Área de vida", dejar como un cuaderno.** El usuario probó la
pantalla y no le encontró sentido a esos dos campos ("tiene un título pero es un diario",
"opciones para elegir que están de más"). Se sacaron ambos de `DiaryEntryEditorScreen` (la
fecha, ya existente, queda como el único encabezado — mismo patrón que diarios de otras apps) y
de `DiaryListViewModel`/`DiaryListScreen`/`DiaryListUiState` (la lista ahora muestra fecha +
extracto, no "(sin título)"). El campo `titulo`/`areaDeVidaId` se queda en el modelo/tabla (sin
migración — no hacía falta borrar la columna), simplemente ya no se piden ni se muestran; las
entradas viejas que ya tenían un área asociada la pierden en la próxima edición, lo cual es lo
que se pidió.

**4 y 5. Sonido de Alarma/Sonido no confiable — diagnóstico y mitigación.** Sin poder capturar
un `logcat` en el momento exacto de una falla real (la prueba en vivo quedó pendiente para
cuando el usuario tenga un recordatorio real y el celular disponible), se revisó todo el código
de notificaciones a fondo y se encontraron y corrigieron dos problemas reales:
- `AlarmaSonidoService.iniciar()` envolvía todo el `MediaPlayer` en un `runCatching` **sin rama
  de error** — cualquier falla (de construir el `MediaPlayer`, o un error asíncrono después de
  `start()`) se tragaba en silencio, sin loguear nada. Se agregó `Log.e` en el fallo y un
  `setOnErrorListener` en el `MediaPlayer` (antes no existía — un error asíncrono a mitad de
  reproducción no se manejaba de ninguna forma, coincide con el síntoma reportado de "sonó menos
  de un segundo y se cortó").
- No se pedía audio focus antes de reproducir. Se agregó `solicitarAudioFocus`/
  `abandonarAudioFocus` con `AudioAttributes.USAGE_ALARM`.
- **Hipótesis más probable para el síntoma reportado** (celular Motorola, fallos intermitentes,
  a veces solo vibra): los fabricantes con gestión agresiva de batería (Motorola, Xiaomi, Huawei)
  matan procesos en segundo plano — eso puede cortar el `Service` de la alarma a mitad de
  reproducción sin ningún error de Android de por medio. Se agregó una fila condicional nueva en
  Ajustes ("🔋 Permitir que Lula funcione siempre", visible solo si
  `!exentoDeOptimizacionBateria`) que lleva al diálogo nativo de excepción de optimización de
  batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, permitido por Play Store para apps cuya
  función principal depende de alarmas confiables). Mismo patrón ya usado para "Permitir
  notificaciones"/"Permitir alarmas exactas".
- Para las citas ("1 día antes"/"1 semana antes" sin sonido, solo vibra): con nivel Sonido, el
  audio corre por el canal de notificación (`NotificationChannel`, stream de notificación) en
  vez del stream de Alarma — si el celular estaba en modo vibrar/silencioso para notificaciones,
  eso explicaría exactamente ese síntoma sin ser un bug de la app. No se puede confirmar sin ver
  el estado del celular en el momento exacto; queda anotado como hipótesis a verificar.
- **Pendiente real**: la próxima vez que el usuario tenga un recordatorio real por sonar y el
  celular disponible, conviene repetir la prueba con `adb logcat` corriendo en paralelo para
  confirmar (o descartar) estas hipótesis con una falla real capturada, en vez de solo con
  lectura de código.

Verificado: `compileDebugKotlin` sin errores. La instalación/verificación visual en dispositivo
quedó pendiente esta sesión porque el celular de prueba estaba en uso activo (WhatsApp en
primer plano) — se evitó mandar comandos `adb` intrusivos (abrir la app, tomar capturas) para
no interrumpir; solo se instaló el APK actualizado, que no cambia lo que se ve en pantalla.

## Segunda ronda de feedback de uso real — 6 puntos (2026-08-05)

El usuario siguió probando la app (ya con los 5 puntos anteriores instalados) y reportó 6
problemas más, incluyendo un bug de pérdida de datos. Mismo criterio: se leyó el código real
antes de tocar nada.

**1. Citas — no había forma de confirmar que ya se cumplió.** Confirmado: `CitaDetailScreen` no
tenía ninguna acción de estado (solo Editar/Eliminar/Compartir). `MarcarActividadUseCase` ya
existía (usado para Hábito/Tarea) y funciona igual de bien para Cita sin ningún cambio — se
agregó `CitaDetailViewModel.marcarCumplida()` y un botón "✅ Marcar como cumplida" (se oculta y
muestra "✅ Ya fuiste / se cumplió" una vez marcada).

**2. Cerrar día mostraba "9 de 14" cuando el usuario esperaba que los 14 fueran hábitos+tareas.**
Causa raíz: `CerrarDiaViewModel` contaba **todas** las `Actividad` activas del espacio
(`ObtenerActividadesDeHoyUseCase` trae Hábito+Tarea+Rutina+Medicamento+Cita+FechaImportante sin
filtrar), pero Medicamento nunca transiciona el `estado` de su `Actividad` padre a `CONFIRMADO`
(sus tomas se trackean aparte, por horario) — así que cada Medicamento sumaba al total sin poder
sumar nunca al completado. La pantalla "Progreso de hoy" de Hoy, en cambio, **ya** filtraba bien
(solo Hábito + Tarea de hoy/vencida) — dos implementaciones del mismo concepto, divergidas, el
mismo tipo de bug que ya había aparecido con Finanzas/fechas. Se corrigió centralizando el
criterio en `core/utils/ResumenDeHoyUtils.kt` (`actividadCuentaParaHoy`): Hábito siempre cuenta,
Tarea si es de hoy o está vencida, **Cita si es hoy** (nuevo, ahora que se puede confirmar —
punto 1), el resto no. `HomeViewModel` y `CerrarDiaViewModel` usan ahora la misma función — no
pueden volver a divergir. De paso, "✅ Ya hechos hoy (N)" en Hoy quedó con un número distinto a
propósito (también suma las tomas de Medicamento resueltas) — se cambió el texto a mostrar el
desglose ("9 + 6 tomas") en vez de un total opaco, para que no parezca un tercer número
contradictorio (el usuario lo señaló explícitamente después de instalar: "ya hechos hoy 15... 9
de 9... confuso").

**3. No había forma de revisar el cierre de días anteriores fuera del flujo justo después de
cerrar.** `HistoryScreen` ya existía (lista completa de `RegistroDiario`) y ya era alcanzable
desde el menú superior, pero el usuario pidió específicamente integrarlo al Calendario. Se
agregó: nuevo `ObtenerRegistroDiarioDeFechaUseCase`, `CalendarViewModel` lo consulta al mostrar
la vista Día, y si existe un cierre para esa fecha se muestra una tarjeta de solo lectura arriba
de los ítems del día (logré/costó/ajusté + "X de Y") — si no existe, no se muestra nada, tal
como pidió el usuario ("si lleno debe aparecer si no, simplemente no").

**4. BUG de pérdida de datos — "Cerrar día" borraba respuestas ya guardadas.** Causa raíz
confirmada: `CerrarDiaViewModel.init` nunca revisaba si hoy ya tenía un `RegistroDiario`
guardado — cada vez que se entraba a la pantalla (aunque fuera para "Actualizar cierre del día"),
`queLogre`/`queCosto`/`queAjusto` arrancaban en `""`. Como `CerrarDiaUseCase` hace upsert por
fecha, tocar "Guardar" sin escribir nada sobrescribía las respuestas reales con `null`. Se
agregó: el ViewModel ahora trae el registro existente (`obtenerProgresoDeHoyUseCase.registroDeHoy`,
ya existía, solo no se usaba acá) y lo expone en el `UiState`; la pantalla lo carga una sola vez
(`LaunchedEffect(uiState.cargando)`) en los campos de texto. También se ajustó el título/botón
("Actualiza el cierre de hoy" / "Guardar cambios") para que sea obvio que se está editando algo
que ya existe, no empezando de cero.

**5. Medicamentos — el mismo bug de "tomas de más" del punto anterior (dosis) seguía en el
Calendario.** El fix de la sesión anterior (recortar las tomas del último día cuando
`fechaFin` se calculó por `cantidadDosisTotal`) solo se había aplicado a
`ObtenerMedicamentosDeHoyUseCase` (Hoy/Mi salud) — `ObtenerAgendaDelRangoUseCase` (el Calendario,
que es como el usuario "revisó para mañana") tenía su propia copia de la lógica de generar tomas
por día, sin el recorte. Se aplicó el mismo criterio ahí también.

**6. Metas — no aparecían en Hoy, sin forma de ver progreso día a día.** `GoalsListScreen` ya
tenía barra de progreso y "X de Y", pero Hoy solo mostraba un enlace "Ver mis metas" (ni
siquiera un número). Se agregó una sección "🎯 TUS METAS" en Hoy con nombre + barra + progreso
de cada meta activa. De paso, se sacó la lógica de cálculo de progreso (antes duplicada dentro
de `GoalsListViewModel`) a `ObtenerMetasConProgresoUseCase` compartido — mismo motivo que el
punto 2, para que Hoy y "Tus metas" nunca puedan mostrar números distintos de la misma meta.
La pregunta más grande del usuario ("cómo hacemos que esto motive de verdad, con criterio de
experto en hábitos/metas") se respondió aparte, sin código todavía — ver conversación — porque
es una decisión de producto, no un bug.

Verificado: `compileDebugKotlin` sin errores, `installDebug` exitoso. El celular de prueba
seguía en uso activo (otra app en primer plano) al momento de instalar, así que no se hizo
verificación visual con `adb` esta vez tampoco — instalación silenciosa nomás.

## Metas — urgencia por fecha límite y reconocimiento de hitos (2026-08-05)

Continuación del punto 6 de la ronda anterior: el usuario pidió opinión de experto en
hábitos/apps de superación sobre cómo hacer que revisar las Metas se sienta vivo. Se propuso y
se aprobó construir dos piezas, ninguna nueva pantalla — ambas viven en Hoy, que es donde ya se
mostraba el progreso:

**1. Urgencia por fecha límite, solo cuando falta poco.** `fechaLimite` no hacía nada además de
mostrarse como texto. Se agregó `MetaConProgreso.diasRestantes`/`esUrgente` (últimos 7 días, o
ya vencida) — deliberadamente **no** se usó la idea original de "últimos 20% del tiempo desde
que se creó la meta" porque `Meta` no guarda fecha de creación, y agregar ese campo solo para
esto no se justificaba (ver simplificación). Cuando está en ese rango, la fila de la meta en Hoy
muestra "⏳ Faltan N día(s)" (o "venció hace N día(s)") en rojo — antes de eso, no se muestra
nada, para que no se sienta como presión constante desde el día 1.

**2. Reconocimiento breve al cruzar 25/50/75/100%.** Nuevo campo `Meta.ultimoHitoCelebrado`
(`MIGRATION_19_20`, versión 20) — evita que la misma tarjeta de "¡vas al 50%!" se repita cada
vez que Hoy se recompone. `MetaConProgreso.hitoActual`/`hayHitoNuevo` calculan si hay algo nuevo
que mostrar; una tarjeta chica (mismo estilo que la de "¿Aumentamos?" de hábitos progresivos) con
un solo botón "Genial" la marca como vista. A propósito es un mensaje breve y positivo, no una
alarma ni un sonido — coherente con que en Lula ningún intento se castiga, tampoco hacía falta
exagerar el premio.

**Cuidado al editar.** `ActualizarMetaUseCase` reconstruía la `Meta` desde cero en cada edición
— sin cuidado, esto hubiera reseteado `ultimoHitoCelebrado` a 0 con cualquier edición mínima
(hasta cambiar solo el nombre), volviendo a mostrar todas las celebraciones de la nada. Se
corrigió trayendo el valor existente (`metaRepository.obtenerConVinculo`) antes de reconstruir.
`AgregarProgresoMetaUseCase` no tenía este riesgo — ya actualiza solo `valorActual` vía DAO, sin
tocar el resto de la fila.

**De paso**, se sacó el cálculo de progreso de una Meta (antes duplicado dentro de
`GoalsListViewModel`) a `ObtenerMetasConProgresoUseCase`, ahora compartido con `HomeViewModel` —
mismo criterio de "una sola fuente de verdad" ya aplicado al resto de esta ronda de feedback.

Verificado: `compileDebugKotlin` sin errores, `installDebug` exitoso, `adb logcat` sin
`FATAL`/`Inconsistency` tras la migración 19→20 (el celular tenía Lula abierto al momento de
instalar — se reinstaló igual porque es justo la app que se estaba probando en la conversación).
No se relanzó la app para verificación visual completa — el proceso queda cerrado después de
`installDebug` sobre una app corriendo, y el usuario ya la tenía abierta por su cuenta.

## Tercera ronda de feedback de uso real — 7 puntos (2026-08-06)

Con capturas de pantalla de otra app como referencia visual para el último punto. Mismo
criterio: se leyó el código real antes de tocar nada; dos de los siete eran bugs confirmados.

**1. Citas — no se veía en Hoy si ya estaba cumplida, y faltaba una opción de "no se cumplió".**
`seccionAgenda` (la fila de Citas/Fechas importantes en Hoy) no mostraba ningún indicador de
estado. Se agregó el emoji de estado (✅/⏭️/⏳, compartido con Calendario — ver más abajo) al
nombre. También se agregó a `CitaDetailScreen` un segundo botón "⏭️ No se cumplió" junto a
"Marcar como cumplida", usando `EstadoActividad.OMITIDO` — mismo estado que ya usa Hábito para
"omitido, no es un fallo", coherente con que en Lula ningún intento se castiga.

**2. BUG — una tarea completada un día anterior seguía apareciendo como "hecha hoy".** Causa
raíz: `esTareaDeHoyOVencida` consideraba "de hoy" a cualquier tarea sin fecha límite, sin
importar cuándo se había completado — una vez `CONFIRMADO`, se quedaba mostrándose en
"Ya hechos hoy" para siempre. `Actividad.fechaCompletado` ya existía justo para esto y no se
usaba. Se rediseñó el criterio compartido (`actividadCuentaParaHoy`, `ResumenDeHoyUtils.kt`):
una tarea ya completada solo cuenta como "de hoy" si `fechaCompletado` cae hoy; si sigue
pendiente, se usa el criterio de siempre (sin fecha límite o vencida). El nombre de la función
para el caso "todavía pendiente" se separó (`esTareaPendienteDeHoyOVencida`) para no mezclar los
dos casos en una sola función ambigua.

**3. No se distinguía tarea de hábito en Hoy sin entrar al detalle.** Se extrajeron
`emojiTipoActividad`/`emojiEstadoActividad` (antes vivían duplicados y privados dentro de
`CalendarScreen.kt`) a `core/ui/TipoActividadEmoji.kt`, compartidos ahora por Hoy y Calendario.
`ActividadUi` ganó un campo `tipo` para poder mostrar el emoji correcto en cada fila,
especialmente en "Ya hechos hoy" (antes mezclaba hábitos y tareas sin ninguna marca).

**4. Metas ya completadas (100%) seguirían apareciendo en Hoy para siempre.** Confirmado como
diseño incompleto, no bug — la sección "Tus metas" en Hoy no filtraba por progreso. Se agregó
`HomeUiState.metasEnProgreso` (excluye `fraccion >= 1f`) para la lista de Hoy; la celebración de
hito 100% (ronda anterior) sigue apareciendo una vez, aparte, vía `hitosMetaPendientes`.

**5. Con muchas metas, no había forma fácil de ver primero lo pendiente.** `GoalsListViewModel`
ahora ordena: incompletas primero (por fecha límite más próxima), completadas al final, con un
separador "✅ Completadas" antes del primer ítem ya logrado.

**6. Rediseño de formularios (referencia visual, no implementado esta ronda).** El usuario
mostró capturas de otra app con filas colapsadas (Fecha/Repetir/Recordatorio/Etiqueta/Meta) que
se expanden a un selector al tocarlas, en vez de mostrar todo desplegado de una vez. Se le dio
una opinión honesta (ver conversación): es un patrón mejor que el actual, pero es un rediseño
grande — tocaría prácticamente cada pantalla de crear/editar de la app para ser consistente. No
se construyó nada todavía; se propuso empezar por una sola pantalla piloto antes de replicarlo,
a definir con el usuario.

**7. Resaltar en Hoy lo vencido o sin marcar cuando ya pasó su hora.** Se agregó a `seccionAgenda`
(Citas/Fechas importantes) un cálculo simple: si `item.horario` (formato "HH:mm") ya pasó
respecto a la hora actual y el estado sigue `SIN_CONFIRMAR`, el texto se pinta con
`MaterialTheme.colorScheme.error`. Deliberadamente **no** se aplicó todavía a Hábitos/Tareas en
esta ronda — `ActividadUi` no lleva el horario (solo `id`/`nombre`/`estado`/`tipo`), agregarlo
ahí es un cambio más grande que no se justificaba solo para esto; queda anotado como pendiente
si el usuario lo pide para esas secciones también. Tampoco se implementó "por vencer" (antes de
que pase la hora) — necesitaría releer el reloj en vivo mientras la pantalla está abierta, no
solo al cargar.

Verificado: `compileDebugKotlin` sin errores. La instalación quedó pendiente esta vez — el
celular no estaba conectado por `adb` al momento de terminar (a diferencia de las rondas
anteriores, donde sí lo estaba aunque en uso). Falta `installDebug` la próxima vez que esté
disponible.

## Piloto de formulario compacto — Crear Medicamento (2026-08-06)

Punto 6 de la ronda anterior, ahora sí construido: el usuario aprobó empezar por Crear
Medicamento (la pantalla que más creció esta sesión) para probar el patrón de las capturas de
referencia — filas colapsadas "etiqueta — valor actual ›" que abren un selector modal al
tocarlas, en vez de mostrar todo desplegado de una vez.

**`core/ui/SelectorRow.kt` (nuevo)** — fila reutilizable, pensada para repetirse en otras
pantallas si el piloto funciona bien: `SelectorRow(etiqueta, valor, onClick)`.

**`CrearMedicamentoScreen.kt` reestructurada.** Nombre y Dosis se quedan siempre visibles
arriba (son la identidad del medicamento). Los tres bloques que antes estaban siempre
desplegados —¿Cada cuánto?, ¿Cuándo termina?, ¿Qué tan insistente?— pasaron a tres
`SelectorRow` ("⏰ Frecuencia", "🏁 Termina", "🔔 Recordatorio") con un resumen en vivo del
valor actual (ej. "Cada 8 h desde 08:00", "5 dosis en total"). Cada fila abre un
`ModalBottomSheet` (mismo componente que ya usaba `AddMenuSheet` para el menú "+", primer
precedente en el código) con el contenido que antes vivía inline — sin cambiar ninguna lógica
de negocio, `CrearMedicamentoViewModel` quedó intacto. Los sheets de Frecuencia y Termina
tienen varios sub-campos, así que llevan un botón "Listo" explícito para cerrar; el de
Recordatorio es una sola elección (`NivelRecordatorioSelector`), así que se cierra solo al
tocar una opción.

**Verificado visualmente en dispositivo real** (no solo compilado): se instaló, se abrió
directo en Crear Medicamento vía `adb -e destino crear_medicamento`, se tocó "Frecuencia", se
confirmó que el sheet abre con todos los campos, se cambió la hora de la primera dosis con el
`TimePicker` anidado dentro del sheet (funciona sin conflicto), se confirmó que la fila
colapsada de atrás se actualiza en vivo ("Sin configurar" → "Cada 8 h desde 08:00") sin cerrar
el sheet, y que "Listo" cierra correctamente dejando el valor guardado. Se salió sin crear el
medicamento de prueba (`atrás`, sin guardar) para no ensuciar los datos reales del usuario.

Pendiente (a decidir con el usuario): si el patrón se siente bien en el uso real, replicarlo en
Crear Tarea, Crear Hábito, Crear Cita — cada una necesitaría sus propios 2-4 `SelectorRow`
según qué campos tenga.

## Formulario compacto replicado a Tarea, Hábito, Cita, Fecha importante (2026-08-06)

El usuario confirmó que el piloto de Medicamento se ve mejor ("está bueno, replica a todos, se
ve más ordenado") — se aplicó el mismo patrón (`SelectorRow` + `ModalBottomSheet`, "Listo" para
cerrar) a las otras cuatro pantallas con bloques de configuración que valía la pena compactar.
**No** se tocó Lista, Rutina, Meta ni Movimiento (Finanzas) — son pantallas simples (1-2 campos,
sin clúster de recordatorio/frecuencia) donde el patrón no aporta, solo agregaría una capa de
indirección de más.

- **Crear Tarea**: Nombre e Importante/Urgente se quedan visibles (identidad rápida). Fecha
  límite + ¿Se repite? + Recordarme a las + ¿Qué tan insistente? — que antes eran 4 bloques
  siempre desplegados, uno detrás de otro solo cuando había fecha — se combinaron en un solo
  `SelectorRow` "📅 Fecha y recordatorio" con resumen tipo "Hoy · 09:00". El vínculo a
  medicamento/cita se dejó inline (opcional, poco frecuente, no vale la pena esconderlo detrás
  de un selector más).
- **Crear Hábito**: Nombre y Momento del día se quedan visibles. Duración inicial + progresión
  (objetivo/incremento/frecuencia de revisión) pasó a "⏱️ Duración". Hora + nivel de
  recordatorio pasó a "🔔 Recordatorio".
- **Crear Cita**: Nombre/Lugar/Motivo/Fecha/Hora se quedan visibles (son la cita en sí). El mapa
  de recordatorios (varias anticipaciones, cada una con su propia hora) + nivel de insistencia
  pasó a un solo "🔔 Recordatorios", con resumen que se adapta: "Sin recordatorios" / "Un día
  antes a las 20:00" / "3 recordatorios" según cuántos haya activos.
- **Crear Fecha importante**: Nombre y Fecha se quedan visibles. Se repite + Recordarme
  (anticipación) + Hora + Cómo avisarme se combinaron en un solo "🔔 Recordatorio".

Verificado visualmente en dispositivo real: se instaló y se abrió cada pantalla por separado
(`adb -e destino crear_tarea`, `crear_habito`, `crear_cita`) confirmando que el resumen de cada
`SelectorRow` reflejaba el estado por defecto correctamente ("Sin fecha", "Sin configurar", "Un
día antes a las 20:00", etc.). No se alcanzó a verificar Crear Fecha importante con captura —
el celular pasó a uso normal (Reels) a mitad de la verificación y se dejó de interactuar con él
para no interrumpir; mismo patrón exacto que las otras tres, y compiló sin errores, así que el
riesgo de que falle específicamente ahí es bajo.

## Medicamento: recordatorio persistente / "insistir" (2026-08-06)

A pedido del usuario ("son las 17:00 y esa toma debió ser a las 14:00, ¿cómo hacer que la
tome? ... poner una opción de repetir cada 5 min u otro tiempo hasta que lo tome"). Dos partes:

1. **Vencido en rojo**: `TomaAccionRow` ahora calcula `vencida = estado == SIN_CONFIRMAR &&
   horario < horaActual` y pinta el nombre/instrucción con `MaterialTheme.colorScheme.error` +
   sufijo "⚠️" — mismo criterio que ya existía para Citas/Fechas importantes vencidas en Hoy,
   que hasta ahora no cubría Medicamento.
2. **Insistencia configurable**: `ActividadDetalle.Medicamento` gana `recordatorioPersistente:
   Boolean` e `intervaloPersistenciaMin: Int?` (`MIGRATION_20_21`, `LulaDatabase` v21). Se
   configura en `CrearMedicamentoScreen` dentro del mismo sheet "🔔 Recordatorio" (chips "Solo
   una vez" / "Insistir" + campo de minutos), que además cambió su cierre automático por un
   botón "Listo" explícito para que quepan los campos nuevos.

Mecanismo: cuando la alarma normal de una toma suena y queda `intervaloPersistenciaMin`
configurado, `RecordatorioReceiver` programa una **segunda cadena de alarmas** independiente
(`RecordatorioScheduler.programarRenotificacionMedicamento`, clave compuesta
`actividadId:horario:renotif` para no pisar la alarma diaria normal). Cada vez que esa alarma
de insistencia suena, revisa si la toma sigue `SIN_CONFIRMAR` **y** si el día calendario no
cambió (`fechaOriginalEpochDay` guardado en el `Intent`, comparado contra
`DateTimeUtils.hoy()`) — respuesta a la pregunta que el usuario dejó abierta ("¿hasta que la
marques o hasta que termine el día?"), eligió explícitamente "hasta que la marques o termine el
día": si cualquiera de las dos deja de cumplirse, la cadena se corta sola en vez de
reprogramarse. `MarcarTomaMedicamentoUseCase` cancela la cadena de insistencia proactivamente
apenas se marca la toma (`CONFIRMADO` u `OMITIDO`), no espera a que la próxima insistencia
descubra el cambio. Nueva función de repositorio `obtenerEstadoToma` (una sola toma puntual,
no la lista completa) para esa revisión.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`).

## Citas recurrentes / "cursos" — diseño (2026-08-06)

Motivado por casos reales de cuidado descritos por el usuario: radioterapia (solo días
laborables, 20 dosis en total, sábados/domingos/feriados no) y masajes (lunes/miércoles/viernes
por 2 meses, después baja a 2 por semana y luego a 1 por semana). El usuario rechazó
explícitamente la alternativa simple ("generar 20 filas de Cita de una vez") a favor de un
**curso único con sesiones reprogramables individualmente**, y dejó a criterio de esta sesión
cómo resolver la reprogramación — la decisión: reprogramar una sesión mueve **solo esa sesión**
(su `fecha` cambia, `fechaOriginal` queda como referencia histórica), el resto del programa y
el conteo total no se tocan.

**Simplificación deliberada — feriados**: no existe (ni se va a construir ahora) un calendario
de feriados por país. En vez de eso, un feriado que cae en un día del patrón se resuelve con la
misma herramienta que ya existe: reprogramar esa sesión puntual a otro día. No es una limitación
oculta, es la misma mecánica que el usuario ya pidió para "se corrió el programa un día".

**Simplificación deliberada — cambio de frecuencia a mitad de curso**: no se modela como
"tramos" superpuestos dentro de un mismo curso (demasiada complejidad para el beneficio). Un
curso tiene **un patrón de días de la semana vigente a la vez**. Cuando cambia (ej. masaje pasa
de 3x/semana a 1x/semana), el usuario edita la Cita y cambia el patrón — eso solo afecta a las
sesiones que se generan **de ahí en adelante**; las ya generadas (pasadas, marcadas, o
reprogramadas a mano) no se tocan. Es el mismo modelo mental que "editar una serie de eventos
recurrentes desde hoy en adelante" en un calendario común.

**Modelo de datos**:
- `ActividadDetalle.Cita` gana (todos opcionales, no rompen citas puntuales existentes):
  `esCurso: Boolean = false`, `diasSemana: Set<Int> = emptySet()` (números ISO 1=lunes..7=domingo,
  mismo formato que `Habito.diasEspecificos`), `horaSesion: String? = null`,
  `fechaInicioCurso: Long? = null`, `cantidadSesionesTotal: Int? = null` (`null` = sin cantidad
  fija, curso abierto como el masaje). Una Cita puntual sigue usando `fechaHora` exactamente
  como antes — `esCurso = false` es el default y no cambia ningún comportamiento existente.
- Nueva tabla `sesion_cita` (entidad `SesionCitaEntity`, dominio `SesionCita`), mismo patrón que
  `TomaMedicamentoEntity` (una fila por ocurrencia, no una Cita por ocurrencia): `id`,
  `actividadId` (FK CASCADE), `numeroSesion` (1-based, fijo — es el número que se muestra y el
  que ordena el progreso, nunca cambia aunque se reprograme), `fecha` (vigente, se actualiza al
  reprogramar), `fechaOriginal` (la que le tocaba según el patrón, no se toca nunca), `horario`
  (HH:mm), `estado`.
- Progreso ("van 9 de 20") = sesiones con `estado == CONFIRMADO` sobre `cantidadSesionesTotal`
  (o sobre el total generado hasta ahora, si el curso es abierto).

**Generación**: al crear o editar un curso, `GenerarSesionesCursoUseCase` calcula las próximas
fechas que caen en `diasSemana` a partir de `fechaInicioCurso` (o de hoy, si se está extendiendo
uno ya existente), numerando sesiones consecutivas a partir del último `numeroSesion` que ya
exista. Se detiene al llegar a `cantidadSesionesTotal` (si hay) o a un horizonte de 90 días (si
es abierto). Un curso abierto se re-extiende solo cuando hace falta — el mismo lugar donde ya se
lee la agenda (`ObtenerAgendaDelRangoUseCase`) llama a `ExtenderSesionesCursoSiHaceFaltaUseCase`
si quedan menos de 14 días de sesiones generadas por delante, para que nunca se le acabe el
programa a un curso sin cantidad fija sin que el usuario tenga que volver a editarlo.

**Recordatorios**: cada sesión reutiliza `recordatorios`/`nivelRecordatorio` del curso. Alarma
independiente por sesión y anticipación (`RecordatorioScheduler.programarSesionCita`, clave
`cita:sesion:$numeroSesion:$anticipacion` — mismo principio de clave compuesta que ya usan
Medicamento y Cita puntual). Reprogramar una sesión cancela y vuelve a programar solo las
alarmas de esa sesión.

**Dónde se ve y se marca**: `ObtenerAgendaDelRangoUseCase` (una sola fuente para Hoy y
Calendario, ver sección de "9 de 14 vs 9 de 9" más arriba) agrega una fila por `SesionCita` en
su fecha vigente en vez de una sola fila por `fechaHora`, cuando `esCurso`. Así Hoy y Calendario
muestran las sesiones de curso sin duplicar lógica. `CitaDetailScreen` muestra, cuando
`esCurso`, el progreso y la lista completa de sesiones (marcar cumplida/no cumplida,
reprogramar fecha) en vez del botón único que usa una Cita puntual. `CerrarDiaViewModel`
también necesitó actualizarse: `Actividad.estado` nunca se toca en una Cita de curso (cada
sesión tiene el suyo), así que "Progreso de hoy"/"Cerrar día" habrían dejado de contar las
citas de curso — se agregó `estadoDeHoy()` (mira la sesión de hoy cuando corresponde) al lado
de `actividadCuentaParaHoy()`, mismo criterio compartido de siempre.

**Implementado**: modelo de dominio, `SesionCitaEntity`/`SesionCitaDao`, `MIGRATION_21_22`
(`LulaDatabase` v21→22), mappers, `ActividadRepository`/`Impl`, `GenerarSesionesCursoUseCase`,
`ExtenderSesionesCursoSiHaceFaltaUseCase` (se llama junto con
`CerrarTareasVinculadasVencidasUseCase` al abrir Hoy), `MarcarSesionCitaUseCase`,
`ReprogramarSesionCitaUseCase`, `RecordatorioScheduler.programarSesionCita`/
`cancelarSesionCita` (clave `cita:sesion:$numeroSesion:$anticipacion`), `CrearCitaUseCase`/
`ActualizarCitaUseCase` extendidos, `EliminarActividadUseCase` cancela la alarma de cada sesión
al borrar un curso. UI: `CrearCitaScreen` ganó un `SelectorRow` "🔁 Repetición" (una sola vez /
curso, con días de la semana, hora de sesión, fecha de inicio y cantidad de sesiones opcional);
`CitaDetailScreen` muestra la lista completa de sesiones con marcar/reprogramar cuando `esCurso`.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`). No se alcanzó a instalar ni
probar en dispositivo real — no había ningún celular conectado (`adb devices` vacío) al
terminar esta sesión.

## Rediseño de Hábitos + protección "salir sin guardar" + Tarea vencida en rojo (2026-08-06)

El usuario le pidió opinión a otro chat de Claude sobre cómo mejorar la pantalla de Hábitos y
trajo esa respuesta como pedido de diseño. Se construyó lo que tenía una definición clara;
"Constancia %" se dejó afuera de esta pantalla a propósito — el usuario mismo anotó dudas sobre
cómo se calcularía, y ya existe un concepto de constancia distinto en `ProgresoScreen`
(`calcularConstancia`, 30 días) que no convenía duplicar ni confundir con uno nuevo.

**`HabitsListScreen` reconstruida** (antes: lista plana de filas, sin nada de esto):
- **Tarjetas en vez de filas** (`Card` de Material3 — fondo distinto del fondo de pantalla,
  esquinas redondeadas, ya viene así del tema).
- **Ícono automático por hábito** (`core/ui/HabitoEmoji.kt`, `emojiParaHabito(nombre)`) — busca
  palabras clave conocidas en el nombre ("cama"→🛏️, "desayun"→🍳, etc.), con un ícono neutro
  (✅) si ninguna coincide. Puramente cosmético, no se guarda.
- **Racha por hábito** (`🔥 3`), no solo la racha global de arriba — usa
  `ObtenerHistorialHabitoUseCase.calcularRacha` (ya existía, no se usaba acá). Esto resuelve
  directamente la queja del usuario ("la racha no cuadra con los círculos de abajo"): la racha
  global de `LulaTopBar` cuenta días con "Cerrar mi día" hecho (otro concepto, sin relación
  directa con los círculos de un hábito puntual — ver sección "9 de 14 vs 9 de 9" más arriba),
  mientras que la racha de la tarjeta se calcula del mismo historial que sus propios círculos,
  así que siempre cuadra.
- **Letras de día (L M M J V S D) sobre los círculos**, calculadas de verdad por fecha
  (`DateTimeUtils.letraDiaSemana`) en vez de asumidas, y **hoy resaltado** (círculo con borde
  más grueso, letra en negrita) — antes no había forma de saber a qué día correspondía cada
  punto.
- **Agrupado por momento del día** (Mañana/Tarde/Noche, mismas etiquetas que ya usa
  `etiquetaMomentoDelDia`), leyendo `Actividad.momentoDelDia` (ya denormalizado, sin queries
  nuevas).
- **Mensaje motivacional** arriba de la lista, calculado de la fracción de días cumplidos de la
  semana visible (no un número frío) — nunca negativo ni de reproche, ni siquiera con 0%
  ("Hoy es un buen día para empezar 🙂"), consistente con "todo intento vale" de
  `Plan/CLAUDE.md`.
- `HabitsListViewModel`/`HabitsListUiState` ganaron los campos nuevos (`emoji`, `momentoDelDia`,
  `racha`, `DiaTrackerUi` con `letra`/`confirmado`/`esHoy`) — sin cambios de esquema, todo se
  deriva de datos que ya existían.

**Investigado, no es un bug**: el chip "💰 S/0" de `LulaTopBar` que el usuario reportó en cero
"ya se había llenado antes". Es reactivo de verdad (`Flow` sobre Room, se actualiza solo) y
está bien scoped — muestra **gastos (egresos) de HOY**, no un total acumulado ni ingresos. Si
el usuario registró un ingreso, o un gasto en un día distinto a hoy, el chip en 0 es correcto
según su definición actual. Queda pendiente confirmar con el usuario si el chip debería mostrar
otra cosa (ej. balance total, o incluir ingresos) — no se cambió sin esa confirmación.

**Tarea (y Hábito) vencidos sin marcar, en rojo en Hoy**: mismo criterio que ya tenían
Cita/Fecha importante (`seccionAgenda`) y Medicamento (`TomaAccionRow`), que no cubría
`seccionActividades` (Tareas y Hábitos). `ActividadUi` ganó `horaRecordatorio: String?`
(se descartaba al mapear desde `Actividad.detalle`); `seccionActividades` ahora calcula
`vencida` igual que las demás secciones y pinta texto + "⚠️" en rojo.

**Protección "salir sin guardar" — nuevo `core/ui/DescartarCambiosAlSalir.kt`**: el usuario
reportó haber perdido una Tarea completa (nombre, hora, insistencia) porque tocó "Listo" en el
sheet de Recordatorio (que solo cierra el sheet, no guarda nada) y salió de la pantalla
pensando que ya había guardado. `DescartarCambiosAlSalir(hayContenidoSinGuardar, onDescartar)`
combina `BackHandler` (de `androidx.activity.compose`, no se usaba en ningún lado del código
antes) + un `AlertDialog` de confirmación — intercepta el botón/gesto de atrás y pregunta antes
de perder lo escrito. Se replicó, a pedido explícito del usuario ("puede pasar lo mismo en
otras ventanas y con otras personas"), en las 10 pantallas "Crear X": Tarea, Hábito,
Medicamento, Cita, Fecha importante, Movimiento (Finanzas), Meta, Lista, Rutina, Reto familiar.

Decisiones de alcance para no sobre-construir:
- El criterio de "hay algo sin guardar" es el campo principal de cada pantalla no vacío
  (`nombre`, o `montoTexto` en Movimiento) — no se instrumentó cada campo individual, hubiera
  sido mucho más código para un beneficio marginal (quien llegó a configurar hora/recordatorio
  casi siempre ya escribió el nombre primero, que es el primer campo de cada formulario).
- Se limitó a **modo creación** (`!esEdicion`) en las pantallas que tienen edición — en modo
  edición el nombre siempre viene lleno desde `formInicial`, así que sin este límite el diálogo
  aparecería SIEMPRE al salir de editar algo ya existente, incluso sin tocar nada, lo cual sería
  más molesto que útil. El caso reportado por el usuario fue de creación, no de edición.
- No se tocó `LulaNavHost.kt` — cada pantalla ganó un parámetro nuevo
  `onSalirSinGuardar: () -> Unit = onGuardado` (mismo valor por defecto, ya que a nivel de
  navegación "guardar y salir" y "descartar y salir" hacen exactamente lo mismo,
  `popBackStack()`); solo cambia qué botón del diálogo lo dispara.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`). No se alcanzó a instalar ni
probar en dispositivo real — no había ningún celular conectado (`adb devices` vacío) al
terminar esta sesión.

## Sexta ronda — 7 puntos de uso real (2026-08-07)

1. **Teclado tapaba el campo "¿Cuántas sesiones?"**: los `ModalBottomSheet` largos con un campo
   numérico cerca del final (Crear Cita: repetición; Crear Medicamento: frecuencia/termina/
   recordatorio; Crear Hábito: duración) no tenían `.verticalScroll()` ni `.imePadding()` — el
   teclado los tapaba enteros en vez de que el sheet se acomodara. Se agregó a los 5 sheets
   afectados.

2. **Cita de curso desaparecía de "Mi salud" apenas empezaba**: `HealthViewModel` filtraba
   "Próximas citas" con `detalle.fechaHora`, que para un curso es la fecha de la PRIMERA sesión
   nada más (nunca se actualiza) — un curso de radioterapia con 19 sesiones por delante se
   volvía invisible ahí apenas pasaba el día 1. Ahora `citaDeCursoUi` calcula la próxima sesión
   `SIN_CONFIRMAR` de verdad (`ObtenerSesionesCitaUseCase`) y muestra "Van X de Y sesiones";
   se deja de mostrar solo cuando ya no queda ninguna pendiente. También en `CitaDetailScreen`:
   cada sesión ahora tiene color propio por estado (antes solo emoji, "se veían todas iguales"
   según el usuario) y un botón "↩️ Deshacer" para volver una sesión marcada por error a
   `SIN_CONFIRMAR` (antes no había forma de corregir un toque equivocado) —
   `MarcarSesionCitaUseCase` reprograma el recordatorio de esa sesión si se deshace y todavía no
   pasó su hora, ya que marcarla la había cancelado.

3. **Tareas hechas y pendientes mezcladas sin orden**: `TasksListScreen` (vista Lista) ordenaba
   por `fechaCreacion DESC` nomás, sin separar completadas de pendientes — confirmado que las
   vencidas de días anteriores sí seguían apareciendo (nada las saca), solo que sin ningún aviso
   más allá del texto en rojo. Ahora hay dos secciones con encabezado ("PENDIENTES" con vencidas
   y fecha próxima primero, luego "✅ HECHAS" aparte) y la fecha vencida suma texto "⚠️ vencida",
   no solo color.

4. **"Descartar cambios al salir" no se activaba en modo edición**: la primera versión (ronda
   anterior) solo comparaba `!esEdicion && nombre.isNotBlank()` — a propósito, para no
   preguntar en CADA salida de una edición sin cambios. El usuario probó editar una Meta
   existente, cambió algo, y salió sin aviso — pidió explícitamente que cubra los dos casos.
   Se reemplazó por un snapshot real: cada pantalla arma `snapshot() = listOf(campo1, campo2,
   ...)` con todos sus campos, se guarda una copia apenas carga (`formInicial`) o al primer
   render (creación), y se compara por igualdad estructural (`!=`) contra el estado actual en
   cada recomposición — funciona igual de bien recién creando que editando algo ya existente,
   sin necesidad de instrumentar cada `onValueChange` individual.

5. **Racha en 0 mientras no se cierre el día + nada recuerda seguir la racha**: confirmado que
   `calcularRachaActual` empieza a contar desde HOY hacia atrás — si hoy todavía no se cerró,
   la racha se muestra en 0 sin importar cuántos días reales lleve la persona, "recuperándose"
   recién al cerrar. No es un bug (la racha siempre fue "días CERRADOS consecutivos"), pero el
   usuario preguntó directamente "¿qué pasa si no entro un día?" y pidió algo que recuerde
   seguir, sobre todo con hábitos sin recordatorio propio. Se construyó un recordatorio diario
   genérico nuevo (Ajustes → "🔥 Recordarme cerrar mi día", apagado por defecto, hora
   configurable): una alarma que se auto-reprograma todos los días (mismo patrón que un Hábito)
   y se salta la notificación sola si el día ya se cerró para esa hora — no hace falta que el
   usuario la apague manualmente los días que sí cumplió. Primer recordatorio de la app que NO
   está ligado a ningún `actividadId` — `RecordatorioReceiver` lo distingue por un extra booleano
   (`EXTRA_ES_RECORDATORIO_CIERRE_DIA`) revisado antes que nada en `onReceive`, con su propio
   texto ("🔥 ¿Cómo te fue hoy?") en vez de pasar por `mostrarNotificacion` (que arma el texto a
   partir de un `TipoActividad` real). Nueva preferencia `AjustesRepository.
   observarHoraRecordatorioCierreDia()` (DataStore, null = apagado), reprogramada también al
   reiniciar el celular (`BootReceiver`).

6. **Finanzas → Historial sin resumen ni rango de fechas**: `FinancesHistoryScreen` solo listaba
   movimientos sueltos, sin ningún total — el resumen con `StatPill` (📈/📉/⚖️) ya existía en
   `FinancesScreen` pero fijo al mes en curso, nunca se reusó acá. Se agregó el mismo resumen
   (ahora sobre lo que esté visible: mes o rango) y un modo "📅 Rango de fechas" con dos
   `DatePickerDialog` encadenados (desde → hasta) que reusa el mismo `ObtenerBalanceMesUseCase`
   — a pesar del nombre, ya aceptaba cualquier `desde`/`hasta`, no solo un mes calendario.

7. **Alarma sonó "un segundo y se cortó"**: no se pudo reproducir ni diagnosticar en firme sin
   Logcat del dispositivo — la sospecha más probable, dado que ya existe la fila "🔋 Permitir que
   Lula funcione siempre" en Ajustes (gestión de batería agresiva de fabricante, ver ronda del
   2026-08-05), es que `AlarmaSonidoService` se esté iniciando y el sistema lo mate casi de
   inmediato si esa exención no está concedida. Se agregó manejo de errores con `Log.e` alrededor
   de `startForegroundService` (antes una excepción ahí quedaba completamente muda, sin ninguna
   pista en Logcat) para poder diagnosticar la próxima vez que pase. Pendiente: confirmar con el
   usuario si ya tiene la exención de batería activada: si no, es la explicación más probable.

**Fuera de alcance esta ronda** (anotado para no perderlo, no construido): una sección de
"citas históricas" en Mi salud (hoy no existe ni para citas puntuales, no solo para cursos) —
el usuario la mencionó pero el arreglo urgente era la desaparición del curso en progreso, no un
historial completo nuevo.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`). No se alcanzó a instalar ni
probar en dispositivo real — no había ningún celular conectado (`adb devices` vacío) al
terminar esta sesión.

## Séptima ronda — 7 puntos de uso real + diagnóstico de alarmas (2026-08-10)

1. **Calendario mostraba Hábitos antes de su fecha de creación**: `ObtenerAgendaDelRangoUseCase`
   agregaba cada Hábito a TODOS los días del rango visible, sin comparar contra
   `Actividad.fechaCreacion` — uno creado hoy aparecía como "pendiente" en días pasados, antes
   de existir. Corregido con un filtro `fecha < fechaCreacion → se salta`. Medicamento/Cita ya
   estaban bien acotados por `fechaInicio`/`fechaHora`; Fecha importante ya tenía
   `if (fecha < base) return false` en `ocurreEnFecha`.

2. **Listas sin flechas de reordenar**: a diferencia de Notas (`orden` + flechas ▲▼), los ítems
   de una Lista no tenían forma de reordenarse aunque el modelo ya traía `ListaItem.orden`.
   Se replicó el mismo patrón exacto de Notas: `ListaItemDao.actualizarOrden`,
   `ListaRepository.actualizarOrdenItem`, `ActualizarOrdenItemListaUseCase`,
   `ListDetailViewModel.moverArriba/moverAbajo` (intercambia `orden` con el vecino).

3. **No se podía llenar/actualizar "Cerrar mi día" de un día anterior**: la pantalla y el caso
   de uso asumían siempre "hoy". Ahora `CERRAR_DIA` acepta un `fecha` opcional (epoch day) —
   sin fecha, comportamiento idéntico a siempre; con fecha (llega desde Calendario), carga el
   registro de ESE día si existe, y las dos cifras "completadas"/"totales" se escriben a mano
   en vez de auto-calcularse (no hay forma confiable de recalcular en vivo el estado de un día
   que ya pasó). Nuevo botón en Calendario (vista Día, día pasado sin cerrar): "📝 Llenar el
   cierre de este día"; la tarjeta de cierre ya existente ganó un botón "✏️ Editar". La racha se
   recalcula siempre al guardar (no solo si es hoy), porque cerrar un día anterior puede
   rellenar un hueco que extiende la racha hacia atrás.

4. **Método FODA**: el usuario pidió opinión antes de construir — se recomendó como extensión
   de Mi propósito (no módulo aparte), con "Aspiraciones" mapeadas a `Meta` y "Resultados" al
   progreso que Metas ya calcula, para no duplicar lógica. Documentado en
   `10-pendientes.md` como algo para más adelante, no construido esta ronda (a pedido explícito
   del usuario: "para uno que ya está metido más en la app").

5. **Recordatorio genérico por franja del día**: además del recordatorio de cierre del día
   (ronda anterior), ahora hay 3 recordatorios independientes configurables en Ajustes
   ("🔔 Recordarme revisar Lula" — Mañana/Tarde/Noche, apagados por defecto), cada uno con su
   propio `AjustesRepository.observarHoraRecordatorioFranja(MomentoDelDia)`. Mismo patrón de
   auto-reprogramación diaria que un Hábito; `RecordatorioReceiver` los distingue por un nuevo
   extra `EXTRA_MOMENTO_FRANJA` (separado de `EXTRA_ES_RECORDATORIO_CIERRE_DIA`), con su propio
   texto ("🔔 ¿Revisaste Lula esta {franja}?") en vez de pasar por `mostrarNotificacion`.

6. **Cita de curso: sesiones futuras se veían como ya cumplidas**: `FilaSesionCita` mostraba el
   botón "✅ Cumplida" en TODAS las sesiones `SIN_CONFIRMAR`, incluidas las que todavía faltan
   varias semanas — con un curso largo (20 sesiones de radioterapia) eso se veía como una fila
   de checks verdes, confundiendo al usuario ("como que ya se hubieran hecho"). Ahora una
   sesión futura (fecha > hoy) solo puede reprogramarse, no marcarse — no tiene sentido marcar
   algo que todavía no pasó — y se etiqueta "· pendiente" en vez de mostrar el botón de marcar.

7. **Diagnóstico a fondo: alarma de Tarea "no sonó nada"** — investigado con el dispositivo
   conectado (`adb shell dumpsys alarm/notification/package`), no adivinado:
   - El `AlarmManager` SÍ tenía la alarma programada y SÍ la disparó (confirmado en
     `dumpsys alarm`, 3 alarmas recientes registradas para `RecordatorioReceiver`).
   - Los 3 canales de notificación (Silencioso/Sonido/Alarma) están bien configurados
     (`mImportance=4`, sonido correcto, no bloqueados).
   - **La causa real**: `dumpsys package` mostró la sección "runtime permissions:" completamente
     vacía — `POST_NOTIFICATIONS` nunca quedó concedido en ese dispositivo. La app SÍ pide este
     permiso al abrir (`SolicitarPermisoNotificaciones` en `MainActivity`), pero Android nunca
     vuelve a mostrar el diálogo después de una negación — sea cual sea el motivo de la
     negación original, la única forma de recuperarlo es ir a Ajustes del sistema. Con el
     permiso ausente, `NotificationManagerCompat.notify()` descarta la notificación en
     silencio, sin excepción ni rastro — exactamente "no sonó nada, ni sonido ni alarma".
   - Arreglado lo que sí es responsabilidad de la app: (a) `Log.e` alrededor de `.notify()` para
     que la próxima vez quede un rastro real en Logcat; (b) un banner nuevo en Hoy
     ("🔕 Sin permiso de notificaciones — tus recordatorios no van a avisarte", toca para abrir
     Ajustes) que se revisa cada vez que la pantalla vuelve a primer plano — antes este estado
     solo se veía si el usuario entraba por su cuenta a Ajustes, sin ningún aviso proactivo en
     ningún otro lado de la app.
   - **Acción pendiente del usuario**: conceder el permiso de notificaciones manualmente (Ajustes
     del sistema, o tocar el banner nuevo en Hoy) — este es un estado del dispositivo, no
     arreglable desde el código.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`). Se intentó instalar
(`installDebug`) con el dispositivo conectado y sin uso activo (pantalla AOD), pero el
dispositivo pasó a estado OFFLINE a mitad de la instalación (`EXIT_CODE=1`, "No online devices
found") — no se alcanzó a confirmar en la app real esta ronda.

## Octava ronda — corrección de Listas (orden mal aplicado) + etiqueta "Hoy" en Calendario (2026-08-10)

1. **Corrige el punto 2 de la ronda anterior — las flechas ▲▼ se habían puesto en el lugar
   equivocado**: el usuario aclaró que lo que quería reordenar eran los TÍTULOS de lista (la
   pantalla `ListsScreen`, que lista "Viaje", "Compras", etc.), no los ítems de adentro de cada
   lista. Se revirtió el orden manual interno y se construyó de nuevo en el nivel correcto:
   - `ListaEntity` gana `orden: Int` (antes solo `ListaItemEntity` lo tenía) — migración
     `MIGRATION_22_23` (`ALTER TABLE lista ADD COLUMN orden`), `LulaDatabase` sube a versión 23.
   - `ListaDao.observarConConteo` ahora ordena por `lista.orden ASC` (antes `fechaCreacion DESC`)
     y expone `orden` en `ListaConConteoRow`; nuevo `ListaDao.actualizarOrden` +
     `obtenerMayorOrden` (para asignar el siguiente `orden` al crear una lista nueva).
   - `ListaRepository.actualizarOrdenItem` → renombrado `actualizarOrdenLista` (opera sobre la
     tabla `lista`, no `lista_item`); `ActualizarOrdenItemListaUseCase` → reemplazado por
     `ActualizarOrdenListaUseCase`.
   - `ListsViewModel` gana `moverArriba`/`moverAbajo` (mismo patrón swap-con-vecino que Notas);
     `ListsScreen` gana los botones ▲▼ junto a cada título de lista.
   - **Dentro de una lista, en vez de flechas manuales, autoordenado por estado**: los ítems no
     marcados quedan siempre adelante y los marcados se hunden al final (`ListaRepositoryImpl.
     observarConItems` ordena con `compareBy({ it.marcado }, { it.orden })` antes de mapear a
     dominio) — así lo pendiente siempre está arriba sin que el usuario tenga que tocar nada.
     `ListDetailViewModel`/`ListDetailScreen` perdieron `moverArriba`/`moverAbajo` y los botones
     ▲▼ correspondientes; `ListaItemDao.actualizarOrden` (a nivel de ítem) se eliminó por no
     tener ya ningún llamador — el campo `ListaItem.orden` se conserva como desempate estable
     dentro de cada grupo (marcado/no marcado), fijado por orden de creación.

2. **Calendario: "Hoy" aparecía debajo de la fecha sin importar qué día se estuviera viendo**:
   el encabezado compartido por las 3 vistas (Día/Semana/Mes) tenía un `TextButton` fijo con el
   texto literal `"Hoy"` — pensado como atajo de navegación ("ir a hoy"), pero visualmente
   indistinguible de una etiqueta que describiera el día mostrado, y el usuario lo leía como tal
   en cualquier fecha. Corregido:
   - Vista Día: el subtítulo ahora es dinámico — `"Hoy, {día de semana}"` (ej. "Hoy, lunes") si
     `fechaSeleccionada` es de verdad hoy, o solo el nombre del día (ej. "Martes") si no
     (`DateTimeUtils.nombreDiaIso` + `numeroDiaIso`, ya existían para otro uso). Vista Semana/Mes
     no muestran subtítulo (un rango o un mes no tienen un solo "día de la semana" que mostrar).
   - El atajo para volver a hoy se mantiene, pero ahora es un botón "Ir a hoy" que solo aparece
     cuando el rango visible NO incluye la fecha de hoy (`diasVisibles.any { it.esHoy }`,
     calculado igual en las 3 vistas) — no vuelve a mostrarse cuando ya se está viendo hoy, que
     era justo la fuente de la confusión original.
   - Los otros usos de "hoy" en Calendario (el 🔵 antes de la fecha en cada día de la vista
     Semana, el círculo de fondo resaltado en la vista Mes) ya estaban bien condicionados a
     `dia.esHoy` real — no tenían este bug, no se tocaron.

3. **Corrección sobre la marcha del punto 1 de esta misma ronda — la primera lista no se
   reordenaba**: probado por el usuario apenas instalado, reportó que la flecha ▲ de la
   primera lista de la pantalla "no retrocedía", mientras que las de más abajo sí funcionaban.
   Causa: `MIGRATION_22_23` le puso `orden = 0` a TODAS las listas que ya existían (nunca antes
   tuvieron esa columna) — quedaron empatadas. Intercambiar el `orden` entre dos listas con el
   mismo valor (0 ↔ 0) es un no-op: no cambia nada en la base, así que la UI no se movía. Las
   únicas parejas que sí se movían eran las que tenían al lado una lista creada DESPUÉS de
   instalar esta actualización (esas sí reciben un `orden` correlativo vía
   `ListaDao.obtenerMayorOrden`). Arreglado con `MIGRATION_23_24`: reparte un `orden` único por
   espacio a partir de `fechaCreacion` DESC (mismo criterio con el que ya se ordenaban antes de
   esta ronda), así ninguna lista salta de lugar al aplicar la migración — solo deja de haber
   empates. `LulaDatabase` sube a versión 24.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`). `MIGRATION_23_24` se instaló y
verificó en la siguiente ronda (Novena, más abajo) una vez el dispositivo volvió a estar
disponible.

## Novena ronda — 6 puntos de uso real: alarma, medicamento, tareas vencidas y calendario (2026-08-11)

1. **Sonido de Alarma "sonó menos de un segundo y se cortó" con el teléfono inactivo**: ya
   había una sospecha documentada de esto (comentarios en `AlarmaSonidoService`/
   `NotificationSettingsUtils` de rondas anteriores, "eso puede cortar el sonido de una Alarma a
   mitad de reproducción") pero nunca se había reforzado con código. Confirmado por el usuario:
   con el teléfono en uso activo la Alarma suena completa; inactivo, se corta casi de inmediato
   — patrón clásico de que el sistema (sobre todo fabricantes agresivos con batería — el
   dispositivo de prueba es un Motorola) vuelve a dormir la CPU a mitad de la reproducción del
   `MediaPlayer`, o directamente mata el `Service` en segundo plano. Dos cambios:
   - `AlarmaSonidoService` toma un `PowerManager.PARTIAL_WAKE_LOCK` (máximo 2 minutos, de sobra
     para que suene y el usuario alcance a tocar "Detener") mientras reproduce — nuevo permiso
     `WAKE_LOCK` en el manifest. Esto es refuerzo de código; no reemplaza la excepción de
     optimización de batería que el sistema operativo puede seguir aplicando por su cuenta en
     fabricantes agresivos.
   - Nuevo banner proactivo en Hoy ("🔋 Tu celular puede apagar Lula en segundo plano...") — ya
     existía el atajo a Ajustes (`abrirAjustesDeOptimizacionBateria`) pero solo se veía si el
     usuario entraba por su cuenta a Ajustes, mismo patrón que el banner de permiso de
     notificaciones de la Séptima ronda.

2. **Medicamento cada 8 horas: aparecía una toma de más el primer día y desaparecía la última**:
   causa raíz confirmada leyendo `calcularHorariosPorIntervalo` — arma "todos los horarios de un
   día completo" dando la vuelta a la medianoche cuando el intervalo no llega justo a 24h (ej.
   desde las 14:00 cada 8h → `["14:00", "22:00", "06:00"]`, ese último "06:00" en realidad es del
   día SIGUIENTE). `ObtenerAgendaDelRangoUseCase` y `ObtenerMedicamentosDeHoyUseCase` mostraban
   los 3 horarios TAMBIÉN el primer día (antes de que arrancara de verdad el tratamiento a las
   14:00), y como `calcularFechaFinPorCantidadDosis` contaba el primer día como si tuviera los 3
   horarios completos, la fecha de fin quedaba un día antes de lo que correspondía — la última
   toma real (día 3, 06:00) directamente desaparecía. Arreglado centralizando TODA esta cuenta
   en un único lugar nuevo, `horariosParaFecha` (`HorariosMedicamentoUtils.kt`), usado ahora por
   Calendario, "Tomas de hoy" y el recordatorio del día siguiente (`RecordatorioReceiver.
   medicamentoSigueVigenteManana`) — antes cada uno tenía su propia copia de esta lógica, que ya
   se había desalineado una vez (visto en la Séptima ronda) y hubiera vuelto a pasar. Nueva
   función `horariosDelPrimerDia` (filtra los horarios que dieron la vuelta) y
   `calcularFechaFinPorCantidadDosis` corregida para contar la fecha de fin a partir de cuántas
   dosis caben de verdad en el primer día, no asumiendo que siempre caben todas. Por modo
   "según las comidas" no aplica (no hay una única `horaPrimeraDosis` de referencia ni da la
   vuelta a la medianoche), así que se dejó sin tocar ese camino.

3. **Tarea vencida de ayer sin ⚠️, una de hace varios días sí lo tenía**: en la sección de
   Tareas de Hoy, `HomeScreen` solo marcaba "vencida" (color rojo + ⚠️) comparando la
   `horaRecordatorio` (hora del día, ej. "09:00") contra la hora actual — una Tarea vencida por
   DÍA (fecha límite ya pasada) pero sin una hora de recordatorio configurada nunca se pintaba
   en rojo, sin importar cuántos días llevara vencida. `ActividadUi` ganó `fechaLimite`; ahora
   una Tarea también cuenta como vencida si su fecha límite ya pasó (`fechaLimite <
   inicioDeHoyEpochMillis()`), sin depender de que tenga una hora configurada — mismo criterio
   ya usado en `TasksListScreen`/`TaskDetailScreen`, solo que ahí faltaba replicarlo en Hoy.

4. **Una tarea vencida de ayer que se completa hoy debía marcarse en Calendario en HOY, no en
   ayer**: antes una Tarea siempre aparecía en Calendario en el día de su `fechaLimite`,
   estado incluido — completarla días después de vencida la seguía mostrando como "hecha" en su
   fecha límite original, no en el día real en que se hizo. `Actividad.fechaCompletado` ya
   existía (se graba al marcar, usado para el progreso de Hoy desde la Séptima ronda) pero
   Calendario no lo usaba. Ahora `ObtenerAgendaDelRangoUseCase` muestra una Tarea CONFIRMADA en
   el día de su `fechaCompletado`; solo mientras sigue pendiente (o si se omite) se queda en su
   `fechaLimite`.

5. **Completar una Tarea con la fecha real en que se hizo, no "ahora"**: pedido explícito del
   usuario — "para hacer esto solo debe hacerse desde calendario y recién poder completar fechas
   pasadas". `ActividadRepository.marcarEstado`/`MarcarActividadUseCase` ganaron un parámetro
   opcional `fechaCompletado: Long?` (default `null` = ahora mismo, mismo comportamiento de
   siempre en todos los demás lugares que ya llamaban a esto). Calendario (`VistaDia`) ahora
   muestra un checkbox junto a una Tarea SIN_CONFIRMAR **solo cuando el día que se está viendo
   es un día PASADO** (no hoy, no futuro) — al tocarlo, `CalendarViewModel.marcarTareaEnFecha`
   marca la Tarea como CONFIRMADO con `fechaCompletado` = la medianoche de ESE día, no la de
   ahora. Hábito no gana nada de esto a propósito (el usuario mismo aclaró que Hábito ya es "por
   día, si no se hizo no se marca" — no aplica el mismo concepto de "completar tarde").

6. **Análisis: ¿qué debería pasar si una toma de medicamento se atrasa varias horas?** — el
   usuario pidió analizar qué es lo correcto médicamente, no solo cómo lo hace la app. Revisando
   el código: `RecordatorioScheduler.proximoTrigger` siempre calcula la PRÓXIMA ocurrencia de la
   hora de reloj fija configurada (ej. "14:00" de mañana), nunca recalcula a partir de la última
   toma real; `MarcarTomaMedicamentoUseCase` solo graba el estado de esa toma puntual y no toca
   la programación de las siguientes. Es decir: **Lula ya mantiene el horario original fijo,
   nunca lo recorre** — que es el criterio correcto (indicación típica de farmacia/médico para
   una dosis olvidada: tomarla en cuanto se recuerde, pero si ya casi es hora de la siguiente,
   saltarla y seguir con el horario de siempre, nunca duplicar ni desplazar todo el resto del
   tratamiento — dejar que el horario "se corra" acumula el atraso y puede juntar dos tomas
   demasiado cerca). No hizo falta ningún cambio de código en este punto — se confirmó que el
   diseño ya construido en fases anteriores es el correcto.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus, sin uso activo al momento de instalar — `installDebug`, `EXIT_CODE=0`).

## Causa real del sonido de Alarma cortado — diagnosticado con `logcat` en vivo (2026-08-11, mismo día)

El usuario reportó que el punto 1 de la Novena ronda (WakeLock + banner de optimización de
batería) no arregló el problema — la Alarma seguía sonando "un ratito" y cortándose con el
teléfono descansando/bloqueado, igual que antes. En vez de seguir agregando mitigaciones a
ciegas, se armó una prueba controlada con `adb logcat` capturando en vivo mientras se reproducía
el bug (disciplina ya establecida en la sesión: diagnosticar con evidencia real del dispositivo,
no adivinar):

1. Se limpió el buffer de logcat (`adb logcat -c`) y se dejó una captura corriendo en segundo
   plano (`adb logcat -v time`).
2. El usuario programó un recordatorio nivel Alarma a pocos minutos, bloqueó el teléfono y lo
   dejó sin tocar. Reportó la hora exacta en que pasó (14:41): vibró, sonó un poco, se cortó,
   apareció el mensaje en pantalla.
3. Con la hora exacta, se filtró el log alrededor de las 14:41:00 y apareció la secuencia
   completa:
   - `14:41:00.076` — `MediaFocusControl: requestAudioFocus()` de `com.aqpseller.lulaapp`
     (arranca `AlarmaSonidoService`).
   - `14:41:00.127` — `MediaCodec` empieza a decodificar el `.wav` (`durationUs = 9400000`,
     coincide exacto con los 9.4s del archivo — el archivo en sí está sano, no es un problema de
     formato ni de codificación).
   - `14:41:00.238` — vibrador ON.
   - `14:41:00.301` — `ActivityTaskManager: START ... cmp=com.aqpseller.lulaapp/.MainActivity`
     — Android abre `MainActivity` **sola**, sin ninguna acción del usuario (esto es el
     `fullScreenIntent` disparándose automáticamente porque la pantalla estaba bloqueada).
   - `14:41:00.434` — **`NuPlayerDriver: stop()` y `reset()`** — el reproductor se detiene, apenas
     ~300ms después de haber arrancado. Coincide exactamente con la apertura automática de
     `MainActivity` de la línea anterior.

**Causa raíz real**: `RecordatorioReceiver.mostrarNotificacion()` armaba UN SOLO `PendingIntent`
(`pendingIntentContenido`) con `EXTRA_DETENER_ALARMA` puesto, y lo reusaba tanto para
`setContentIntent` (tocar la notificación — ahí sí tiene sentido detener la alarma, es una
acción real de la persona) como para `setFullScreenIntent` (que Android dispara **solo**, sin
ninguna acción del usuario, para mostrar la alerta sobre la pantalla bloqueada). Como
`MainActivity.detenerAlarmaSiCorresponde()` no distingue "la persona tocó la notificación" de
"Android me abrió solo por el full-screen intent", la app se detenía la Alarma a sí misma casi
al instante cada vez que el teléfono estaba bloqueado — que es justo la única condición bajo la
que Android dispara el `fullScreenIntent` automáticamente (con la pantalla desbloqueada/en uso,
Android NO lo dispara solo, por eso "si estoy usando el cel sí suena bien": ahí nunca se abre
`MainActivity` sin que la persona lo pida). Nada que ver con batería, Doze, ni con el archivo de
sonido — un bug de lógica propio, autoinfligido.

**Arreglado**: el `fullScreenIntent` ahora usa su propio `PendingIntent` separado (mismo
`destino`/`EXTRA_MOSTRAR_SOBRE_BLOQUEO`, pero SIN `EXTRA_DETENER_ALARMA`), con un request code
distinto (`"$claveNotificacion:pantallaCompleta".hashCode()`) para que no se pise con
`pendingIntentContenido`. El WakeLock y el banner de optimización de batería del punto 1 de la
Novena ronda se dejan tal cual — son mejoras defensivas razonables (protegen contra que el
sistema mate el proceso en fabricantes agresivos), pero **no eran la causa real** de este bug
puntual; la causa real era este error de lógica en el propio código.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

### Segundo hallazgo, encadenado — nada detenía la Alarma desde las pantallas de acción

Con el arreglo anterior instalado, el usuario probó de nuevo: la Alarma ya no se cortaba sola,
pero ahora sonaba **indefinidamente** — ni tocando botones en la pantalla que se abre, ni con la
tecla de encendido, la cortaban. Se la detuvo de emergencia por `adb shell am force-stop` para
no dejarlo sonando, y se revisó el código de esas pantallas.

Causa: `RecordatorioAccionViewModel` (Hábito/Tarea, pantalla "Ya lo hice" / "Recuérdame en 15
minutos" / "Ver en Hoy") y `AccionTomaViewModel` (Medicamento, "Ya la tomé" / "La omito" / "Ver
en Mi salud") solo cancelaban la **notificación visual** al abrirse (`NotificationManagerCompat.
cancel(...)`) — nunca llamaban a `AlarmaSonidoService.intentDetener`, que es lo único que de
verdad para el `MediaPlayer` en loop. Ese cancelar-la-notificación-al-abrir era, en sí, un
intento anterior de tapar el mismo síntoma (el comentario que tenía ese código lo decía
textual: "el sonido seguía sonando... porque el fullScreenIntent no pasa por el flujo normal") —
pero apagaba la vista, no el sonido. Ninguno de los 3 botones de cada pantalla llamaba a
detenerlo tampoco; y en Medicamento, el botón "Ver en Mi salud" ni siquiera pasaba por el
`ViewModel` (llamaba directo a `onListo`), así que tampoco marcaba nada.

**Arreglado**: se sacó el `cancel()` del `init` de ambos ViewModels (dejarlo ahí habría sido
volver a apagar la Alarma sola al abrir la pantalla — el mismo bug de la sección anterior) y se
movió a cada una de las 6 acciones reales (`marcarHecho`/`posponer`/`irAHoy` en Hábito-Tarea;
`marcar(CONFIRMADO)`/`marcar(OMITIDO)`/`verEnMiSalud` en Medicamento — esta última, nueva,
reemplaza el `onListo` directo del botón "Ver en Mi salud"), cada una llamando a
`AlarmaSonidoService.intentDetener` con la misma clave de notificación que usó
`RecordatorioReceiver` al crearla. Así la Alarma se corta justo cuando la persona SÍ hace algo
en esa pantalla — ni antes (bug anterior) ni nunca (este bug).

**Cita queda afuera a propósito**: su recordatorio también puede ser nivel Alarma, pero navega
directo a "Mi salud" (lista general), sin una pantalla de acción dedicada como Hábito/Tarea/
Medicamento — no hay un único botón al que atarle "se hizo algo con este recordatorio en
particular". Por ahora la única forma de cortarla ahí es el botón "🔕 Detener alarma" de la
propia notificación (que nunca dejó de funcionar, es un `PendingIntent` aparte). Construir una
pantalla de acción dedicada para Cita es una extensión futura si hace falta — no se construyó
ahora porque no fue lo reportado.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus). Pendiente: confirmación del usuario de que, tocando una acción real en la
pantalla que se abre, la Alarma se corta al instante.

## Cuarta ronda del día — dato viejo de medicamento, checkbox de Citas, diseño de Metas (2026-08-11)

1. **El medicamento de 6 tomas seguía sin mostrar nada el día 13 — confirmado como dato viejo,
   no un bug del código actual**: en vez de asumir, se sacó una copia real de la base del
   dispositivo (`adb exec-out run-as com.aqpseller.lulaapp cat databases/lula.db` — con
   `exec-out`, no `adb shell` normal, porque este último mete un pseudo-terminal que corrompe
   binarios; y sin arrastrar `-wal`/`-shm` porque copiarlos por separado con `cat` los deja
   inconsistentes entre sí, mejor solo el `.db` principal) y se consultó con `sqlite3` local
   (el del Android SDK, `platform-tools/sqlite3.exe`, ya que el dispositivo no tiene el binario
   instalado). La fila real de ese medicamento tenía `fechaInicio = 2026-08-11`,
   `cantidadDosisTotal = 6`, pero `fechaFin = 2026-08-12` — la fecha vieja, calculada por la
   fórmula DE ANTES del arreglo de esta misma sesión (esa actividad se había creado en una
   prueba anterior a que el fix quedara instalado). Con `fechaFin` desactualizado, `horariosParaFecha`
   (ya corregido) hace exactamente lo que debe con el dato que tiene: como el día 13 queda
   DESPUÉS de un `fechaFin` que ya de por sí está mal, no devuelve nada para ese día — el bug no
   está en la lógica actual, está en un dato ya guardado con la lógica vieja. Solución: editar y
   volver a guardar ese medicamento (dispara `ActualizarMedicamentoUseCase`, que ya usa la
   fórmula corregida) para que recalcule bien, o borrarlo y crearlo de nuevo.

2. **Checkbox confuso en sesiones de Cita**: el usuario reportó que el "⬜"/"✅" al inicio de
   cada fila de sesión (`FilaSesionCita`) parecía un checkbox tocable pero no hacía nada —
   la acción real de marcar vivía en un botón de texto aparte ("✅ Cumplida") más abajo,
   desconectado visualmente del ícono. Arreglado: para una sesión que sí se puede marcar (no
   futura, no ya omitida), el emoji decorativo se reemplazó por un `Checkbox` real de Material —
   tocarlo marca (`onMarcarCumplida`) o desmarca (`onDeshacer`) directamente, eliminando el
   botón "✅ Cumplida" ahora redundante. Sesión futura (nada que marcar todavía) y sesión
   omitida (no es un simple sí/no) siguen mostrando el emoji fijo, sin fingir que se puede
   tocar — solo se volvió interactivo lo que de verdad lo es.

3. **Diseño de Metas — el usuario pidió opinión, no construcción todavía**: antes de proponer
   algo, se revisó qué ya existe (más de lo esperado): `Meta.fechaLimite` opcional ya existe y
   ya se puede elegir al crear/editar; `GoalsListViewModel` ya ordena por fecha más próxima
   primero (sin fecha al final) y ya separa "✅ Completadas" en su propia sección al final de la
   lista; ya existe un aviso "⏳ Faltan N día(s)" para metas "urgentes" (últimos 7 días o
   vencidas, `MetaConProgreso.esUrgente`). Lo que falta y coincide con el pedido del usuario: en
   Hoy se muestran TODAS las metas en progreso sin filtrar (`metasEnProgreso` no distingue
   urgentes de las que faltan meses) — con muchas metas eso ensucia Hoy, que es justo el
   problema que anticipó el usuario. Recomendación dada (pendiente de confirmación antes de
   construir): (a) en Hoy, filtrar a solo las urgentes (reusar el umbral de 7 días que ya
   existe), el resto vive solo en la pestaña Metas; (b) atajos rápidos de fecha límite ("en 1
   semana"/"1 mes"/"3 meses"/"1 año") además del calendario actual; (c) un "🔜 Aplazar" rápido
   desde la lista de Metas, sin entrar a editar completo; (d) mostrar el conteo en el
   encabezado ("✅ Completadas (8)") en vez de solo el título, para reforzar la sensación de
   avance que pidió el usuario. **No construido todavía** — se dio la recomendación y se espera
   confirmación del usuario antes de tocar código, mismo patrón que la conversación de FODA.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus, sin uso activo al momento de instalar).

## Construcción completa de Metas — categoría, fechas rápidas, aplazar, agrupar, aviso (2026-08-11)

A diferencia del resto de esta ronda, esto no fue un bug — el usuario confirmó explícitamente el
alcance completo con una pregunta de selección múltiple antes de tocar código (mismo patrón que
FODA, pero acá sí se pidió construirlo). Seis partes:

1. **Categoría (6 preguntas de ayuda) → etiqueta real de la meta**: `Meta` gana `categoria:
   CategoriaMeta?` (nuevo enum `HACER/SER/VER/TENER/IR/COMPARTIR`, mismo orden que
   `PREGUNTAS_AYUDA_META`, que ya existía desde el 2026-08-01 pero solo como texto de
   referencia). `CrearMetaScreen` ahora deja elegir a cuál de las 6 preguntas responde la meta
   (`FilterChip` por pregunta) y muestra 3 ejemplos reales por categoría (tomados de los
   ejemplos que trajo el usuario) cuando se selecciona una — antes las preguntas eran solo texto
   fijo, sin ninguna forma de usarlas para clasificar. `MetaEntity.categoria` (String nullable),
   `MIGRATION_24_25` (`ALTER TABLE meta ADD COLUMN categoria TEXT`), `LulaDatabase` sube a v25.

2. **Selector de fecha rápido reutilizable**: nuevo `core/ui/SelectorFechaRapida.kt` — botones
   "+1 semana"/"+1 mes"/"+3 meses"/"+1 año" (siempre contados desde hoy) más "📅 Elegir fecha"
   (el `DatePicker` de siempre para una fecha puntual). Reemplaza el selector de fecha límite
   de `CrearMetaScreen` (antes solo calendario) y se reutiliza tal cual para "🔜 Aplazar".

3. **Aplazar sin pasar por editar completo**: nuevo `AplazarMetaUseCase`, `MetaRepository.
   aplazarFechaLimite` (+ `MetaDao.actualizarFechaLimite`, cambia solo esa columna) — botón
   "🔜 Aplazar" en `MetaDetailScreen`, que abre el mismo `SelectorFechaRapida` de arriba.
   Reprograma el aviso sonoro (punto 5) si estaba activado, para la fecha nueva.

4. **Completadas agrupadas por categoría con conteo**: `GoalsListScreen` antes tenía una sola
   lista plana con "✅ Completadas" como único separador; ahora agrupa las completadas por
   `categoria` (una sección por cada una de las 6, más "Sin categoría" al final si aplica), cada
   una con su propio conteo ("🎯 Qué quiero tener (3)"), y un total arriba de todo
   ("✅ Completadas (8)"). Motivo explícito del usuario: ver varias metas cumplidas juntas, con
   número, refuerza la sensación de avance.

5. **Aviso sonoro al llegar la fecha límite**: `Meta` gana `avisarAlVencer: Boolean`. Meta vive
   en su propia tabla, separada de `Actividad` — no puede pasar por el camino normal de
   recordatorios (`RecordatorioReceiver` espera un `TipoActividad` real ligado a una fila de
   `Actividad`). Se armó un camino aparte, mismo patrón bypass ya usado para "cierre de día" y
   "franja del día": `RecordatorioScheduler.programarMeta`/`cancelarMeta` (hora fija 09:00, un
   solo disparo — la fecha límite de una meta no se repite) + nuevo extra `EXTRA_META_ID` en
   `RecordatorioReceiver`, chequeado temprano en `onReceive` antes del camino normal, con su
   propia notificación ("🎯 ¡Hoy vence tu meta!") y su propio destino (`LulaDestinations.
   metaDetalle`). Se programa/cancela en `CrearMetaUseCase`, `ActualizarMetaUseCase` (cancela y
   reprograma siempre, más simple que comparar qué cambió — la hora fija hace que sea barato),
   `EliminarMetaUseCase` y `AplazarMetaUseCase`.

6. **Hoy ya no se llena de metas**: `HomeUiState.metasEnProgreso` filtraba antes TODAS las metas
   en progreso sin importar la fecha; ahora solo las `esUrgente` (últimos 7 días o ya vencida,
   ese umbral ya existía). El resto de las metas (sin fecha, o con fecha lejana) ya no aparece
   en Hoy — solo viven en la pestaña Metas, tal como pidió el usuario para no llenar Hoy si
   hubiera muchas metas.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus, sin uso activo al momento de instalar).

## Rutinas/Tareas sin filtrar (crecimiento sin límite) + rediseño completo de Metas (2026-08-12)

1. **Selector de "¿Qué actividades agrupa?" en Rutina crecía sin límite**: `CrearRutinaViewModel`
   traía TODOS los hábitos y tareas alguna vez creados (`ObtenerHabitosUseCase` +
   `ObtenerTareasUseCase`, sin ningún filtro) para elegir qué agrupar en una rutina nueva —
   con el tiempo, cada tarea ya completada seguía apareciendo ahí para siempre. Arreglado:
   se excluyen las Tareas con `estado == CONFIRMADO` (Hábito no se filtra, es recurrente —
   "ya se cumplió hoy" no lo descalifica de una rutina que se repite mañana).

2. **"HECHAS" en Tareas crecía sin límite, y la vista Matriz nunca filtraba nada**: la lista de
   Tareas ya separaba pendientes/hechas (de una ronda anterior), pero "✅ HECHAS" acumulaba TODO
   el historial de tareas completadas, sin importar hace cuánto — dejaba de sentirse "lo
   vigente". Ahora "HECHAS" solo muestra lo completado HOY (`Actividad.fechaCompletado`); lo
   completado otro día se sigue viendo en Calendario, en su propio día. La vista Matriz (🗂️,
   Eisenhower) tampoco filtraba nada — mostraba tareas completadas hace meses ocupando un
   cuadrante para siempre; ahora excluye cualquier tarea ya completada (no aporta a decidir qué
   hacer, sin importar el día).

3. **Rediseño completo de Crear Meta — formulario compacto tipo Medicamento**: a pedido
   detallado del usuario, se reemplazó el formulario largo (todo visible, scroll) por filas
   compactas que abren un `ModalBottomSheet` al tocarlas (mismo patrón `SelectorRow` ya usado en
   Crear Medicamento):
   - **Categoría** es ahora el primer paso obligatorio del flujo — nada más se muestra hasta
     elegir una de las 6 preguntas (antes era una ayuda opcional colapsable). Al elegirla, la
     hoja muestra la pregunta con sus ejemplos y consejos de redacción.
   - Elegida la categoría, aparece la pregunta como título y el campo "Nombre" (con dictado)
     justo debajo, para responderla.
   - Filas: "¿Cómo la vas a medir?" (con el objetivo movido adentro de esa hoja, junto al
     selector de hábito si aplica), "Área de vida", "Fecha límite", "Recordatorio".
   - **Fecha límite dejó de ser opcional**: siempre arranca con un valor por defecto (+1 mes
     desde hoy) en vez de forzar a elegir uno antes de poder seguir — el usuario explicó que
     "uno debe ponerse una fecha límite". La hoja de fecha reutiliza `SelectorFechaRapida`
     (semana/mes/3 meses/año + calendario).
   - **Recordatorio pasó de un simple booleano ("avisar sí/no") a nivel completo Silencioso/
     Sonido/Alarma**, igual que Hábito/Tarea/Medicamento/Cita (`NivelRecordatorioSelector`
     reutilizado). `Meta.avisarAlVencer` → `Meta.nivelRecordatorio: NivelRecordatorio`,
     `MIGRATION_25_26` agrega la columna nueva (la vieja se deja sin usar, no vale la pena
     reconstruir la tabla por una columna huérfana). Como el nivel Alarma necesita el mismo
     tratamiento especial que el resto de la app (pantalla completa + `AlarmaSonidoService`),
     se extrajo esa lógica a una función compartida nueva,
     `RecordatorioReceiver.mostrarNotificacionConNivel`, usada tanto por el camino normal
     (`TipoActividad`) como por el de Meta — evita tener una segunda copia de la rama de Alarma
     que se pudiera volver a desalinear (ya pasó una vez hoy con el bug del `fullScreenIntent`
     compartido). `MetaDetailViewModel` también gana el mismo corte de Alarma en sus acciones
     reales (aplazar/agregar progreso/eliminar) que ya tienen Hábito/Tarea/Medicamento, por el
     mismo motivo de esta mañana.

4. **Rediseño completo de "Ver mis metas"**: antes eran dos listas separadas (pendientes con
   barra de progreso, completadas agrupadas por categoría al final); ahora TODO se agrupa por
   categoría desde el principio (pendientes y completadas juntas dentro de cada grupo, pendientes
   primero), para repasar las metas rápido y seguidas como pidió el usuario. Cada fila reemplaza
   la barra de progreso (reportada como que "no llama la atención") por un contador compacto
   tipo "(1/3)" — con ✅ y en el color primario cuando llega a completarse — ubicado a la derecha
   junto a la fecha límite, bien separado del nombre a la izquierda. Arriba de todo, un resumen
   "✅ N de M completadas" da la sensación de avance de un vistazo.

5. **Metas urgentes en Hoy, mismo estilo compacto + atajo directo a reprogramar**: la sección de
   Hoy tenía su propia barra de progreso distinta a la de la lista completa; ahora usa el mismo
   contador "(x/y)". El aviso "⏳ Faltan N día(s)"/"Venció hace N día(s)" ahora viene acompañado
   de un botón "🔜 Reprogramar" a su lado (lleva al detalle de la meta, donde ya vive "🔜
   Aplazar" desde la ronda anterior) — antes solo era texto, sin ninguna acción directa desde
   Hoy cuando una meta se está por vencer.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus) en la siguiente ronda, una vez reconectado.

## Causa real de "los recordatorios no suenan al día siguiente" — diagnosticado en vivo, no adivinado (2026-08-13)

El usuario reportó que TODOS los tipos de recordatorio (Hábito, Tarea, Cita, Medicamento,
franjas del día) sonaban bien al probarlos en el momento, pero dejaban de sonar "al día
siguiente" — sin mensaje, sin sonido, nada. Su propia sospecha: "parece que pasara a segundo
plano y se queda dormido". Se armó una batería de pruebas reales con el dispositivo conectado,
sin asumir nada:

1. `adb shell dumpsys alarm | grep aqpseller` — confirmó que las alarmas SÍ estaban bien
   programadas en el sistema (ej. una para el día siguiente a la hora exacta configurada,
   `whenElapsed=+23h45m...`). Esto ya descartaba "el código nunca programa nada para más
   adelante".
2. `adb shell uptime` — el teléfono llevaba **20 días sin reiniciarse**. Descarta "se reinició
   de noche y `BootReceiver` no alcanzó a reprogramar nada".
3. `adb shell dumpsys package com.aqpseller.lulaapp` — `stopped=false` (la app NO estaba en
   estado "detenida" en ese momento) y `am get-standby-bucket` devolvió `5` (EXEMPTED, el mejor
   bucket posible). `dumpsys deviceidle whitelist` confirmó a Lula en la lista blanca de
   optimización de batería.
4. **Prueba controlada de reposo forzado**: se programó una Alarma real de prueba (Hábito, hoy
   21:07) y, apenas antes de la hora, se forzó el modo de reposo más profundo de Android
   (`adb shell dumpsys deviceidle force-idle`) mientras se capturaba `logcat` en vivo. La alarma
   sonó exactamente a las 21:07:00.070 (proceso arrancado por el sistema específicamente para
   entregar la alarma, `Start proc ... for broadcast RecordatorioReceiver`), con sonido, aviso y
   pantalla completa — todo funcionando perfecto incluso en el reposo más agresivo que existe en
   Android. Revisando `dumpsys alarm` de nuevo después, la MISMA alarma ya se había vuelto a
   programar sola para el día siguiente a la misma hora (`2026-08-14 21:07:00`) — confirma que la
   cadena de "reprogramarse a sí misma" (usada por Hábito, Medicamento y franjas) también
   funciona bien en el código.
5. Con el "Doze" estándar de Android descartado (funciona incluso forzado), el usuario aclaró el
   dato que resolvió el caso: las fallas reales pasan "usando el celular, no está durmiendo" —
   o sea, ni siquiera hace falta que el teléfono esté inactivo para que falle. Esto apunta a algo
   que canceló las alarmas ANTES, en algún momento entre que se programaron y que debían sonar,
   no a un problema de entrega en el momento exacto.

**Causa raíz**: algunos fabricantes (Motorola confirmado en el dispositivo de prueba) "fuerzan
detener" apps en segundo plano para ahorrar batería de forma más agresiva que el Android
estándar — esto cancela TODAS las alarmas pendientes de `AlarmManager`, exactamente igual que un
reinicio del teléfono, pero **sin disparar `BOOT_COMPLETED`**. Como `BootReceiver` (la única
reparación que existía) solo se activa con un reinicio real, un "forzar detener" silencioso
dejaba todos los recordatorios rotos para siempre — ni volver a abrir la app los reparaba, porque
nada dentro de la app revisaba/reprogramaba nada al abrirse, solo al recibir `BOOT_COMPLETED`.

**Arreglado**: se sacó la lógica de "reprogramar todo" de `BootReceiver` a un nuevo caso de uso
compartido, `ReprogramarTodosLosRecordatoriosUseCase` (de paso, se le sumó Meta, que
`BootReceiver` nunca reprogramaba — hueco encontrado al mover el código a un solo lugar). Ahora
se llama tanto desde `BootReceiver` (reinicio real) como desde `AppViewModel.init` (cada vez que
se abre la app, sin bloquear `isReady`) — así que con solo volver a abrir Lula alcanza para
reparar todos los recordatorios, sin depender de que el usuario reinicie el teléfono.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Rediseño de Ajustes en tarjetas (2026-08-13)

El usuario compartió 3 capturas de pantalla de otras apps (cuenta de Honor, una app de botica,
una app de salud) señalando cómo agrupan sus ajustes/cuenta: cada sección vive en su propia
tarjeta (rectángulo redondeado), con filas ícono + texto + flecha, en vez de una sola lista
continua. `SettingsScreen.kt` (antes: todo suelto en un `Column` con solo espacios entre
secciones) se reagrupó siguiendo ese mismo patrón — nuevo composable local `TarjetaAjustes`
(envuelve cualquier contenido en un `Card` con su propio título), usado para 5 grupos:

- **🔔 Recordatorios y permisos** — notificaciones/alarmas exactas/optimización de batería
  (solo si faltan) + sonido de recordatorios.
- **🗓️ Revisión y cierre del día** — día de revisión semanal + recordatorio de cerrar el día.
- **🔔 Recordarme revisar Lula** — las 3 franjas (mañana/tarde/noche).
- **✅ Marcar en Hoy** — sonido al marcar un check.
- **🧭 Personalizar mi navegación** — las 3 posiciones configurables de la barra inferior.

Mismo contenido y misma lógica de antes (nada de esto tocó `SettingsViewModel`) — el cambio es
puramente de agrupación visual, para que la pantalla se pueda escanear de un vistazo en vez de
sentirse como un formulario largo sin secciones.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Perfil en tarjetas + separar "marcar" de "ver/editar" en Hoy (2026-08-13)

Siguiendo con el mismo pedido de elegancia visual (mismas 3 capturas de referencia), tres
cambios más:

1. **`TarjetaAjustes` se sacó de `SettingsScreen.kt`** a un composable compartido,
   `core/ui/TarjetaSeccion.kt` (mismo comportamiento, nombre genérico porque ya no es solo de
   Ajustes) — para no duplicar el mismo patrón de tarjeta en cada pantalla que lo necesite.

2. **`ProfileScreen.kt` se reagrupó en tarjetas** con `TarjetaSeccion`: "🧭 Mi crecimiento" (Mi
   propósito), "👥 Mi espacio" (nueva — Círculo de cuidado y Familia/Espacios), "🍽️ Horarios de
   comida", "🔒 Privacidad y legal", "⚠️ Zona de peligro". Antes Círculo de cuidado y
   Familia/Espacios no aparecían en Perfil para nada — solo se llegaba a ellos desde el menú "⋮"
   de la barra superior o, si el usuario los había configurado, desde una posición del bottom
   bar. `ProfileScreen` ganó los parámetros `onVerCirculoCuidado`/`onVerFamilia`, cableados en
   `LulaNavHost.kt` a las rutas `CIRCULO_CUIDADO`/`FAMILIA` que ya existían.

3. **`HomeScreen.kt`: separación visual entre lo que se marca y lo que solo se abre.** A pedido
   del usuario — antes una fila de Cita/Fecha importante/Meta (clickeable, lleva al detalle) se
   veía visualmente igual que una fila de Hábito/Tarea (con checkbox), sin ninguna pista de que
   unas se tocan para ver/editar y otras se marcan. Se agregó una flecha "›" al final de cada
   fila de `seccionAgenda` (Citas/Fechas importantes) y `seccionMetas`, mismo lenguaje visual que
   ya usa `SectionLinkRow` en el resto de la app. No se tocó ninguna fila con checkbox
   (Hábitos/Tareas/Medicamentos) — esas ya se distinguen por tener el checkbox mismo.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Hoy en tarjetas + check verde + resaltado de vencidos (2026-08-13)

Tercera ronda del mismo pedido de elegancia visual. En `HomeScreen.kt`:

1. **Todo lo que se marca (checkbox) va en una sola tarjeta "✅ Para marcar hoy"** — Mañana,
   Tarde, Noche, Tareas y Medicamentos pendientes, antes sueltos uno tras otro en el
   `LazyColumn`. Solo aparece si hay al menos un pendiente.
2. **Todo lo que solo se abre para ver/editar va en otra tarjeta "📌 Metas y agenda de hoy"**
   — Metas, Fechas importantes y Citas. Mismo criterio de "marcar vs no marcar" pedido para
   Perfil/Ajustes, ahora aplicado también a Hoy: dos tarjetas separadas en vez de una lista
   plana donde checkbox y flecha se mezclaban visualmente.
3. **"✅ Ya hechos hoy" pasó a ser su propia tarjeta redondeada** (antes solo tenía un divisor
   arriba, ahora la tarjeta misma cumple ese rol — se quitó el divisor porque ya no hacía falta).
4. **El bloque de enlaces del final ("Ver tareas", "Ver calendario", etc.) pasó a la tarjeta
   "🔎 Explorar más"**.
5. **Checkbox marcado ahora es verde** (`LulaHabito`, el mismo verde de "hábitos/crecimiento"
   de la paleta) en vez del violeta primario de Material por defecto — a pedido del usuario, que
   lo notó como "medio violeta" y pidió verde para que se lea como "confirmación", no solo como
   el color de acento genérico de la app. Se centralizó en `coloresCheckMarcar()` (HomeScreen) y
   se aplicó también al `Checkbox` de `TomaAccionRow.kt` (compartido con "Mi salud" y
   `AccionTomaScreen`), para no tener dos checks de color distinto en la misma app.
6. **Las filas vencidas (hora ya pasada, sin marcar) ahora tienen fondo resaltado**
   (`colorScheme.errorContainer` + esquinas redondeadas), no solo texto en rojo como antes — a
   pedido del usuario, para que sea fácil detectar de un vistazo cuál toca resolver ahora mismo
   y pasar a check verde. Aplicado en `ActividadesSeccion` (Hábitos/Tareas), `AgendaSeccion`
   (Citas/Fechas importantes) y `TomaAccionRow` (Medicamentos) — mismo criterio "vencida" que ya
   existía en cada una, solo se sumó el fondo.

Para que las secciones antes sueltas (`seccionActividades`, `seccionAgenda`, `seccionMetas`,
`seccionMedicamentos`) pudieran vivir dentro de una sola `Card`, se convirtieron de extensiones
de `LazyListScope` (`item`/`items`) a composables normales (`ActividadesSeccion`, `AgendaSeccion`,
`MetasSeccion`, `MedicamentosSeccion`) que se recorren con `forEach` — aceptable porque las
listas de un solo día son chicas, no hace falta que sean lazy.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Ícono de Hábito: de "✅" a "🌱" (2026-08-13)

Con el checkbox ahora verde (ver sección anterior), el usuario notó que la fila de un Hábito
tenía dos símbolos de check pegados: el emoji de tipo ("✅") justo al lado del checkbox real
("✅" cuando está marcado) — confuso, parecían dos checks compitiendo. Se cambió el emoji de
Hábito a "🌱" (semilla), reusando el mismo símbolo que ya usa Lula para "crecimiento" en otros
lados (el estado vacío de Hoy, y el verde de la paleta ya está descrito como "hábitos,
crecimiento"). El checkbox se queda tal cual — solo se cambió el ícono que identifica el tipo.

Como el emoji de tipo Hábito estaba duplicado en varios lugares en vez de vivir en un solo sitio
(la función compartida `emojiTipoActividad()` existe justo para evitar esto, pero no todos los
lugares la usan todavía), el cambio se aplicó a mano en los 7 sitios encontrados:
`TipoActividadEmoji.kt` (la función compartida, usada en Hoy/Calendario), `OpcionBottomBar.kt`
(chip/ícono de la barra inferior), `AddMenuSheet.kt` (menú "+"), `HabitoEmoji.kt` (ícono
automático por palabra clave del hábito — el símbolo de respaldo cuando ninguna palabra
coincide), `RecordatorioReceiver.kt` (copia duplicada del mapeo para notificaciones, pendiente
de consolidar en el futuro), `RecordatorioAccionScreen.kt` (ícono grande en la pantalla de
Hecho/Posponer) y `CrearHabitoScreen.kt` (botón "Ver mis hábitos"). No se tocó el "✅" que
significa "estado confirmado" (`EstadoActividad.CONFIRMADO` en `emojiEstadoActividad()`,
`CitaDetailScreen.kt`, `MedicamentoDetailScreen.kt`) — ese es un símbolo distinto, compartido por
todos los tipos de actividad, no específico de Hábito.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Ícono de Tareas en "Personalizar mi navegación" era el de Calendario (2026-08-13)

El usuario notó que el chip de "Tareas" en Ajustes → Personalizar mi navegación
(`OpcionBottomBar.TAREAS`) usaba "📅" — el mismo símbolo de Calendario, aunque Tareas en todo el
resto de la app (menú "+", `TipoActividadEmoji`, enlaces de Hoy) ya usa "📝". Se corrigió ese
único punto para que coincida con el resto — no era una decisión de diseño nueva, era una
inconsistencia real (`OpcionBottomBar.kt` nunca se alineó con `TipoActividadEmoji.kt` cuando se
definieron por separado). Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e
instalado en el dispositivo real (moto g(9) plus).

## Ícono de Citas: de "📅" a "🩺" (2026-08-13)

Mismo motivo que Tareas arriba — Citas usaba "📅", el mismo símbolo de Calendario. Se cambió a
"🩺" en los 4 lugares donde vivía: `TipoActividadEmoji.kt` (función compartida), `AddMenuSheet.kt`
(menú "+"), `RecordatorioReceiver.kt` (notificaciones) y `HealthScreen.kt` ("Nueva cita"). Esto
dejó "🩺" ocupado por Citas, así que la tarjeta "👥 Mi espacio" de `ProfileScreen.kt` (que usaba
"🩺" para "Círculo de cuidado", agregado el mismo día) se realineó a "👥" — que además ya era el
símbolo establecido para Círculo de cuidado en `OpcionBottomBar.kt` (chip de "Personalizar mi
navegación"), así que de paso corrigió una inconsistencia que ya existía entre esos dos lugares.
Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Recordatorios que seguían sonando después de terminados/eliminados — diagnóstico y arreglo de fondo (2026-08-14)

El usuario reportó dos casos: (1) un medicamento "cada hora" cuyo tratamiento ya había
terminado volvió a sonar cada hora un día después de vencido; (2) un medicamento "cada 8 horas"
que se eliminó para que dejara de sonar, siguió sonando igual (y ya no aparece en Calendario,
lo cual es correcto porque de verdad se borró). Se diagnosticó leyendo el código real de punta a
punta, no adivinando:

**Causa del caso 1**: `ReprogramarTodosLosRecordatoriosUseCase` (agregado el 2026-08-13 para el
bug de "los recordatorios se duermen") reprograma TODOS los `horariosCalculados` de cada
Medicamento activo cada vez que se abre la app, sin revisar `fechaFin` — a diferencia del
recordatorio que se auto-reprograma solo al sonar (`RecordatorioReceiver.reprogramarMedicamentoSiVigente`,
que sí la revisa desde una ronda anterior). Un medicamento cuyo tratamiento ya terminó nunca se
"desactiva" solo (`activa` es un toggle manual, no se apaga con la fecha), así que cada apertura
de la app volvía a armar sus 24 alarmas de "cada hora" enteras. Arreglado: ahora usa
`horariosParaFecha(detalle, hoy)` — el mismo filtro que ya usa "Medicamentos de hoy" — así que
si `fechaFin` ya pasó, no se programa nada.

**Causa del caso 2**: `EliminarActividadUseCase` cancelaba la alarma diaria normal de cada
horario del medicamento, pero NO la cadena de "insistir" (recordatorio persistente,
`programarRenotificacionMedicamento`) si estaba activada — esa cadena es una alarma aparte, con
su propia clave (`actividadId:horario:renotif`). Si el medicamento eliminado tenía insistencia
prendida, esa cadena seguía viva. Arreglado: ahora usa `cancelarMedicamento()` (que cancela
ambas) en vez de solo `cancelar()`.

**Arreglo de fondo, no solo el síntoma**: además de las dos causas puntuales, se agregó una
guardia general en `RecordatorioReceiver` — antes de mostrar CUALQUIER recordatorio de Hábito/
Tarea/Medicamento/Cita/Fecha importante, ahora se revisa el estado real de la actividad
(`debeMostrarRecordatorio`): si ya no existe (se borró), está pausada, ya se marcó como hecha, o
(Medicamento) su tratamiento ya no está vigente hoy — no se muestra nada y no se reprograma más.
Así, ninguna vía de cancelación (eliminar, marcar, editar, pausar) necesita ser perfecta: aunque
alguna se le escape un caso, la alarma que ya quedó armada en `AlarmManager` se autocorrige la
próxima vez que suena, en vez de seguir sonando indefinidamente. Se aplicó el mismo criterio a
la cadena de "insistir" de Medicamento (`manejarRenotificacionMedicamento`), que antes solo
revisaba el estado de la toma y no si la actividad seguía existiendo — una toma sin registro
(porque el medicamento ya no existe) se leía igual que "sin confirmar" y seguía insistiendo.

**Rastro de eliminados en Calendario**: a pedido del usuario ("si la elimino debe quedar
registro que fue eliminado también, para que quede rastro esto en calendario nomas"), se
aprovechó la auditoría que ya existe desde el MVP (`historial_cambios`, escrita en cada método
de escritura de los repositorios — ver "Lecciones de MayiaApp aplicadas") en vez de construir
algo nuevo. Nuevo método `ActividadRepository.obtenerEliminadosDeRango(espacioId, desde, hasta)`
lee `historial_cambios` donde `entidad = "actividad"` y `accion = "ELIMINAR"` en el rango,
deserializa el `ActividadEntity` que quedó guardado en `valoresAntesJson` (de ahí sale
nombre/tipo) y arma un `ItemAgenda` con el nuevo campo `eliminado = true`, puesto en el día en
que se eliminó (no en su fecha original — no se intenta reconstruir todo el rango que ocupaba
antes, solo dejar la huella de que existió y se borró). `ObtenerAgendaDelRangoUseCase` lo suma
al resto de la agenda. En `CalendarScreen.kt`, una fila `eliminado` se ve apagada (color
`onSurfaceVariant`), con "🗑️" en vez del emoji de estado, sin checkbox y sin poder tocarla (no
hay ningún detalle al que ir, ya no existe).

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`).

## Botón "Hoy" confuso en Historial de Finanzas (2026-08-14)

El usuario notó que en "Ver historial completo" (Finanzas), el título del mes (ej. "Agosto
2026") siempre tenía un botón "Hoy" debajo, incluso viendo el mes actual — parecía otro dato del
mes, no un atajo para volver a hoy. `FinancesHistoryScreen.kt` ya tenía precedente para esto:
el propio Calendario (`CalendarScreen.kt`) resuelve el mismo problema mostrando "Ir a hoy" SOLO
cuando no se está viendo el período actual. Se aplicó el mismo criterio acá: el botón ahora solo
aparece si `mesVisible` no es el mes/año de hoy, y se renombró a "Ir a hoy" (mismo texto que
Calendario) para que sea inconfundible que es una acción, no una etiqueta.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`).

## Ronda de feedback de uso real — 5 puntos, con datos reales del dispositivo (2026-08-14)

Antes de tocar código, se sacó la base de datos real del dispositivo (`adb exec-out run-as
com.aqpseller.lulaapp cat databases/lula.db`) para verificar cada reporte contra datos reales en
vez de adivinar — misma disciplina que el diagnóstico de alarmas de días atrás.

**1. Racha global (🔥 en la barra superior, junto a "💰") y 2. racha de Hábitos — ninguna es un
bug, son dos mecánicas distintas y el usuario las vio en 0 por el mismo motivo.** La racha
global (`ObtenerProgresoDeHoyUseCase.calcularRachaActual`) cuenta días CONSECUTIVOS con "Cerrar
mi día" hecho Y al menos una actividad cumplida — empieza a contar desde HOY hacia atrás, así
que si hoy todavía no se cerró el día, se corta ahí mismo y muestra 0, aunque ayer y antes haya
una racha real en curso. La racha de un Hábito (`ObtenerHistorialHabitoUseCase.calcularRacha`)
es análoga pero por hábito: cuenta días CONSECUTIVOS confirmados desde hoy hacia atrás, así que
si el hábito de hoy todavía no se marcó, también se corta y muestra 0. Ninguna tiene relación con
Finanzas — el "💰" de la barra superior es "gastos de HOY" (solo egresos), no un contador
relacionado con la racha; están uno al lado del otro mostrando cosas distintas. Sobre "21 días":
no hay ningún tope ni mención a 21 días en el código — la racha de Hábito no tiene límite
superior (si acaso, `calcularRacha` solo mira los últimos 60 días de historial al calcular, así
que una racha real de más de 60 días se vería truncada en 60, no en 21).

**Observación para el usuario, no aplicada todavía**: mostrar 0 hasta que se actúa HOY (cerrar el
día / marcar el hábito) contradice un poco la filosofía ya documentada en este proyecto ("en
Lula ningún intento se castiga") — una racha real de 10 días se ve como "0" toda la mañana hasta
que se cierra el día, aunque no se haya roto nada. Posible mejora: si hoy todavía no se cerró,
mostrar la racha "hasta ayer" con algún indicio de "continúa hoy" en vez de un 0 directo. No se
aplicó porque cambia el significado de una métrica visible constantemente — se deja para decidir
con el usuario.

**3. Medicamento "eliminado" que aparece sin haberlo borrado — investigado con datos reales, NO
es un bug.** Se revisó `historial_cambios` y `actividad` directamente en el dispositivo: el
medicamento nuevo que el usuario creó ("Ampicilina", mayúscula, 09:29am de hoy,
`id=7be40846…`) es una actividad distinta de una "ampicilina" (minúscula, `id=4d311459…`)
eliminada a las 00:01am del mismo día — un resto de una prueba anterior, no algo que el usuario
haya borrado ahora. Por eso el rastro "🗑️ eliminado" del feature nuevo (ver ronda anterior) SÍ
aparece hoy: es real, solo que de otra actividad que el usuario ya no recordaba. Se revisó
también `horariosParaFecha` para el medicamento nuevo (3 tomas por intervalo de 8h desde las
14:00: `["14:00","22:00","06:00"]`) y para HOY correctamente filtra el "06:00" (que en realidad
es de mañana) — no se encontró ninguna fila de toma con horario "06:00" para ninguno de los dos
medicamentos en `toma_medicamento`. No se pudo reproducir con datos el "aparece 6:00 am pero
tachado" tal como se describe — puede haberse visto en una build anterior a este último install,
o en otra vista del Calendario. Si sigue apareciendo después de este build, hace falta una
captura de pantalla para ubicar exactamente qué fila es.

**4. Tarea sin fecha límite, completada hoy, no aparecía en Calendario — bug real, arreglado.**
En `ObtenerAgendaDelRangoUseCase`, la Tarea salía del `forEach` con
`detalle.fechaLimite ?: return@forEach` ANTES de llegar a mirar `fechaCompletado` — una Tarea
creada sin fecha (solo nombre) que se completaba hoy nunca tenía ningún ancla de fecha para
mostrarse en Calendario, ni siquiera en el día real en que se completó. Se movió el fallback de
`fechaCompletado` antes del `return@forEach`, así que ahora si no hay `fechaLimite` pero SÍ hay
`fechaCompletado` (se marcó hecha), se muestra en su día de finalización.

**5. Citas de curso (sesiones) — rediseño visual completo, mismo patrón que el resto de la
app.** El usuario reportó: el texto de una sesión ya cumplida se veía morado (era
`colorScheme.primary`, el violeta de marca, igual que el resto de la UI antes de los cambios de
esta semana — nunca se actualizó a verde), la sesión de HOY sin marcar no se distinguía de una
futura ni de una vencida, y el botón "📅 Reprogramar" competía a la izquierda con "No se
cumplió"/"Deshacer" como si fuera parte del mismo grupo de acciones. `CitaDetailScreen.kt`
(`FilaSesionCita`) se actualizó: el `Checkbox` de una sesión ahora usa el mismo verde
(`LulaHabito`) que el resto de la app; una sesión cumplida se tacha y se apaga (`onSurfaceVariant`)
en vez de pintarse morada; la sesión de HOY sin marcar y sin vencer se resalta con fondo
(`colorScheme.surfaceVariant`) para diferenciarla de un vistazo de las futuras (blanco) y las
vencidas (rojo); "Reprogramar" se movió al lado derecho de su fila (`Arrangement.SpaceBetween`),
separado de las acciones de la izquierda; se redujo el `contentPadding` de los `TextButton` (el
valor por defecto de Material es bastante generoso) y el padding vertical de cada fila, para
achicar el hueco en blanco que el usuario notó entre sesiones.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Finanzas: la sección "Hoy" solo mostraba gastos, un ingreso no se veía en ningún lado (2026-08-14)

Ligado al punto 1 de arriba: el usuario registró un ingreso hoy y "no aparece nada". Se
verificó con datos reales que el movimiento SÍ se guardó bien (dos ingresos de S/20, fecha de
hoy, confirmados en la tabla `finanzas`) — el problema era de visibilidad, no de guardado.
`FinancesScreen.kt` tenía una sección "Gastos de hoy" que, tal como decía su nombre, solo
filtraba `EGRESO` — un ingreso registrado hoy nunca iba a aparecer ahí por diseño, y no había
ningún otro lugar en esa pantalla que mostrara "esto es lo que hiciste hoy" para un ingreso (solo
se reflejaba, sin resaltar, en el total "Este mes" de arriba). Se renombró a "Hoy" y ahora
lista TODOS los movimientos de hoy (ingresos y egresos, con su signo +/-), con un total
"Neto de hoy" en vez de "Total" (puede ser negativo). `FinancesUiState.gastosHoy`/`totalGastosHoy`
pasaron a ser `movimientosHoy`/`netoHoy`.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Racha en 0 al empezar el día + signo en gastos de la barra superior (2026-08-15)

El usuario confirmó con un ejemplo concreto la observación de la ronda anterior: "ayer tenía 3"
de racha y hoy, antes de cerrar el día, se veía "0" — se sentía como haber perdido la racha sin
haberla perdido. Se aplicó la mejora que se había dejado pendiente para decidir con él:
`ObtenerProgresoDeHoyUseCase.calcularRachaActual` y `ObtenerHistorialHabitoUseCase.calcularRacha`
ahora, si hoy todavía no se cerró el día / no se marcó el hábito, arrancan a contar desde AYER en
vez de desde hoy — así se sigue viendo la racha real en curso toda la mañana, y cerrar hoy la
extiende en +1 en vez de hacerla "aparecer de la nada". Mismo criterio en ambas rachas (global y
por hábito) para que sean consistentes entre sí.

También, a pedido del usuario ("el 💰 que es gasto le faltaría el signo, no se entiende que es
gasto"), el pill "💰" de `LulaTopBar.kt` ahora antepone "-" al monto cuando hay algo gastado hoy
(nunca "-0" cuando no hay nada).

Sobre "premios por persistencia" (el usuario mostró capturas de Duolingo: racha con cofres de
recompensa por niveles, desafíos diarios/mensuales) — quedó como pregunta abierta para el
usuario, no implementado: se recomendó NO copiar el mecanismo de cofres/urgencia ("¡última
oportunidad!"), que choca con la filosofía ya establecida en este proyecto ("ningún intento se
castiga", "no hace falta exagerar el premio" — ver comentario de los hitos de Meta), y en cambio
extender el mismo patrón ya usado en Metas (tarjeta chica de reconocimiento, una sola vez, sin
sonido) a hitos de racha (7/21/30/60/100 días).

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Hitos de racha: pantalla grande de celebración + mensajes variados al cerrar el día (2026-08-15)

Se construyó la Fase 1 de la estrategia guardada (ver memoria de sesión
`project_gamificacion_premios_persistencia`), acordada en varias rondas con el usuario (que trajo
una conversación con ChatGPT como referencia, evaluada y adaptada, no copiada literal — se dejó
afuera la mecánica de "gemas" a propósito, para más adelante).

**`core/utils/MensajesRacha.kt`** (nuevo) centraliza todos los textos:
- `esHitoRacha(racha)`: 7, 21, 30, y cada 30 días después de ese (60, 90, 120...).
- `mensajeHitoRacha(racha)`: un mensaje al azar de un pool de 4-5 por hito (7/21/30 tienen su
  propio pool con lenguaje que evoluciona — de "lo lograste" a "esto ya es parte de ti"; 60+
  usa un pool genérico parametrizado por los días).
- `emojiHitoRacha(racha)`: una plantita que crece (🌱 hasta 21, 🌿 hasta 30, 🌳 de ahí en más) —
  reusa el mismo símbolo que ya se eligió para Hábitos/crecimiento (`TipoActividadEmoji.kt`) en
  vez de inventar un ícono nuevo, y deja el camino listo para más adelante reemplazarla por un
  personaje propio (a pedido del usuario: "con el tiempo armemos un muñeco").
- `mensajeCierreDiario()`: pool de 12 frases cortas para el cierre normal (sin hito), al azar.
- `mensajeAnticipacionHito(racha)`: si falta exactamente 1 día para el próximo hito, devuelve un
  aviso tipo "Mañana completas 7 días. Ya casi." en vez del mensaje diario normal — información
  real (no urgencia falsa), pensada para dar una razón concreta de volver mañana.

**Persistencia**: `AjustesRepository.obtenerUltimoHitoRachaCelebrado()`/`setUltimoHitoRachaCelebrado()`
(nuevo, DataStore) guarda la racha más alta ya celebrada con la pantalla grande, para no repetir
la misma celebración si se vuelve a guardar el cierre del mismo día. Se resetea a 0 cuando la
racha vuelve a 0 o 1 (se rompió), así una racha nueva puede volver a celebrar 7/21/30 desde cero.

**`CerrarDiaViewModel.cerrarDia()`**: después de calcular la racha, decide qué mostrar —
celebración de hito (si `esHitoRacha` y todavía no se había celebrado esa racha) > aviso de "casi
llegas" (si falta 1 día) > mensaje diario al azar. Todo se calcula una sola vez al cerrar, no en
cada recomposición, para que el mensaje no cambie solo si la pantalla se recompone.

**`CerrarDiaScreen.kt`**: si se acaba de cruzar un hito, en vez de la vista chica normal de "día
cerrado" se muestra `CelebracionHitoRacha` — pantalla completa a propósito (no una tarjeta), con
la plantita grande (96sp), "N días", el mensaje al azar, y un botón fijo "Voy a seguir 🌱" (a
propósito NO rota, para que sea un llamado a la acción siempre reconocible — solo el mensaje de
arriba varía) que vuelve a Hoy. Sin hito, la vista normal ahora también usa el mensaje
diario/de-anticipación variable en vez del texto fijo "Buen trabajo. Mañana seguimos." de antes.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Compartir una Lista como texto plano (2026-08-15)

El usuario preguntó cómo compartir una Lista con otra persona (familia/amigos). Se analizaron 4
variantes que planteó y se separaron en dos grupos, según si necesitan backend o no:

**Necesitan cuentas reales/sync (quedan pendientes, ver `10-pendientes.md`)**:
1. **Seguimiento conjunto en vivo** ("hagamos esto juntos, cada uno avanza y ve al otro") — el
   mismo patrón que ya existe en Retos familiares, pero para una Lista y con un amigo puntual,
   no solo dentro de un espacio Familia.
2. **Copia vía QR/enlace que se desvincula** (compartir una lista puntual y que quede
   totalmente independiente después) — técnicamente NO necesita backend (es una transferencia
   de una sola vez, no una relación en curso), pero sí necesita construir el mecanismo de
   generar/leer el QR o enlace y el importador del otro lado. Quedó fuera de esta ronda, es la
   siguiente candidata obvia si se retoma esto.

**No necesitaba backend, se construyó ahora**: compartir como texto plano (WhatsApp, correo, lo
que sea) — ni siquiera requiere que la otra persona tenga Lula instalada. `ListDetailScreen.kt`
ganó dos botones nuevos, "📋 Copiar" y "📤 Compartir", mismo patrón ya usado en
`NoteEditorScreen.kt` (`Intent.ACTION_SEND` + `Intent.createChooser`, con
`ClipboardManager`/`Toast` para copiar). El texto se arma como "☑/☐ + nombre del ítem" por
línea, con el nombre de la lista arriba. Como es una copia de una sola vez, sin ningún vínculo
después, no hay nada más que sincronizar ni mantener.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Diagnóstico del mensaje de cierre de día "no aparece" — no era un bug (2026-08-16/17)

El usuario reportó que al cerrar el día no salía ningún mensaje, dos días seguidos. Se
diagnosticó con datos reales del dispositivo en vez de adivinar: se sacó la base de datos
completa (incluyendo `-wal`/`-shm`, que un primer pull sin esos archivos dejaba desactualizada —
lección para la próxima vez que se necesite ver el estado real de la BD en vivo), se revisó
`logcat` completo durante una prueba en vivo, y se leyó `ultimo_hito_racha_celebrado` directo
del archivo de preferencias (protobuf, sin `strings`/`sqlite3` en el dispositivo → se decodificó
a mano con Python). Conclusión: no hay ningún bug — la racha real es 7 (confirmado con los
datos), el hito 7 ya se había celebrado antes (durante una prueba rellenando días atrasados en
Calendario), y no se encontró ningún crash en el log. El mensaje corto (no la pantalla grande)
debería seguir apareciendo en cada cierre — quedó pendiente que el usuario mande una captura de
pantalla del momento exacto para confirmar visualmente, ya que todo lo verificable por datos
está correcto.

## Ronda de 6 puntos de uso real (2026-08-17)

**1. Duración de la Alarma configurable** — el usuario pidió poder elegir "silenciar después
de: 1/5/10/15/20/25 minutos o nunca" (mostró como referencia la pantalla de Alarmas del reloj
nativo de Android). Se confirmó en `AlarmaSonidoService.kt` que hoy el nivel Alarma suena en
loop **indefinidamente** (`isLooping = true`, sin ningún timer de corte) hasta que el usuario la
detiene a mano — no existe la opción de duración máxima. Quedó como propuesta a confirmar antes
de construir (toca `Ajustes` + el Service), no implementada esta ronda.

**2. Tarea/hábito para "ir al mercado" con alarma de madrugada** — no es un bug, es una pregunta
de uso. Para el caso regular (sábados y domingos, misma hora) conviene un **Hábito** con
`FrecuenciaHabito.DIAS_ESPECIFICOS` (sábado + domingo) y recordatorio Alarma a las 3:00 — ya
soportado, no hace falta nada nuevo. Para los viajes irregulares entre semana, sí hay que crear
una Tarea puntual cada vez (no hay патrón fijo que capturar), con fecha + recordatorio Alarma.

**3. Sección "¿Acompaña a un medicamento o cita?" muy cargada en Crear Tarea — arreglado.**
Vivía siempre desplegada (título + explicación + un `FilterChip` por cada medicamento/cita
existente), a diferencia de "Fecha límite"/"Recordatorio" que ya usaban el patrón compacto
`SelectorRow` + `ModalBottomSheet`. Se le aplicó el mismo patrón: ahora es una fila colapsada
más, las opciones solo aparecen al tocarla.

**4. Sin aviso al faltar el nombre al crear — arreglado en Tarea y Hábito.** Antes
`CrearTareaViewModel`/`CrearHabitoViewModel` hacían `if (nombre.isBlank()) return` en silencio —
tocar "Crear" sin nombre no hacía nada visible, sin explicar por qué. Se agregó `mensajeError`
(mismo patrón ya usado en `NoteEditorViewModel`) + `Toast` en ambas pantallas. Probablemente el
mismo hueco existe en el resto de "Crear X" (Medicamento, Cita, Meta, Rutina, Lista, Fecha
importante) — no barridos todavía, quedan pendientes si el usuario lo pide.

**6. Las 6 preguntas de Metas (Hacer/Ser/Ver/Tener/Ir/Compartir)** — el usuario trajo una
conversación con ChatGPT proponiendo una 7ª pregunta ("¿Cómo quiero vivir y sentirme?") y un
seguimiento de "¿por qué es importante?" después de cada respuesta. Se evaluó contra cómo esta
función vive HOY en el código: no es un flujo de entrevista guiada con respuestas guardadas (eso
es "Mi propósito", una función distinta) — es solo texto de referencia/ejemplos mostrado al
elegir la categoría de una Meta en `CrearMetaScreen` (`SelectorCategoriaSheet`). Se recomendó
**no** adoptar la propuesta de ChatGPT tal cual: agregar una 7ª categoría rompe el mapeo limpio
que ya existe (`CategoriaMeta`, usado para agrupar la lista de Metas) y el seguimiento "¿por
qué?" es una entrevista profunda que no calza con lo liviano que es hoy este selector. Sin
cambios de código esta ronda — decisión de producto, no bug.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) — pendiente instalar, el
dispositivo se desconectó justo antes.

## Duración máxima de la Alarma + ejemplos de las 6 preguntas de Metas actualizados (2026-08-17)

**Duración máxima de Alarma**: nueva opción en Ajustes ("⏱️ Silenciar alarma después de", en la
tarjeta "🔔 Recordatorios y permisos") con las mismas opciones que el reloj nativo de Android
que mostró el usuario: 1/5/10/15/20/25 minutos o "Nunca" (default, el comportamiento de siempre
— suena hasta apagarla a mano). Nueva preferencia `AjustesRepository.observarDuracionMaximaAlarmaMin()`/
`setDuracionMaximaAlarmaMin()` (DataStore, `Int?`). `AlarmaSonidoService` pasó a `@AndroidEntryPoint`
(inyecta `AjustesRepository` directo, evitando tocar `RecordatorioReceiver` — ese Service ya es
quien controla el loop del `MediaPlayer`, tiene sentido que también controle cuánto dura): al
iniciar, lanza una corrutina en su propio `CoroutineScope` que lee la duración configurada y, si
no es "Nunca", espera esos minutos y llama a `detener()` sola — cancelable si el usuario la
detiene antes a mano (botón, notificación, o abrir la app), y cancelada también en `onDestroy()`.

**Ejemplos de las 6 preguntas de Metas actualizados**: los ejemplos en `ejemplosAyuda()` de
`CrearMetaScreen.kt` eran muy específicos de un perfil emprendedor en particular ("Yo creo
empresas y las hago crecer", "Yo tengo casas para alquilar", "Yo voy al evento de Tomorrowland")
— se reemplazaron por ejemplos más universales (crear un negocio, aprender un idioma, tener
independencia financiera, conocer un país, compartir conocimientos...) que le calcen a cualquier
persona nueva en la app, manteniendo el mismo estilo "Yo..." en presente que ya usaba Lula (la
meta afirmada como si ya estuviera lograda). El usuario aclaró que no quería agregar una 7ª
pregunta ni el seguimiento "¿por qué?" de la ronda anterior — solo mejorar el contenido de los
ejemplos existentes.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Confirmado con captura de pantalla: el mensaje de cierre de día SÍ funciona (2026-08-17)

Cierre del ciclo de diagnóstico de los últimos 3 días: el usuario mandó captura de la pantalla
de "día cerrado" y ahí está el mensaje — "Otro día a tu favor." (uno de los 12 de
`MENSAJES_CIERRE_DIARIO`), justo arriba de "🔥 Racha: 8 días". Nunca hubo bug — el mensaje corto
se confundía visualmente con la píldora de racha de al lado y no se notaba como "un mensaje"
aparte. Sin cambios de código; el usuario decidirá si lo quiere más notorio (más grande/negrita/
ícono) en otra ronda.

## Punto 4 completo: aviso de campos obligatorios en TODOS los "Crear X" (2026-08-17)

Se confirmó que Medicamento, Cita y Fecha importante YA tenían `mensajeError` (de una ronda
anterior). Se agregó el mismo patrón (`mensajeError: StateFlow<String?>` + `Toast` en la
pantalla) a los que faltaban:

- **Meta**: nombre vacío o "cuánto quieres lograr" ≤ 0.
- **Rutina**: nombre vacío, o ningún hábito/tarea elegido para incluir.
- **Lista**: nombre vacío.
- **Reto familiar**: nombre vacío, o objetivo vacío.
- **Movimiento financiero** (Finanzas): categoría vacía, o monto ≤ 0.

Con esto, las 10 pantallas "Crear X" de la app avisan qué falta en vez de fallar en silencio al
tocar el botón de crear.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus).

## Firebase Auth + Firestore: integración Gradle inicial (2026-08-19)

Primer paso concreto de `12-firebase-auth-y-sync.md`: conectar el SDK de Firebase al proyecto,
sin todavía escribir código de autenticación real. El usuario creó el proyecto "Lula" en
Firebase Console, descargó `google-services.json` y lo colocó en `app/` (ubicación correcta,
confirmada). Se le indicó apagar "modo prueba" de Firestore y pegar una regla temporal de
denegar-todo (`allow read, write: if false;`) hasta escribir las reglas reales de la sección 4
del plan.

Cambios de Gradle:
- `gradle/libs.versions.toml`: versiones `googleServices=4.4.2`, `firebaseBom=33.6.0`,
  `credentials=1.3.0`, `googleid=1.1.1`; librerías `firebase-bom`, `firebase-auth`
  (`firebase-auth-ktx`), `firebase-firestore` (`firebase-firestore-ktx`),
  `androidx-credentials`, `androidx-credentials-play-services-auth`, `googleid`; plugin
  `google-services`.
- `build.gradle.kts` (raíz): `alias(libs.plugins.google.services) apply false`.
- `app/build.gradle.kts`: aplica el plugin `google-services` de verdad, agrega BoM de Firebase +
  `firebase-auth` + `firebase-firestore` + Credential Manager (`androidx.credentials` +
  `credentials-play-services-auth`) + `googleid`.

Se eligió **Credential Manager API** (`androidx.credentials` + `googleid`) para el login con
Google, no la API vieja `com.google.android.gms:play-services-auth` (deprecada por Google).

**Nombre "Lula" ya usado por otras apps**: se confirmó que no hay problema técnico en usar
"Lula" ahora en Firebase Console — el nombre del proyecto Firebase, el título en Play Store y el
`applicationId` (`com.aqpseller.lulaapp`) son tres cosas independientes; solo el
`applicationId` es efectivamente permanente tras publicar, y no contiene "lula" literal. Si la
marca pública cambia de nombre más adelante por conflicto de marca registrada, no exige tocar
código ni el proyecto de Firebase.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus) — arrancó sin crash, logcat confirma `FirebaseApp initialization successful`.
Falta el código real de autenticación (`AuthRepositoryFirebaseImpl`, pantallas de login/registro,
flujo de "reclamar cuenta" del usuario semilla, reglas de Firestore reales) — siguiente paso.

## Firebase Auth: login con Google real + "reclamar cuenta" (2026-08-19)

Paso 2 de `12-firebase-auth-y-sync.md` §8: login real con Google sobre el usuario semilla
existente (correo mágico queda para otra ronda, para no acoplar todo en un solo cambio grande).

- **`UsuarioEntity`/`Usuario` ganan `firebaseUid: String?`** (nullable, default `null`) —
  `MIGRATION_26_27` (`ALTER TABLE usuario ADD COLUMN firebaseUid TEXT`), `LulaDatabase.version = 27`.
  Verificado con `PRAGMA table_info(usuario)` en el dispositivo real después de instalar: la
  columna existe y la migración no truena (nada en logcat, la fila semilla sigue con su mismo
  `id`).
- **`AuthRepository` gana dos métodos** (interfaz sigue "estable" en el sentido de que
  `usuarioActualId()`/`observarUsuarioActualId()` no cambiaron de significado — siguen
  devolviendo el id del usuario LOCAL, no el uid de Firebase, porque Lula sigue siendo
  local-first de un usuario por dispositivo):
  `sesionFirebaseActiva(): Boolean` y `suspend fun iniciarSesionConGoogle(idToken): ResultadoSesionGoogle`.
- **`AuthRepositoryLocalImpl` se borró y se reemplazó por `AuthRepositoryFirebaseImpl`** (no se
  dejó como alternativa/flag — ya no tiene sentido con Firebase siempre enlazado). `signOut()`
  ahora sí hace algo real (`firebaseAuth.signOut()`), pero deliberadamente NO toca la fila local
  del usuario semilla ni sus datos — cerrar sesión de Google solo detiene la sincronización, la
  app se sigue usando 100% local como siempre.
- **`UsuarioRepository.vincularConGoogle(usuarioId, correo, firebaseUid)`** — el "reclamar
  cuenta": actualiza la misma fila (`copy` + `upsert`, mismo patrón que
  `actualizarConsentimientos`), nunca crea un usuario nuevo. Nuevo caso de uso
  `ReclamarCuentaConGoogleUseCase` conecta `iniciarSesionConGoogle` + `vincularConGoogle`.
- **Google Sign-In con Credential Manager** (no la API vieja deprecada): nuevo
  `core/auth/GoogleSignInHelper.kt` (`obtenerGoogleIdToken(context, serverClientId)`), usa
  `GetGoogleIdOption` + `GoogleIdTokenCredential`. El `serverClientId` (Web Client ID) no se
  hardcodeó — se lee de `R.string.default_web_client_id`, que el plugin `google-services` genera
  solo a partir de `google-services.json` (confirmado leyendo
  `app/build/generated/res/processDebugGoogleServices/values/values.xml`).
- **UI en `ProfileScreen.kt`**: nueva tarjeta "🔑 Cuenta" arriba de "Mi crecimiento" — si
  `metodoLogin != GOOGLE` muestra botón "🔵 Continuar con Google" (dispara Credential Manager
  desde una corrutina de `rememberCoroutineScope`, captura `GetCredentialException` si la
  persona cancela); si ya está vinculada muestra "✅ Vinculada con Google" + botón "Cerrar sesión
  de Google". Mensajes de éxito/error vía `Toast`, mismo patrón `mensajeError`/`LaunchedEffect`
  ya usado en el resto de la app.
- Nuevo `di/FirebaseModule.kt` (provee `FirebaseAuth.getInstance()` como singleton de Hilt).
  Se agregó la dependencia `kotlinx-coroutines-play-services` (necesaria para `Task<T>.await()`
  sobre las llamadas de Firebase Auth) — no llegaba transitivamente declarada, había que
  agregarla a mano.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus) — arrancó sin crash, migración de Room confirmada con `PRAGMA table_info` sobre
la base real del dispositivo.

**Bug real al primer intento, diagnosticado con logcat en vivo, no adivinado**: el botón
"Continuar con Google" fallaba con "No se pudo iniciar sesión con Google". El log mostró la
causa exacta: `Auth: [GetTokenResponseHandler] Server returned error: This android application
is not registered to use OAuth2.0, please confirm the package name and SHA-1 certificate
fingerprint match...` — al proyecto de Firebase le faltaba la huella SHA-1 del certificado de
firma del APK (obligatoria para que Google valide qué app está pidiendo el login, aparte del
`google-services.json`). Se generó el SHA-1 del keystore de debug local
(`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey`) y se le indicó
al usuario agregarlo en Firebase Console → Configuración del proyecto → Tus apps → Huellas
digitales de certificado SHA. Con eso agregado (sin tocar código ni `google-services.json`, se
resuelve del lado de Google), el login funcionó al segundo intento — confirmado con logcat
(`FirebaseAuth: Notifying id token listeners about user (DKLRvhoEglMEbiw8XhQ4ig4T45J3)`) Y
leyendo la base de datos real del dispositivo (`usuario.correo`, `metodoLogin='GOOGLE'`,
`firebaseUid` quedaron seteados sobre la misma fila semilla de siempre, mismo `id`). **Pendiente
para cuando se publique con firma de release**: agregar TAMBIÉN el SHA-1 de esa firma en Firebase
Console — el de debug no sirve para el APK firmado de Play Store.

Sigue pendiente: correo mágico (passwordless), sync de `Conexion`/`SolicitudCompartir`/Espacios
Familia a Firestore, y las reglas de seguridad reales (hoy Firestore sigue con la regla temporal
de denegar-todo).

## Círculo de cuidado: aceptar/rechazar real + sync a Firestore + reglas de seguridad (2026-08-19)

Al ponerse a sincronizar `Conexion`/`SolicitudCompartir` (paso 4-6 del plan) apareció un hueco
más grande de lo esperado: **nada en la app aceptaba o rechazaba una solicitud, ni se creaba
nunca una `Conexion`** — solo existían "enviar" y "cancelar", diseñados en Fase 1.0 pero nunca
conectados porque no había cómo emparejar cuentas reales. Se cerró ese círculo local primero,
porque sincronizar solicitudes que nunca se pueden aceptar no serviría de nada.

**Local — aceptar/rechazar:**
- `SolicitudCompartirRepository.responder(solicitudId, estado, usuarioId)` (nuevo) — marca
  `ACEPTADA`/`RECHAZADA` + `fechaRespuesta`.
- `ActividadRepository.agregarPermisoCompartido(actividadId, concederA, permiso, usuarioId)`
  (nuevo) — agrega a `puedeVer[]` (y a `puedeRecordar[]` si el permiso lo incluye) de la
  `Actividad`; no-op silencioso si la actividad no vive en este dispositivo.
- `AceptarSolicitudCompartirUseCase` (nuevo) — encadena `responder(ACEPTADA)` +
  `ConexionRepository.crearSiNoExiste(...)` + `agregarPermisoCompartido(...)` + push a Firestore.
- `RechazarSolicitudCompartirUseCase` (nuevo) — `responder(RECHAZADA)` + push a Firestore.
- **Bug real corregido de paso**: `SolicitudCompartirDao.observarPendientesPara` filtraba por
  `usuarioId`, pero `para` guarda un contacto de texto libre (correo/teléfono), no un id — nunca
  iba a matchear nada. Ahora filtra por **correo** del usuario actual.
- `SolicitudCompartir`/`SolicitudCompartirEntity` ganan `deNombre` (nombre de quien envía,
  denormalizado — igual que `contexto` ya denormalizaba el nombre del elemento) para que el
  destinatario vea "Juan te compartió..." en vez de un UUID. Migración `MIGRATION_27_28`
  (`ALTER TABLE solicitud_compartir ADD COLUMN deNombre TEXT NOT NULL DEFAULT ''`),
  `LulaDatabase.version = 28`. `CompartirActividadUseCase` ahora resuelve el nombre desde
  `UsuarioRepository` en vez de requerirlo como parámetro (evita tocar los 6 sitios que ya la
  llaman: Hábito/Tarea/Rutina/Medicamento/Cita/Meta).
- `CareCircleScreen`/`ViewModel`/`UiState`: la sección "PERSONAS QUE ACOMPAÑO" (antes un texto
  fijo) ahora lista solicitudes recibidas con botones "Aceptar"/"Rechazar".

**Firestore — sync real:**
- Nuevo `domain/repository/CompartirSyncRepository.kt` + `data/repository/CompartirSyncRepositoryImpl.kt`
  (Firestore) — `subirPerfil`, `subirSolicitud`, `eliminarSolicitud`, `subirConexion`,
  `escucharSolicitudes` (listener en vivo por `Filter.or(para == miCorreo, de == miUsuarioId)`).
  Cada push es *best-effort*: envuelto en `runCatching {}` en el caso de uso que lo dispara,
  nunca bloquea la acción local si Firestore falla o la cuenta no está vinculada — el
  local-first sigue siendo la garantía real, la nube es un extra.
- `SincronizarSolicitudesRecibidasUseCase` (nuevo) — corre mientras `CareCircleScreen` esté
  abierta (`viewModelScope`, se cancela sola al cerrarla), upsertea en Room cada cambio remoto.
  Room sigue siendo la única fuente de verdad para la UI; Firestore es solo transporte.
- `ReclamarCuentaConGoogleUseCase` ahora también sube el perfil mínimo a
  `usuarios/{firebaseUid}` justo después de vincular la cuenta.
- `di/FirebaseModule.kt` gana `FirebaseFirestore.getInstance()`.

**Problema de diseño real, encontrado y resuelto antes de escribir las reglas**: `de`/`para`/
`usuarioA`/`usuarioB` son ids de la app (UUID local de Room) o contactos en texto libre — NO son
el `uid` de Firebase Auth, son dos sistemas de identificación distintos. Las reglas de seguridad
de Firestore solo pueden verificar contra `request.auth.uid`, así que una regla como
`request.auth.uid == resource.data.de` nunca iba a funcionar. Solución: cada `SolicitudCompartir`
subida a Firestore también guarda `deFirebaseUid` (leído de `firebaseAuth.currentUser?.uid` al
momento de escribir, nunca inventado ni pasado desde afuera), y el lado del destinatario se
verifica contra `request.auth.token.email` (el correo YA verificado por Firebase, no un dato que
cualquiera podría falsificar) comparado contra `para`.

**Reglas de seguridad — ahora versionadas en el repo**: nuevo archivo `firestore.rules` en la
raíz (antes las reglas solo existían pegadas a mano en Firebase Console, sin ningún historial).
Reemplaza la regla temporal de "denegar todo": `usuarios/{uid}` (lectura para cualquier
autenticado, escritura solo del dueño), `solicitudes_compartir/{id}` (lectura/escritura solo
para `deFirebaseUid` o quien tenga el correo de `para`), `conexiones/{id}` (regla provisional
floja — solo exige estar autenticado, porque nada la lee todavía en ninguna pantalla),
`espacios/**` (denegado por completo — paso 5 del plan, sin código todavía). **El usuario debe
pegar el contenido de `firestore.rules` en Firebase Console → Firestore Database → Reglas** (no
hay Firebase CLI instalado en este entorno para desplegarlo automáticamente).

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
(moto g(9) plus) — migración a v28 confirmada con `PRAGMA table_info` (columna `deNombre`
presente), pantalla Círculo de cuidado abierta sin crash con el nuevo listener de Firestore
corriendo (logcat limpio, sin `FATAL EXCEPTION`/`AndroidRuntime`).

**Deliberadamente fuera de esta ronda** (documentado, no a medias): cuando alguien acepta una
solicitud que le compartieron, todavía **no ve el contenido real** del hábito/tarea/medicamento
en su propio dispositivo — hoy solo se sincroniza la solicitud y la conexión (la "capa social"),
no la actividad en sí. Mostrar el detalle real cruzando cuentas es un paso más grande (mapear
cada tipo de actividad a un documento de Firestore + una pantalla nueva de "lo que otros
comparten conmigo") que queda como siguiente pieza en `Plan/10-pendientes.md`. Tampoco se
construyó todavía el sync de Espacios Familia (paso 5 del plan) ni se probó de punta a punta con
dos cuentas reales — falta una segunda persona/dispositivo para eso.

## Invitar de verdad a un Espacio Familia (2026-08-20)

Antes de sincronizar el *contenido* de un Espacio Familia (paso 5 del plan), se confirmó un
bloqueo real: `FamiliaScreen` decía explícitamente "Invitar a alguien de verdad todavía no se
puede" — sincronizar el contenido de un espacio que nunca puede tener un segundo miembro real
no serviría de nada. Se construyó la invitación real primero, **reutilizando toda la
infraestructura de `SolicitudCompartir` de Círculo de cuidado en vez de duplicarla**:

- `TipoSolicitud` (nuevo enum: `ACTIVIDAD`/`ESPACIO`) — `SolicitudCompartir.elementoId` ahora
  puede ser un `actividadId` (como antes) o un `espacioId`; `permisos` solo tiene efecto para
  `ACTIVIDAD`. Migración `MIGRATION_28_29`, `LulaDatabase.version = 29`.
- `EspacioRepository.agregarMiembro(espacioId, usuarioId, rol)` (nuevo).
- `InvitarAEspacioUseCase` (nuevo, en `domain/usecase/espacio/`) — mismo patrón que
  `CompartirActividadUseCase`: crea una `SolicitudCompartir(tipo = ESPACIO)` y la sube a
  Firestore.
- `AceptarSolicitudCompartirUseCase` ahora bifurca por `solicitud.tipo`: `ACTIVIDAD` sigue dando
  acceso en la actividad (como antes); `ESPACIO` agrega al usuario como `EspacioMiembro` real.
  En ambos casos se crea la `Conexion`.
- `CareCircleScreen` distingue visualmente una invitación a Familia ("🏠 Invitación a la Familia
  ...") de una solicitud de actividad normal — se reutiliza la misma bandeja de "recibidas", no
  se construyó una pantalla aparte.
- `FamiliaScreen` gana el formulario real de invitar (pedir correo, botón "Enviar invitación"),
  visible solo si la cuenta ya está vinculada con Google.

**Bug real encontrado y arreglado en vivo, con logcat**: al aceptar la primera invitación a
Familia, la app se cerraba. El log mostró la causa exacta:
`kotlinx.serialization.SerializationException: Serializer for class 'EspacioMiembroEntity' is
not found` — esa entidad nunca se había marcado `@Serializable` porque, hasta ahora, nada la
auditaba (`AuditLogger` usa `kotlinx.serialization` para serializar antes/después). Se agregó
`@Serializable` a `EspacioMiembroEntity`. Como el crash pasó a mitad del flujo (la solicitud ya
había quedado `ACEPTADA` localmente antes de tronar, pero sin crear el `EspacioMiembro`), se
corrigió el dato inconsistente a mano en el dispositivo (parar la app, sacar `lula.db`, revertir
esa fila a `PENDIENTE` con `sqlite3` local, empujarla de vuelta, borrar `-wal`/`-shm` viejos)
para poder reprobar limpio. **Nota para el futuro**: estos pasos (`responder` → `crearSiNoExiste`
→ `agregarPermisoCompartido`/`agregarMiembro`) no corren en una sola transacción — si algo falla
a mitad de camino puede dejar estado a medias, como pasó acá. No se envolvió en una transacción
esta ronda (cruza tres repositorios distintos); queda como mejora pendiente si se repite.

Segundo intento: compiló, se instaló, y **aceptar una invitación a Familia funcionó de punta a
punta** — verificado con la base de datos real del dispositivo: la solicitud quedó `ACEPTADA` y
apareció una fila nueva en `espacio_miembro` con `rol = MIEMBRO`, sin tocar la fila `ADMIN`
original.

## Compartir por código QR: Listas, "mi código para conectar", y botón global de escanear (2026-08-20)

El usuario comparó con Yape (QR sin abrir la app, escanear = ya quedó hecho) y pidió avanzar esa
dirección. Se aclaró primero una diferencia importante de diseño: escanear en persona SÍ puede
saltarse el paso de "aceptar" (es la persona físicamente mostrando su código, no una solicitud
remota sin revisar) — pero eso solo aplica a lo que es 100% local (Listas); para conectar
personas/Familia se mantuvo el paso de aceptar que ya existe y está probado, porque abrir esa
puerta requeriría reglas de Firestore nuevas (un "código canjeable" que cualquiera puede
reclamar) y, aun resuelto eso, quien invita seguiría sin enterarse de que alguien se unió hasta
que exista sync real del contenido del Espacio (pendiente, ver más abajo) — no vale la pena
construir una función a medias.

**Compartir una Lista por QR** — transferencia de una sola vez, 100% local, sin backend (ver
`Plan/10-pendientes.md`):
- `core/utils/ListaQrCodec.kt` (nuevo) — codifica `{nombre, items}` como JSON con prefijo
  `LULA_LISTA_V1:`.
- `ListDetailScreen.kt` — botón "Por código" muestra el QR (reutiliza `QrCodeGenerator` ya
  existente, sin cambios).
- `ImportarListaDesdeQrUseCase` (nuevo) — decodifica y llama a `ListaRepository.crear` (mismo
  camino que crear una lista a mano).

**Escanear real** — no existía nada de esto: sin permiso de cámara, sin librería de lectura.
Se eligió el **Code Scanner de Google Play Services**
(`com.google.android.gms:play-services-code-scanner`) en vez de CameraX+zxing manual o ML Kit
embebido — da una UI de escaneo lista, y **no requiere declarar permiso de cámara en el
manifiesto** (lo maneja el módulo de Play Services aparte). `core/utils/QrScanner.kt`
(`escanearQr(context): String?`, suspend, wrapper sobre el `Task<Barcode>`).

**"Mi código para conectar"** (`ProfileScreen.kt`, solo visible con cuenta vinculada) — un QR
con el propio correo (`core/utils/ContactoQrCodec.kt`, prefijo `LULA_CONTACTO_V1:`), para que
otra persona lo escanee y no tenga que escribirlo a mano al compartir/invitar.

**Botón único de escanear, en la barra superior** (`LulaTopBar.kt`, visible en toda la app) —
a pedido del usuario, en vez de un botón distinto enterrado en cada pantalla (Listas, Compartir,
Invitar a Familia — todos esos botones sueltos se sacaron después de agregar este). Detecta
solo qué tipo de código de Lula es: si es una Lista, la importa directo; si es un contacto, copia
el correo al portapapeles con un aviso de dónde pegarlo. `TopBarStatsViewModel.escanear(...)`
hace el enrutamiento.

**Bug real encontrado de paso**: el aviso "📩" de solicitudes pendientes en la barra superior
llamaba a `obtenerSolicitudesRecibidasUseCase` con el `usuarioId`, no con el correo — el mismo
bug que ya se había corregido en el DAO la ronda pasada, pero que no se había propagado hasta
`TopBarStatsViewModel`. Corregido (ahora resuelve el correo vía `UsuarioRepository` antes de
llamar).

**Íconos reales en vez de emoji** — el usuario mostró una captura de otra app (Alipay/similar)
con íconos claros de "compartir por QR"/"escanear QR" y pidió lo mismo; un emoji (📷, 🔳) no se
entendía o parecía "sacar foto". Se agregó `androidx.compose.material:material-icons-extended`
**solo para estos dos íconos** (`Icons.Filled.QrCode`, `Icons.Filled.QrCodeScanner`) — el resto
de la app sigue siendo 100% emoji a propósito (decisión de siempre, ver
`Plan/02-pantallas.md`/`Plan/CLAUDE.md`). R8 recorta los íconos no usados en el build de
release, así que el costo real en tamaño de APK es mínimo.

Compilado y verificado (`compileDebugKotlin`, `EXIT_CODE=0`) e instalado en el dispositivo real
en cada paso — sin crash. **No probado con un segundo dispositivo real todavía** (queda para
cuando el usuario tenga el `.apk` de debug instalado en un segundo teléfono — ver
`Plan/10-pendientes.md`).

## Sync de contenido de Espacio Familia — Tareas y Retos familiares (paso 5, 2026-08-21)

Último paso grande del plan de Firebase: hasta ahora solo se sincronizaba la "capa social"
(solicitudes, conexiones, membresía). Esta ronda sincroniza el contenido real de un Espacio
Familia — **solo Tareas y Retos familiares**, que es lo que `FamiliaScreen` ya ofrece hoy
("tareas del hogar y los retos familiares"). Hábitos/Medicamentos/Citas/Fechas importantes
quedan fuera a propósito (son de uso personal, Círculo de Cuidado es la función pensada para
esos) — evita construir 6 veces el mismo mapeo para tipos que Familia no usa en la práctica.

**Nuevo `EspacioSyncRepository`** (`domain`/`data`) — espejo en Firestore:
```
espacios/{espacioId}                         — nombre, tipo, creadoPor, fechaCreacion
espacios/{espacioId}/miembros/{firebaseUid}  — usuarioIdLocal, rol
espacios/{espacioId}/actividades/{actividadId} — solo tipo TAREA, campos base + detalle aplanado
espacios/{espacioId}/retos/{retoId}          — RetoFamiliar
espacios/{espacioId}/registrosReto/{retoId}_{usuarioId}_{fecha} — "cumplido hoy" de cada quien
```
`miembros` se guarda por **uid de Firebase**, no por el id local de la app — mismo motivo que
`deFirebaseUid` en `solicitudes_compartir`: las reglas de seguridad solo pueden verificar contra
`request.auth.uid`. `registrosReto` es una colección plana bajo el espacio (no anidada dentro de
cada reto) a propósito — anidarla hubiera necesitado un `collectionGroup` query con índice
compuesto para poder escuchar todos los registros a la vez, que no se puede crear solo con este
archivo de reglas (necesita la consola o Firebase CLI, que no está instalado acá).

**Local → Firestore** (push, siempre `runCatching`, nunca bloquea la acción local): sale de
`CrearTareaUseCase`/`ActualizarTareaUseCase`/`MarcarActividadUseCase` (solo si el Espacio de la
Tarea es tipo `FAMILIA` — revisa con `EspacioRepository.obtenerEspacioSiEsMiembro`) y de
`CrearRetoFamiliarUseCase`/`MarcarRetoFamiliarCumplidoUseCase` (un Reto familiar por definición
siempre vive en un Espacio Familia, no hace falta revisar el tipo). `CrearEspacioFamiliaUseCase`
ahora también sube el Espacio y la membresía del admin al crearlo (antes no subía nada).
`AceptarSolicitudCompartirUseCase` (rama `ESPACIO`) también sube mi propia membresía real al
aceptar una invitación, además del mirror local que ya hacía.

**Firestore → Local** (`SincronizarEspacioFamiliaUseCase`, nuevo): escucha mientras el Espacio
Familia sea el activo — vive en `TopBarStatsViewModel` (ya es efectivamente el único ViewModel
de toda la sesión, hospedado en el `Scaffold` que envuelve el `NavHost`, no por pantalla) con un
`Job` que se cancela y reinicia solo cuando cambia el espacio activo. `ActividadRepository`
gana `mergeTareaRemota` y `RetoFamiliarRepository` gana `mergeRemoto`/`mergeRegistroRemoto` —
upserts puros en Room, nunca vuelven a subir a Firestore (evita un loop de sync).

**Reglas de seguridad reales para `espacios/**`** (antes completamente bloqueado) — cada quien
solo puede escribir su propia membresía (`miembros/{miFirebaseUid}`); leer/escribir cualquier
otra cosa del espacio exige ser miembro (`exists(.../miembros/$(request.auth.uid))`, una función
`esMiembro()` reusada en cada subcolección).

**Bug real encontrado con logcat en vivo, no adivinado**: al probar, la primera Tarea creada
falló con `PERMISSION_DENIED` en todas las lecturas Y en la escritura — el Espacio Familia usado
para la prueba se había creado en una sesión anterior, **antes de que existiera este sync**, así
que nunca tuvo membresía subida a Firestore y `esMiembro()` daba falso, correctamente, para las
reglas nuevas. Se agregó un "respaldo" (`SincronizarEspacioFamiliaUseCase.respaldarMiPresenciaRemota`):
antes de empezar a escuchar, si soy miembro local de ese espacio, sube el Espacio + mi propia
membresía (idempotente, no pisa nada si ya existía) — así un Espacio Familia viejo (o uno cuyo
push original falló por estar sin conexión) también empieza a funcionar sin tener que recrearlo.

Confirmado con evidencia real, no solo con la ausencia de errores: se verificó en Firebase
Console que el documento `espacios/{id}` tiene sus campos (nombre, tipo, creadoPor), que existen
las subcolecciones `miembros` y `actividades`, y que la Tarea creada después del arreglo aparece
completa con todos sus campos (`tipo: "TAREA"`, `nombre`, `propietario`, `puedeVer`,
`puedeRecordar`, `responsables`, el detalle aplanado). Compilado, instalado en el dispositivo
real, sin crash en ningún intento.

**Sigue pendiente**: el segundo miembro de un Espacio Familia todavía no ve el contenido que ya
existía ahí ANTES de unirse (el listener solo trae cambios desde que empieza a escuchar hacia
adelante, no hace un "catch-up" histórico completo — aunque en la práctica `addSnapshotListener`
de Firestore sí entrega el estado actual completo al conectarse por primera vez, así que esto
debería funcionar solo; falta confirmarlo con una segunda cuenta real). Probar de punta a punta
con un segundo dispositivo real sigue pendiente — ver `Plan/10-pendientes.md`.

## Respaldo del Espacio Personal — Hábitos y Tareas, sin restricción todavía (2026-08-21)

El usuario preguntó qué pasaría si cambia de celular hoy — la respuesta honesta fue "casi todo
se pierde", porque la decisión de privacidad del 1 de agosto dice que lo Personal nunca
sincroniza. Se decidió construir el respaldo real ahora, **sin cortarlo detrás de un muro de
pago todavía** (el sistema de cobros no existe aún) — la idea es que quien prueba la app ahora
no pierda su avance, y cuando exista premium, se corte con las mismas reglas de seguridad que ya
sabemos escribir (nada que rediseñar).

**Decisión de arquitectura clave**: a diferencia de `EspacioSyncRepository` (Familia, varias
personas editando lo mismo en simultáneo → necesita escuchar en vivo), lo Personal es de **un
solo dispositivo activo a la vez** — no hace falta un listener permanente. Nuevo
`PersonalSyncRepository`: sube cada cambio (`subirHabito`/`subirTarea`/`subirRegistroHabito`,
siempre `runCatching`) y **restaura una sola vez** (`restaurarHabitos`/`restaurarTareas`/
`restaurarRegistrosHabito`, un `.get()` puntual, no `addSnapshotListener`) — se llama al vincular
la cuenta con Google (`ReclamarCuentaConGoogleUseCase`) y, por si se agregó algo desde otro
dispositivo mientras tanto, también best-effort en cada apertura de la app (`AppViewModel`).
Idempotente por diseño: cada fila se aplica por upsert sobre su id original, así que llamarlo
varias veces nunca duplica nada.

**Alcance de esta ronda**: Hábitos (con su historial día por día — la racha, que es justo el
"avance" que más le dolería perder a alguien) y Tareas del Espacio Personal. Medicamentos,
Citas, Fechas importantes, Finanzas, Diario, Notas, Mi propósito, Metas y Listas quedan fuera a
propósito — se agregarán en rondas siguientes si hace falta, mismo patrón ya probado.

**Firestore**: `usuarios/{firebaseUid}/actividadesPersonales/{actividadId}` (Hábito y Tarea
comparten la colección, discriminados por campo `tipo`, igual que Familia) y
`usuarios/{firebaseUid}/registrosHabito/{actividadId}_{fecha}`. A diferencia del perfil
(`usuarios/{uid}`, legible por cualquier autenticado), estas subcolecciones son privadas — reglas
nuevas: solo el dueño (`request.auth.uid == firebaseUid`) puede leer o escribir.

**Bug real encontrado y arreglado con logcat en vivo**: el primer intento de push falló con
`PERMISSION_DENIED` porque las reglas nuevas todavía no estaban publicadas — se le pasaron al
usuario, las publicó, y el segundo intento funcionó. Confirmado con Firebase Console: el
documento del Hábito con todos sus campos (`tipo`, `nombre`, `detalleFrecuencia`,
`detalleNivelRecordatorio`, etc.) y la subcolección `registrosHabito` con el día marcado,
verificado ahí en vivo.

**Aclaración importante sobre "30 días" de Firebase**: el usuario preguntó si el proyecto de
Firebase se creó con algún vencimiento de 30 días que había que "migrar a permanente". No —
Firestore muestra por defecto un aviso de vencimiento de 30 días cuando se crea en "modo
prueba" (reglas abiertas), pero esa etapa ya se superó hace varias rondas cuando se pegaron las
primeras reglas reales de verdad (deny-all, después las granulares). El plan Spark (gratis) no
tiene fecha de vencimiento — no hay nada pendiente de migrar.

## Restaurar membresía a Espacios Familia en un celular nuevo (2026-08-21)

El usuario preguntó explícitamente "¿recupero lo de Familia si cambio de celular?" — la
respuesta honesta fue "el contenido sí está en la nube, pero tu membresía no se restaura sola
todavía", exactamente el hueco identificado unas rondas atrás. Se cerró ahora.

**Problema de diseño resuelto**: para saber "en qué Espacios Familia soy miembro" en un celular
nuevo, la opción obvia (`collectionGroup("miembros").where("firebaseUid", "==", miUid)`)
necesita un índice compuesto que Firestore no crea solo — hay que configurarlo a mano en la
consola o con Firebase CLI, que no está instalado en este entorno. Se evitó ese problema con un
diseño más simple: un puntero liviano en el propio perfil,
`usuarios/{miFirebaseUid}/misEspacios/{espacioId}`, que cada quien escribe sobre sí mismo al
crear o aceptar unirse a un Espacio Familia — descubrirlos es una consulta directa sin índices
especiales.

- `EspacioSyncRepository` gana `subirPunteroMiEspacio(espacioId)` y
  `descubrirMisEspacios(): List<Pair<Espacio, EspacioMiembro>>`.
- Se escribe el puntero en `CrearEspacioFamiliaUseCase`, en `AceptarSolicitudCompartirUseCase`
  (rama ESPACIO), y en el "respaldo" de `SincronizarEspacioFamiliaUseCase` (así un Espacio
  Familia viejo, creado antes de que este puntero existiera, también se auto-repara la próxima
  vez que se visita — mismo patrón que el respaldo de membresía de la ronda pasada).
- `RestaurarEspaciosFamiliaUseCase` (nuevo) — descubre mis espacios, asegura el mirror local
  mínimo + mi membresía, y trae el contenido (Tareas/Retos) de una sola vez (reutiliza los
  mismos `escuchar*` de `EspacioSyncRepository`, tomando solo la primera emisión con `.first()`
  en vez de quedarse escuchando — el listener en vivo lo retoma `TopBarStatsViewModel` recién
  cuando de verdad se entra a ese espacio). Se llama al vincular la cuenta con Google y en cada
  apertura de la app (igual que `RestaurarDatosPersonalesUseCase`).
- Reglas de seguridad nuevas para `usuarios/{uid}/misEspacios/{espacioId}`.

Confirmado con Firebase Console: el documento `usuarios/{uid}/misEspacios/{espacioId}` con el
`espacioId` de "Familia Vllca" — el puntero se creó correctamente al cambiar de espacio activo
(donde vive el "respaldo" que también lo escribe). Sin crash, sin errores de permisos en el
segundo intento (el primero falló porque las reglas nuevas no estaban publicadas todavía —
mismo patrón de esta sesión: error real, se corrige, se confirma).

## Respaldo del Espacio Personal — Finanzas, Diario, Notas, Metas, Listas y Mi propósito (2026-08-22)

Continuación directa de la ronda anterior ("recuperar la cuenta por completo"): el usuario
preguntó explícitamente si Finanzas/Notas/Diario se guardaban al cambiar de celular — la
respuesta fue "todavía no, solo Hábitos/Tareas" — y pidió cerrar ese hueco. Mismo patrón ya
probado de `PersonalSyncRepository` (subir en cada escritura con `runCatching`, restaurar una
sola vez al vincular cuenta / abrir la app), extendido a los 6 tipos que faltaban del Espacio
Personal. Quedan fuera a propósito, mismo patrón para cuando haga falta: Medicamentos, Citas,
Fechas importantes.

**Firestore**: un tipo, una colección — `usuarios/{uid}/movimientosFinancieros`,
`/entradasDiario`, `/notas`, `/metas`, `/listas` (cada Lista guarda sus ítems como array
embebido de mapas, no como subcolección — no hace falta un `.get()` extra para leerlos) y
`/proposito` (documento único `"unico"`, no una colección de muchos). Reglas: mismo patrón
privado que `actividadesPersonales` — solo `request.auth.uid == firebaseUid`.

**Restauración por tipo — reglas distintas según qué tan idempotente era ya cada repositorio**:
- Finanzas, Diario, Notas, Metas: sus métodos `crear`/`registrar` ya eran upsert-por-id-real
  (`@Upsert` de Room), así que `RestaurarDatosPersonalesUseCase` los reutiliza tal cual — no
  hizo falta ningún método nuevo en esos repositorios.
- Listas: `ListaRepository.crear()` siempre genera un id nuevo (pensado para "crear una lista
  nueva desde cero", no para restaurar una que ya existe) — no servía para restaurar sin
  duplicar. Se agregó `ListaRepository.mergeRemota(espacioId, lista, usuarioId)`: upsert por
  el id original tanto de la `ListaEntity` como de cada `ListaItemEntity`, preservando
  `fechaCreacion`/`orden` si la lista ya existía localmente.
- Mi propósito: `guardarRespuesta` ya mezcla una respuesta a la vez sobre el mapa existente, así
  que restaurar es simplemente iterar `respuestas.forEach { guardarRespuesta(...) }` — tampoco
  hizo falta un método nuevo.

**Push desde las pantallas — mismo problema repetido en Listas**: la mayoría de casos de uso
(`CrearNotaUseCase`, `RegistrarMovimientoUseCase`, etc.) ya construyen el objeto completo antes
de guardarlo, así que pushearlo es directo. Los casos de uso de ítems de Lista
(`MarcarItemListaUseCase`, `EliminarItemListaUseCase`) en cambio solo reciben el `itemId`, no la
`Lista` completa que hay que subir — se agregó `ListaRepository.obtenerListaIdDeItem(itemId)`
para encontrar a qué lista pertenece y volver a leerla completa (`observarConItems(listaId)
.first()`) antes de subirla. En `EliminarItemListaUseCase` el id de la lista se lee **antes** de
borrar el ítem (después ya no se puede reconstruir la relación).

**Decisión de privacidad explícita, confirmada con el usuario**: Diario vive documentado dentro
de "Zona Privada" (gateado con huella/biometría, nunca salía del celular). Se le preguntó
directamente si quería que Diario se respaldara igual que el resto — confirmó que sí. Diferencia
importante a tener presente: localmente sigue protegido por biometría, pero en la nube queda
protegido por la cuenta de Google, no por la huella del teléfono.

**Bug real encontrado y arreglado con logcat, mismo patrón que todas las rondas anteriores**: el
primer intento de guardar una respuesta de Mi propósito falló con `PERMISSION_DENIED` en
`usuarios/{uid}/proposito/unico` porque las reglas nuevas todavía no estaban publicadas. Se le
pasaron al usuario, las publicó, y el reintento (Meta + Lista + Propósito juntos) pasó limpio —
sin `PERMISSION_DENIED`, sin excepciones — confirmado con logcat y visualmente en Firebase
Console.

## Respaldo del Espacio Personal — Rutina, Medicamento, Cita, Fecha importante, Cerrar mi día y Revisión semanal (2026-08-22)

Cierre del respaldo Personal: el usuario preguntó explícitamente qué de todo lo que tiene la
app se recupera en otro celular — la auditoría de código encontró que Rutina, Medicamento,
Cita, Fecha importante, el historial de "Cerrar mi día" y la Revisión semanal **no** se
respaldaban todavía (Calendario no es un dato aparte, hereda el problema de los tipos de
Actividad que agrupa). Se completó con el mismo patrón ya probado de `PersonalSyncRepository`.

**Hallazgo colateral real, corregido de paso**: nunca existió un `eliminarActividad` en
`PersonalSyncRepository` — borrar un Hábito/Tarea local nunca borraba su copia en la nube. Esto
no solo dejaba "basura" en Firestore: si se hubiera dejado así, un futuro restore habría
revivido un `registro_actividad`/`toma_medicamento` huérfano (sin su Actividad dueña), violando
la FK local. Se agregó `eliminarActividad(actividadId)` — borra el documento y, si tenía,
también sus `tomasMedicamento`/`sesionesCita` asociadas, todo en una sola llamada usada por
`EliminarActividadUseCase` para cualquier tipo.

`PausarReanudarActividadUseCase` tampoco resubía nada — pausar/reanudar podía dejar desalineado
el campo `activa` en la nube hasta la próxima edición. Se agregó
`PersonalSyncRepository.subirActividadSegunTipo(actividad)`, que despacha al `subirX` correcto
según `actividad.detalle` — necesario ahí porque el tipo no se conoce en tiempo de compilación
(a diferencia de Crear/Actualizar, que sí lo conocen).

Todos los flujos de escritura nuevos siguen el mismo criterio ya establecido: solo empujan a
`PersonalSyncRepository` si `espacioRepository.obtenerEspacioSiEsMiembro(...)?.tipo ==
TipoEspacio.PERSONAL` (mismo patrón de `MarcarActividadUseCase`/`CrearHabitoUseCase`) —evita
contaminar el respaldo Personal con datos de un Espacio Familia.

**Firestore**: Rutina/Medicamento/Cita/Fecha importante reusan `actividadesPersonales` (misma
colección que Hábito/Tarea, discriminados por `tipo`, sin colección nueva). Sí son nuevas:
`tomasMedicamento`, `sesionesCita`, `registrosDiarios`, `registrosSemanales` — mismo patrón
privado (`request.auth.uid == firebaseUid`) que el resto de lo Personal.

Confirmado sin `PERMISSION_DENIED` ni excepciones tras publicar las reglas nuevas y probar
Rutina + Medicamento (con toma) + Cita (con sesiones de curso) + Fecha importante juntos.

## Registro obligatorio al iniciar + preguntas de onboarding (2026-08-22/23)

El usuario probó la app en un segundo celular por primera vez y notó que arrancaba directo en
Hoy con el usuario semilla, sin ningún registro — exactamente el hueco que `Plan/06-onboarding.md`
ya había diseñado el 2026-08-01 pero nunca se había construido. Se construyó el flujo completo
descrito ahí (Bienvenida → Cuenta → Privacidad → 5 preguntas → Resumen → Hoy), con una decisión
distinta a la original en un solo punto:

- **Paso "Hábitos sugeridos" del documento original, fuera de esta ronda a propósito** — arma
  hábitos según las respuestas 4a/4b, es una pieza de lógica de producto separada del gate de
  registro en sí (que era el pedido explícito). Queda documentado como pendiente.
- **Cuenta: Google obligatorio, correo mágico como "próximamente"** — el plan original (`Plan/12-
  firebase-auth-y-sync.md`) preveía enlace mágico por correo sin contraseña, pero completarlo de
  verdad (que el link vuelva a abrir la app) necesita un dominio propio configurado en Firebase —
  Firebase Dynamic Links (la forma vieja de resolver esto sin dominio) fue dado de baja por
  Google. El usuario confirmó: por ahora Google nomás, sin contraseña clásica, dejando el diseño
  listo para sumar correo/gestor de contraseñas más adelante.

**Gate de navegación**: `Usuario.onboardingCompletadoEn: Long?` (null = falta registrarse).
`AppViewModel` lo expone como `StateFlow<Boolean?>`; `MainActivity` compone `OnboardingScreen`
en vez de `LulaNavHost` mientras sea `false`. Los usuarios que YA tenían la app instalada con
datos reales se marcan completados de una vez en la propia migración (`MIGRATION_29_30`), para
no interrumpirlos con un registro retroactivo.

**Bug propio, encontrado y arreglado en la misma ronda**: la primera versión de
`OnboardingScreen` se veía con texto gris casi invisible — a diferencia de cualquier otra
pantalla de la app (siempre dentro del `Scaffold` de `LulaNavHost`, que ya pinta su propio fondo
y define el color de contenido correcto), esta pantalla se muestra ANTES de `LulaNavHost`, sin
ningún `Surface`/`Scaffold` que la envuelva — sin eso, Compose no tiene de dónde sacar el color
de texto correcto. Se arregló envolviendo la pantalla en un `Surface` propio.

## Código de invitación a Espacio Familia con tiempo de vida corto (2026-08-23)

El usuario pidió, hace semanas, que escanear un QR de invitación dejara a la persona "ya
adentro" sin un paso de aceptar aparte (estilo Yape) — se había descartado antes (2026-08-20)
por el riesgo de seguridad de un QR permanente que cualquiera con la imagen pudiera reclamar
después. Se retomó con una idea del propio usuario: que el código tenga tiempo de vida corto y
se renueve solo, para que guardar una foto del QR ya no sirva pasado ese tiempo.

**Por qué solo para Familia, no para Círculo de cuidado todavía**: el contenido de un Espacio
Familia ya sincroniza de verdad entre dispositivos (paso 5 ya construido) — aceptar de golpe ahí
es 100% funcional. El contenido de Círculo de cuidado (compartir una Tarea/Medicamento puntual)
**no** — aceptar ahí hoy no deja ver nada real del otro lado (hueco ya documentado aparte). Se
decidió con el usuario: Familia ahora, Círculo de cuidado cuando se resuelva ese hueco de
contenido (queda documentado, no descartado).

**Diseño de seguridad**: colección nueva `codigosInvitacionEspacio/{codigoId}` (Firestore),
independiente de `espacios/{id}/**` porque quien escanea todavía no es miembro y no podría leer
ahí. Cada código dura 60 segundos (`DURACION_CODIGO_MS`); `FamiliaViewModel.mostrarCodigoQr()`
genera uno nuevo y se regenera solo mientras el diálogo sigue abierto (loop con `delay` hasta
casi el vencimiento). Reclamar un código es una `runTransaction` de Firestore (no un `update`
suelto) para que dos personas escaneando casi al mismo tiempo no puedan reclamarlo ambas. La
regla de seguridad refuerza lo mismo del lado del servidor: `update` solo permite fijar
`reclamadoPor` a mi propio uid, solo si nadie lo reclamó antes y no venció, y solo ese campo
puede cambiar en esa escritura — un cliente malicioso no puede saltarse el vencimiento ni pisar
el reclamo de otro aunque ignore la app.

`UnirseAEspacioConCodigoUseCase` reutiliza exactamente la misma secuencia de
`AceptarSolicitudCompartirUseCase` (rama ESPACIO): `asegurarEspacioMinimo` + `agregarMiembro` +
`subirMiembro` + `subirPunteroMiEspacio` — no se duplicó lógica de unión, solo cambia cómo se
dispara. El QR en sí solo lleva el `codigoId` (texto corto, prefijo `LULA_CODIGO_ESPACIO_V1:`,
mismo patrón que `ListaQrCodec`/`ContactoQrCodec`); los datos reales viven en Firestore y se
validan ahí, nunca en el propio QR.

**Aclaración importante confirmada con evidencia real**: un lector de QR genérico (no el botón
de escanear de Lula) solo abre el texto crudo en otra app (ej. notas) — no lanza Lula. Es
esperado: el código es texto plano con un prefijo propio, no una URL, así que solo el escáner
interno de Lula (`ML Kit` + `decodificarCodigoEspacioQr`) sabe interpretarlo. Convertirlo en un
link real que cualquier lector entienda necesitaría Android App Links (dominio propio +
verificación `assetlinks.json`) — mismo tipo de bloqueo que el enlace mágico por correo.

## Bugs reales encontrados en la primera prueba de dos celulares con cuentas distintas (2026-08-23)

Primera vez en toda la sesión probando con dos cuentas de Google reales en dos celulares —
salieron 3 bugs reales, cada uno confirmado con evidencia (capturas de pantalla o logcat) antes
de arreglarlo:

1. **Miembro de Familia mostraba su id local (una UUID) en vez de su nombre.** Causa: `EspacioMiembro`
   nunca guardó el nombre — solo `usuarioId` (el id LOCAL de cada quien, una UUID distinta por
   dispositivo, sin significado fuera de su propio celular) y `rol`. Para "mí mismo" la pantalla
   ya resolvía el nombre desde `Usuario` local, pero para cualquier OTRO miembro (llegado por
   sync remoto) no había de dónde sacarlo. Se agregó `EspacioMiembro.nombre` (denormalizado,
   `MIGRATION_30_31`), se sube en `subirMiembro`/se lee en `escucharMiembros`, y
   `EspacioRepositoryImpl.agregarMiembro` preserva el nombre ya guardado si el nuevo valor llega
   null (para que un sync viejo sin nombre nunca borre uno que ya se sabía).
2. **Alarma con un sonido extra antes del loop propio.** Ver sección de arriba — canal de
   notificaciones con un sonido asignado a mano desde Ajustes del sistema en el dispositivo real
   del usuario, confirmado con `dumpsys notification` (`mUserLockedFields` != 0). Arreglado
   subiendo el canal a `_v4` (mismo patrón que `_v2`→`_v3` antes).
3. **Bug propio de esta sesión, encontrado y arreglado de inmediato**: al agregar
   `MIGRATION_30_31`, se importó en `DatabaseModule.kt` pero se olvidó agregar a la lista real
   `.addMigrations(...)` — la app crasheaba al abrir con `IllegalStateException: A migration
   from 30 to 31 was required but not found`. Se detectó con logcat en vivo durante una prueba de
   recordatorio (que además quedó contaminada por una reinstalación mía en simultáneo — ver
   nota de proceso abajo) y se corrigió en el momento.

**Nota de proceso**: durante esta ronda, una reinstalación de la app mientras un recordatorio de
prueba sonaba mató el proceso a mitad de la alarma — un corte que en el momento parecía el bug
que el usuario venía reportando ("suena y se corta"), pero era un artefacto de la propia sesión
de pruebas, no el bug real. Quedó como recordatorio de no reinstalar mientras se prueba algo en
vivo en el dispositivo.

## Ícono de la app que evoluciona con el tiempo (2026-08-23)

El usuario propuso un ícono que cambia solo con el uso — semilla → plantita → flor — inspirado en
apps como Genshin Impact que cambian su ícono real del launcher. Se construyó con la técnica
estándar de Android para esto: 3 `activity-alias` en el manifest (todas apuntando a
`MainActivity`, cada una con su propio ícono), con solo una `enabled` a la vez — `MainActivity`
en sí ya NO lleva el intent-filter de LAUNCHER (si lo tuviera además de los alias, aparecerían
4 íconos en vez de 1).

**Regla de evolución, definida junto con el usuario tras un par de iteraciones**:
- 🌱 Semilla: 0-29 días desde `Usuario.onboardingCompletadoEn` (el "día 0" de la cuenta).
- 🌿 Plantita sin flor: 30+ días, pero (a) todavía no llega a 60 días, o (b) ya los pasó pero
  ahora mismo no tiene racha activa.
- 🌸 Flor: 60+ días de cuenta **y** racha activa ahora mismo — si se corta la racha, vuelve a
  plantita sin flor (nunca retrocede a semilla).

`ActualizarIconoAppUseCase` calcula el estado y llama `PackageManager.setComponentEnabledSetting`
best-effort en cada apertura de la app (mismo patrón que el resto de checks de `AppViewModel`) —
no reinicia ni mata el proceso en curso (`PackageManager.DONT_KILL_APP`).

**Detalle de las imágenes**: las 3 imágenes que dio el usuario ya venían con un fondo tipo
"tarjeta" (esquinas redondeadas horneadas en el propio dibujo, no transparencia real) — para el
ícono adaptativo de Android (que aplica su propia máscara: círculo, squircle, gota según el
launcher) se generó una versión escalada al ~62% dentro de un lienzo transparente más grande,
para que la máscara no recorte el personaje. No es 100% idealmente "limpio" (Android igual
recorta ligeramente el borde de la tarjeta original), pero se ve bien en la práctica — el usuario
confirmó verlo bien en su dispositivo real.

## Perfil de usuario: arreglo de sincronización + saltar registro en celular nuevo (2026-08-23)

El usuario probó el registro en el segundo celular, escribió "Eli" en "¿cómo te llamo?", y al
revisar Firebase Console encontró que el perfil en la nube seguía mostrando "Tú" (el placeholder
del usuario semilla). Causa real, encontrada leyendo el código (sin necesitar el segundo
celular): `subirPerfil` se llama en el paso "Cuenta" del registro (justo después de vincular con
Google) — que es ANTES del paso "¿cómo te llamo?" en el flujo de onboarding. El nombre real nunca
se volvía a subir después de escribirlo.

**Arreglado en dos puntas**:
1. `OnboardingViewModel.finalizar()` vuelve a subir el perfil después de guardar las respuestas
   (ahí sí ya tiene el nombre real). `ProfileViewModel` también resube el perfil al editar
   horarios de comida, por el mismo motivo (evitar que cualquier edición posterior quede
   pegada en la nube con un valor viejo).
2. `subirPerfil` ahora también manda `nombreCompleto`, horarios de comida, y
   `onboardingCompletadoEn` — antes solo mandaba `nombrePreferido`/`correo`.

**Gap más grande, encontrado al responder "¿si cambio de celular, recupero 'Eli'?"**: el perfil
solo se SUBÍA, nunca se DESCARGABA — un celular nuevo con la misma cuenta de Google nunca traía
el nombre real, y como `onboardingCompletadoEn` es un campo 100% local, el registro se volvía a
pedir desde cero en cualquier celular nuevo, aunque la cuenta ya existiera. Se agregó
`CompartirSyncRepository.restaurarPerfil(firebaseUid)` (pull) y
`UsuarioRepository.aplicarPerfilRemoto(...)`, llamados desde `ReclamarCuentaConGoogleUseCase`
ANTES de subir de nuevo (para no pisar un nombre real con el placeholder semilla si es la
primera vez que se abre en este dispositivo). `ReclamarCuentaConGoogleUseCase` ahora devuelve
`Boolean` (si la cuenta ya tenía el registro completo en otro lado) — `OnboardingViewModel` lo
usa para saltarse el resto del wizard (preguntas, privacidad) y entrar directo a Hoy si ya
estaba todo respondido antes, en vez de volver a preguntar. Ver también la nota de "Ajustes" (no
tocado en esta ronda) en `10-pendientes.md`.

## Círculo de cuidado: ver el contenido real compartido (2026-08-23)

Cierre del hueco documentado desde hace varias rondas ("Ver el contenido real de lo que me
compartieron" — antes, aceptar una `SolicitudCompartir` solo sincronizaba la solicitud/conexión,
nunca el hábito/tarea/medicamento en sí). Se construyó con el mismo nivel de detalle que ya tenía
Familia: Hábito, Tarea, Rutina, Medicamento (con tomas recientes), Cita (con sesiones de curso) y
Fecha importante — decidido con el usuario, alcance completo en vez de solo Tarea/Hábito.

**Diseño**: colección nueva `actividadesCompartidas/{solicitudId}` (Firestore) — un documento por
`SolicitudCompartir` ya `ACEPTADA` de tipo `ACTIVIDAD`. Solo quien comparte escribe
(`deFirebaseUid`); solo quien comparte y a quien le comparten (`paraCorreo`, correo verificado)
pueden leer — mismo patrón de seguridad que `solicitudes_compartir`.

- `SincronizarActividadCompartidaUseCase`: sube el contenido completo de una actividad
  compartida y aceptada — se llama (a) cada vez que `CareCircleScreen` refresca sus "enviadas"
  (así detecta cuando la otra persona recién aceptó y sube por primera vez), y (b) cada vez que
  se marca la actividad (`MarcarActividadUseCase`/`MarcarTomaMedicamentoUseCase`/
  `MarcarSesionCitaUseCase`), vía `SincronizarSiEstaCompartidaUseCase` (resuelve si la actividad
  tiene alguna solicitud ACEPTADA saliente antes de subir). No es un listener en vivo permanente
  como Familia — es "best-effort, se refresca cuando hay una acción real", mismo criterio que el
  resto de sync de esta app.
- `CancelarSolicitudCompartirUseCase` ahora también borra el espejo de contenido al cancelar/
  revocar — si no, quien acompañaba seguiría viendo la actividad después de perder el acceso.
- Pantalla nueva "Lo que me comparten" (`LoQueMeComparteScreen`), accesible desde Círculo de
  cuidado — escucha en vivo `actividadesCompartidas` filtrado por mi correo, muestra cada
  actividad con quién la comparte y un resumen de su estado actual (hecho hoy/no, tomas
  recientes, sesiones cumplidas, etc.). Decidido con el usuario: pantalla aparte, no mezclado con
  Hoy (son conceptualmente cosas distintas — lo propio vs. lo que se acompaña).

**Gap relacionado, encontrado pero NO resuelto en esta ronda**: compartir una **Meta** por
Círculo de cuidado ya estaba roto de antes (no es parte de esta ronda) — `MetaDetailScreen` tiene
el mismo botón "🤝 Compartir seguimiento" que Hábito/Tarea/etc., pero una Meta vive en su propia
tabla (`Meta`), no en `Actividad` — al aceptar, `AceptarSolicitudCompartirUseCase` busca
`solicitud.elementoId` en la tabla `actividad` y nunca lo encuentra (es en realidad un id de
`Meta`), así que no pasa nada. Pendiente, ver `10-pendientes.md`.

## Quitar/salir de Familia, dejar de ver algo compartido, y sync al editar — añadido 2026-08-23

El usuario probó lo recién construido y encontró tres huecos reales con una sola pregunta:
"¿cómo elimino a una persona de Familia, y si dejo de compartir algo se borra de Firebase de
verdad, y puedo salir de una invitación/de algo que me compartieron?" Una segunda pregunta
("si algo ya creado se edita, ¿se actualiza en Firebase?") encontró un cuarto hueco.

**1. Quitar a un miembro de Familia (admin) y salir uno mismo.**

Necesitaba un dato que no existía: `EspacioMiembro` no guardaba el `firebaseUid` del miembro,
solo su `usuarioId` local — pero Firestore guarda la membresía por documento con id =
`firebaseUid` (ver reglas de `espacios/{id}/miembros/{miembroFirebaseUid}`), así que sin ese
dato el dispositivo del admin no podía saber qué documento borrar para otra persona. Se agregó
`firebaseUid: String?` a `EspacioMiembro`/`EspacioMiembroEntity` (`MIGRATION_31_32`), capturado
gratis desde `doc.id` durante el listener ya existente (`escucharMiembros`) — no hizo falta
ningún mecanismo de sync nuevo, el dato ya estaba en Firestore, solo faltaba guardarlo.

- `EliminarMiembroEspacioUseCase`: valida que quien ejecuta sea ADMIN (doble validación — igual
  en las reglas de Firestore, ver abajo) antes de borrar local + remoto.
- `SalirDeEspacioFamiliaUseCase`: cualquier miembro puede borrar su propia membresía; si el
  espacio del que sale era el activo, vuelve a `null` (Personal).
- Reglas nuevas: función `esAdmin()` en `firestore.rules` (`get()` del propio doc de membresía,
  no solo `exists()` como `esMiembro()`) — `allow delete` en `miembros/{miembroFirebaseUid}`
  ahora permite borrar la propia fila (salir) O cualquier fila si soy admin (quitar a otro).
- Decisión de producto explícita, confirmada con el usuario: **el contenido ya creado se queda
  en el espacio** al quitar/salir — no se borra en cascada. Coherente con "nada se pierde, nada
  se castiga" (`Plan/CLAUDE.md`).
- UI: botón "Quitar" por fila de miembro (solo visible si `soyAdmin` y no es uno mismo) y
  "🚪 Salir de este espacio" en `FamiliaScreen`, ambos con `ConfirmarEliminarDialog`.

**2. "Dejar de ver esto" — el destinatario se desconecta de un contenido compartido, sin
depender de que quien comparte revoque primero.**

Antes solo existía el camino de quien comparte ("Revocar acceso", que sí borraba
`actividadesCompartidas` de verdad — se confirmó y quedó documentado, era la duda #2 del
usuario). Faltaba el camino inverso. `DejarDeVerActividadCompartidaUseCase` reutiliza
`EstadoSolicitud.RECHAZADA` sobre una solicitud que ya estaba `ACEPTADA` (mismo estado, otro
momento — evita inventar un tercer estado solo para esto), sube ese cambio, y además borra
directamente el documento `actividadesCompartidas` del lado del destinatario para que
desaparezca al instante sin esperar el próximo refresh de quien comparte. Regla de
`actividadesCompartidas` ampliada: `allow delete` ahora también acepta
`request.auth.token.email == resource.data.paraCorreo`, no solo el dueño. Botón "Dejar de ver
esto" en cada tarjeta de `LoQueMeComparteScreen`, con confirmación.

**3. Editar (no solo marcar) algo compartido no se actualizaba en Firebase.**

La pregunta del usuario destapó que `SincronizarSiEstaCompartidaUseCase` solo estaba conectado
a los casos de uso de **Marcar** y al refresco de "enviadas" — ninguno de los seis
`Actualizar*UseCase` (Tarea/Hábito/Rutina/Medicamento/Cita/FechaImportante) lo llamaba. Alguien
editaba la hora de una cita compartida y quien lo acompañaba seguía viendo la hora vieja
indefinidamente. Se agregó la misma llamada `runCatching { sincronizarSiEstaCompartidaUseCase(...) }`
a los seis, y un caso simétrico que tampoco existía: `EliminarActividadesCompartidasDeUseCase`,
llamado desde `EliminarActividadUseCase` antes de borrar, para que borrar el original limpie
también su espejo en `actividadesCompartidas` (antes quedaba huérfano, visible para siempre del
otro lado).

Compilado, `installDebug` limpio, sin crash verificado con `adb logcat`. Reglas nuevas
publicadas por el usuario en Firebase Console. Pendiente probar de punta a punta con los dos
celulares reales (quitar miembro, salir, dejar de ver, editar algo compartido) — ver
`10-pendientes.md`.

## "Compartir seguimiento" con QR instantáneo — añadido 2026-08-24

El usuario probó compartir una Tarea y encontró que "🤝 Compartir seguimiento" pedía escribir
nombre/correo/teléfono ANTES de mostrar cualquier QR — y ese QR (`InvitacionEnviadaDialog`) era
solo texto de invitación para WhatsApp, no un código que la app supiera leer. Pidió explícitamente
que todo lo compartible tuviera QR fácil, "parecido a Listas". Se rediseñó `CompartirActividadDialog`
(un solo archivo, usado por los 5 detalles de Actividad: Hábito/Tarea/Rutina/Medicamento/Cita) con
el mismo patrón de dos caminos que ya tenía `FamiliaScreen`: **QR primero** (sin escribir nada) y
**correo/teléfono en texto** como opción secundaria, colapsada, para alguien que no está presente.

**Diseño, mismo espíritu que `codigosInvitacionEspacio` de Familia pero async, sin necesitar que
quien comparte esté mirando la pantalla en ese momento:**

- Colección nueva `codigosCompartirActividad/{codigoId}` (Firestore) — un código de 3 minutos con
  `actividadId`/`tipoActividad`/`nombreActividad`/`permiso`/`deUsuarioId`/`deFirebaseUid`/
  `deNombre`/`expiraEn`/`reclamadoPor`. `GenerarCodigoCompartirActividadUseCase` lo crea;
  `CompartirPorQrController` (clase con `@Inject constructor`, sin scope de Hilt — cada ViewModel
  de detalle recibe su propia instancia, mismo criterio que la duplicación ya existente del botón/
  diálogo en los 5 detalles) lo muestra como QR (`CompartirPorQrDialog`) y escucha en vivo el mismo
  documento mientras el diálogo sigue abierto, para mostrar "✅ confirmado" apenas alguien lo
  escanea — sin loop de auto-renovación (a diferencia del código de Familia): si vence sin
  escanearse, pide tocar "Generar de nuevo", más simple de razonar que un timer en segundo plano.
- **Reclamar el código NO pasa por "pendiente → aceptar" como el resto de Círculo de cuidado**:
  `ReclamarCodigoCompartirActividadUseCase` reclama el código (transacción, igual que
  `reclamarCodigoInvitacion`) y crea la `SolicitudCompartir` directo con `estado = ACEPTADA`,
  usando el mismo id que el código (`solicitudId == codigoId`) — clave para la regla de
  seguridad: la regla de `create` en `solicitudes_compartir` ahora también permite que sea
  **quien escanea** quien cree el documento (no solo quien comparte, como antes), siempre que
  declare `estado: ACEPTADA`, `para` == su propio correo verificado, y un `get()` confirme que
  el código con ese mismo id de verdad fue reclamado por él y pertenece a quien dice compartir.
  Sin este mecanismo, solo quien comparte podía escribir ahí — pero quien comparte no está
  presente en el momento del escaneo para autorizarlo, así que la única forma de que sea
  realmente instantáneo (sin depender de que el otro dispositivo esté online/escuchando) era que
  quien escanea cree el documento él mismo, con la regla verificando la legitimidad vía el código
  ya reclamado.
- **El contenido real (`actividadesCompartidas`) sigue subiéndolo solo quien comparte** — eso no
  cambió (la regla de esa colección exige `auth.uid == deFirebaseUid`, y sigue siendo así a
  propósito). En la práctica esto ya se resuelve solo: `CareCircleViewModel` sincroniza
  "solicitudes recibidas" (que ahora incluye esta, ya `ACEPTADA`, en cuanto quien comparte abra
  "Mi círculo de cuidado" o la sincronización en vivo la traiga) y su colector de "enviadas" ya
  resube el contenido apenas ve una solicitud propia en estado `ACEPTADA` — mismo mecanismo
  best-effort que toda esta sección de la app, sin agregar nada nuevo para esto.
- **Meta queda excluida a propósito**: `CompartirActividadDialog` ganó un parámetro
  `soportaQr: Boolean = true`; `MetaDetailScreen` pasa `false`. Ofrecerle QR a Meta habría hecho
  más engañoso el bug ya documentado (Meta no vive en `Actividad`, el contenido nunca llega) — el
  QR mostraría "✅ confirmado" y después nunca aparecería nada del otro lado, peor que el estado
  "Pendiente" honesto que ya tenía.
- Escáner global: nuevo prefijo `LULA_CODIGO_COMPARTIR_V1:` (`CodigoCompartirActividadQrCodec.kt`),
  agregado como cuarta rama en `TopBarStatsViewModel.escanear` (después de Lista, código de
  Espacio, contacto).

Compilado limpio (`compileDebugKotlin`), `installDebug` en dispositivo real, sin crash verificado
con `adb logcat`. Reglas nuevas (`codigosCompartirActividad` + `create` ampliado de
`solicitudes_compartir`) pendientes de publicar por el usuario en Firebase Console. Falta probar
de punta a punta con los dos celulares reales.

**Corrección del ícono de QR (mezclaba emoji con el ícono real) — añadido 2026-08-24**: el
usuario probó la ronda anterior y notó que el botón nuevo de "Compartir seguimiento" usaba el
emoji "🔳" como texto del botón, en vez del ícono real `Icons.Filled.QrCode` que ya se había
definido (2026-08-20) para todo lo que "genera/muestra un QR" (Familia, Listas, Perfil) — con
`Icons.Filled.QrCodeScanner` reservado solo para el botón de ESCANEAR de la barra superior. Se
corrigió `CompartirActividadDialog.kt` para usar `Icon(Icons.Filled.QrCode, ...)` igual que los
demás, y se sacó el emoji "🔳" de los textos de instrucción (`CompartirPorQrDialog.kt`),
reemplazado por la misma frase que ya usaba Familia ("Con el botón de escanear de su Lula") en
vez de referenciar un símbolo.

## Recorte de alcance de "Compartir seguimiento" + varios Espacios Familia — añadido 2026-08-24

Dos decisiones de producto tomadas juntas en la misma conversación.

**1. "Compartir seguimiento" ya no existe en Hábito, Meta ni Rutina — son "más personales".**
El usuario decidió explícitamente: Hábito/Meta/Rutina se sacan del todo (no solo el QR — el
botón completo desaparece de esas 3 pantallas); Tarea, Medicamento y Cita lo mantienen. Razón
dada por el usuario: Tarea "puede armarse con una persona y sería como una meta, y la tarea la
reemplaza hasta que se cumpla" (Tarea ya cubre el caso de "meta compartida" sin necesitar que
Meta tenga su propio mecanismo); Medicamento/Cita son el caso real de cuidado (acompañar a
alguien con sus medicamentos/citas). Se eliminó `CompartirActividadDialog`/
`CompartirPorQrDialog`/`CompartirActividadUseCase`/`CompartirPorQrController` por completo de
`HabitDetailScreen`/`ViewModel`, `RoutineDetailScreen`/`ViewModel` y `MetaDetailScreen`/
`ViewModel` (antes Meta solo tenía `soportaQr = false`; ahora no tiene ningún botón). El bug ya
documentado de Meta (no vive en `Actividad`, el contenido nunca llegaría a "Lo que me comparten")
queda sin efecto práctico porque ya no hay forma de intentar compartir una Meta.

**2. Varios Espacios Familia por usuario.**
El usuario preguntó cómo modelar el caso real: una persona pertenece a varias "familias" a la
vez (la que formó con su pareja/hijos, la de sus padres/hermanos, la de su pareja/suegros).
Investigación previa a construir (agente `Explore`) confirmó que **la capa de datos ya lo
soportaba sin ningún cambio**: `Espacio`/`EspacioMiembro` (Room), `EspacioRepository` y todos los
casos de uso de `domain/usecase/espacio/` ya están parametrizados por `espacioId` explícito
(nunca buscan "la" Familia); Firestore (`misEspacios` puntero, reglas de `espacios/{espacioId}`)
ya itera sobre todos los documentos sin límite por usuario. El único lugar que asumía "cero o una
Familia" era `FamiliaViewModel`/`FamiliaScreen` (`espacios.firstOrNull { tipo == FAMILIA }`).

Rediseño de `features/family/`:
- `FamiliaUiState` gana `familias: List<FamiliaResumenUi>` (todas mis Familias) y
  `familiaSeleccionadaId: String?` (cuál se está viendo/administrando) — **deliberadamente
  independiente** de "espacio activo" (el selector de arriba, que sigue existiendo igual): así
  se puede invitar a alguien a la Familia de tus padres sin tener que cambiar tu espacio de
  trabajo del día a día.
- `FamiliaScreen` ahora tiene una sección "Tus espacios familiares" con una fila por Familia +
  botón "Administrar"/"Ocultar" cada una, y "+ Crear otro espacio familiar" siempre visible
  (antes el botón de crear desaparecía en cuanto existía una).
- `FamiliaViewModel.seleccionarFamilia(espacioId, nombre)` reemplaza la carga automática de "la"
  Familia — cancela el listener de miembros anterior (`jobMiembros`) antes de escuchar la nueva,
  para no mezclar datos de dos Familias si el usuario cambia de selección rápido.
- Los 6 métodos de acción (renombrar/eliminar/salir/quitar miembro/invitar/generar QR) pasaron de
  leer `espacioFamiliaId` a leer `familiaSeleccionadaId` — mismo patrón, ahora N-capaz.
- **Límite conocido, no resuelto esta ronda**: "🏆 Retos familiares" navega usando el espacio
  ACTIVO (`RetosFamiliaresViewModel`/`CrearRetoFamiliarViewModel` leen
  `obtenerSesionActualUseCase().espacioId`, no reciben un `espacioId` explícito por navegación).
  Si administras una Familia que no es la activa, el botón se reemplaza por un aviso de "cambia
  de espacio primero" en vez de abrir los retos de la Familia equivocada. Extender Retos (y
  Tareas del hogar) a navegación explícita por `espacioId` queda pendiente, ver
  `10-pendientes.md`.

Compilado limpio, `installDebug` en dispositivo real, sin crash verificado con `adb logcat`.
Ningún cambio de esquema Room ni de `firestore.rules` en esta ronda (confirmando que era
puramente un límite de UI). Falta probar con una segunda Familia real de punta a punta.

## Dos bugs reales de "no se borra/no se cancela de verdad" — añadido 2026-08-24

El usuario creó Familias de prueba, las eliminó, y siguieron apareciendo. Investigado con
evidencia real (código + `sqlite3` sobre la base de datos real del dispositivo, no adivinado).

**1. "Eliminar espacio Familia" no tocaba Firestore.** `EliminarEspacioFamiliaUseCase` solo
borraba local (Room) — nunca borraba el documento `espacios/{id}`, su membresía, ni mi propio
puntero `misEspacios/{id}`. Como `RestaurarEspaciosFamiliaUseCase` corre en **cada apertura de
la app** (`AppViewModel.init`) y trae de vuelta todo lo que encuentra en `misEspacios`, la
Familia "eliminada" volvía sola la siguiente vez que se abría Lula. Arreglado:
`EspacioSyncRepository.eliminarEspacioCompleto(espacioId)` nuevo — borra las 4 subcolecciones
(`actividades`/`retos`/`registrosReto`/`miembros`), el documento raíz, y mi puntero. **El orden
importa** por las reglas de seguridad: cada borrado depende de que mi propia membresía siga
existiendo (`esMiembro()`/`esAdmin()`), así que mi propio documento de `miembros` se borra
**al final**, después de todo lo demás (esa regla no depende de nada más:
`auth.uid == miembroFirebaseUid`). Los punteros `misEspacios` de otros miembros quedan
huérfanos pero inofensivos — `descubrirMisEspacios` los descarta solo en cuanto el documento raíz
ya no existe. De paso: `EliminarEspacioFamiliaUseCase` ahora exige ser admin (antes cualquier
miembro podía borrar la Familia entera).

**2. "Pausar" un Medicamento (o una Cita con varias sesiones) no cancelaba sus alarmas de
verdad.** Bug distinto, encontrado mientras se investigaba un reporte de "configuré Sonido y
sonó como Alarma". Medicamento programa una alarma independiente por horario (clave compuesta
`"$actividadId:$horario"`, ver `RecordatorioScheduler.claveRequestCode`) — pero
`PausarReanudarActividadUseCase` llamaba `recordatorioScheduler.cancelar(actividadId)` sin
horario, que solo cancela la clave simple (la que usan Hábito/Tarea). Este mismo bug **ya se
había encontrado y arreglado para "Eliminar"** (`EliminarActividadUseCase`, Fase 0.8,
2026-07-28) pero nunca se replicó a "Pausar" — quedó con el bug original. Resultado real: la
pantalla mostraba "Pausado", pero las alarmas seguían armadas en `AlarmManager` y sonaban en su
horario normal, sin ninguna pista de que algo estaba mal. Arreglado replicando exactamente el
mismo patrón de `EliminarActividadUseCase` (cancelar por cada horario de Medicamento, por cada
sesión×anticipación de Cita); de paso se agregó que **reanudar** un Medicamento pausado también
reprograma sus alarmas (antes tampoco pasaba, mismo hueco al revés).

**Diagnóstico de la base de datos real (no explicó la causa, pero confirmó otra cosa útil):**
`sqlite3` sobre la base pulled del dispositivo mostró varios Medicamentos de prueba del usuario
todavía `activa = 1` con `fechaFin` ya vencido hace días — pero `horariosParaFecha` (usado tanto
por `RecordatorioReceiver` al mostrar como por `ReprogramarTodosLosRecordatoriosUseCase` al
reabrir la app) ya respeta `fechaFin` correctamente (arreglado en una ronda anterior, ver
comentario en el propio código) — confirmado que esos específicos ya no pueden sonar. El bug de
"Pausar" (arriba) es la explicación más probable del reporte real. Se le sugirió al usuario
limpiar (pausar/eliminar) esos Medicamentos de prueba igual, por prolijidad — pendiente de que
lo confirme.

Se agregó además un `Log.i` permanente en `RecordatorioReceiver.mostrarNotificacionConNivel`
(nivel + canal usado) para diagnosticar con evidencia real cualquier caso futuro, en vez de
adivinar. Compilado limpio, `installDebug` en dispositivo real, sin crash verificado con
`adb logcat`.

**"¿Revisaste Lula?" y "¿Cómo te fue hoy?" ahora son Alarma — añadido 2026-08-25.** A pedido
del usuario: los avisos genéricos para abrir la app (por franja del día, y el de cierre de día)
sonaban siempre con el canal `RECORDATORIOS_SONIDO` fijo, sin usar el nivel Alarma nunca — el
usuario los quiere insistentes, no un simple sonido de mensaje/campana. Se reescribieron
`mostrarNotificacionFranja`/`mostrarNotificacionCierreDia` para pasar por
`mostrarNotificacionConNivel(..., NivelRecordatorio.ALARMA, ...)`, el mismo camino compartido que
ya usan Hábito/Tarea/Medicamento/Cita/Meta en Alarma (pantalla completa + `AlarmaSonidoService`
en loop). No quedó configurable por el usuario (no hay selector de nivel en Ajustes para estos
dos, a diferencia de cada Hábito/Tarea individual) — si más adelante hace falta elegir, se agrega
ahí. Compilado limpio, `installDebug` en dispositivo real, sin crash verificado con `adb logcat`.
**Revertido el mismo día**: el usuario lo probó y resultó demasiado molesto (sonaba sin parar,
no encontró cómo detenerlo) — quería algo suave ("avisar sin molestar mucho"), no Alarma. Vuelto
a `NivelRecordatorio.SONIDO`.

## Bug real confirmado: el canal "Sonido" sonaba como Alarma — añadido 2026-08-25

El usuario probó los 3 niveles con pruebas espaciadas (Tareas nuevas a las 20:35/20:40/20:45, más
el recordatorio de franja a las 20:50) y un log en vivo (`adb logcat` streamed a un archivo,
correlacionado con el `Log.i` de diagnóstico agregado antes) confirmó algo contradictorio: la app
calculaba y usaba `nivel=SONIDO` con el canal `recordatorios_sonido_v2` — sin arrancar
`AlarmaSonidoService` — pero el sonido real que se escuchaba era el de Alarma.

**Causa real, confirmada con `dumpsys notification` (no adivinada):** el canal
`recordatorios_sonido_v2` tenía guardado `mSound=android.resource://com.aqpseller.lulaapp/2131623937`
— convertido a hex (`0x7F0E0001`) y buscado en la tabla de recursos de la build actual con
`aapt2 dump resources` (`C:\...\Sdk\build-tools\36.0.0\aapt2.exe dump resources app-debug.apk`),
ese ID resultó ser **`raw/lula_alarma_gorrion_habla_ventana`**, no `raw/lula_mensaje`. Mismo
mecanismo que el bug ya conocido del canal Alarma (`Plan/08-decisiones-tecnicas.md`, ronda
anterior): los IDs de `R.raw.*` que asigna el compilador de Android **no son estables entre
builds** — suelen asignarse en orden alfabético dentro del tipo de recurso, así que agregar
`lula_alarma_gorrion_habla_ventana.wav` (que alfabéticamente va ANTES que `lula_mensaje.wav`) en
algún momento corrió los IDs, y el archivo de mensaje pasó de `0x7F0E0001` a `0x7F0E0002`. El
canal, creado en una build anterior a ese cambio, quedó con el número viejo grabado para
siempre (Android no deja que la app actualice el sonido de un canal ya creado) — y ese número
ahora resuelve al archivo equivocado.

**Arreglo**: mismo patrón ya usado 3 veces para el canal Alarma — `RECORDATORIOS_SONIDO` pasa de
`_v2` a `_v3`, agregado `recordatorios_sonido_v2` a `CANALES_HUERFANOS` (se borra solo al abrir
la app). Verificado con `dumpsys notification` tras instalar: `recordatorios_sonido_v2` quedó
`mDeleted=true`, `recordatorios_sonido_v3` se creó con `mSound=.../2131623938` (`0x7F0E0002`,
confirmado con `aapt2` que es `raw/lula_mensaje`) y `mUserLockedFields=0` (limpio). Esto también
explica retroactivamente el reporte de "Sonido sonó como Alarma" de rondas anteriores en esta
misma sesión — no era un bug de código en `RecordatorioReceiver`/`PausarReanudarActividadUseCase`
(esos sí tenían bugs reales, ya arreglados, pero no eran la causa de ESTE síntoma en particular),
sino este canal con el ID pegado. **Lección para el futuro**: cualquier vez que se agregue un
nuevo recurso `res/raw/*`, hay que revisar si empuja los IDs de los `raw` existentes y, si algún
canal de notificación ya creado depende de uno de ellos, bumpear su sufijo de versión de una vez
— no esperar a que un dispositivo real lo revele.

Diagnosticado con evidencia real de punta a punta: `Log.i` en vivo (streamed con `adb logcat -v
time` a un archivo mientras el usuario probaba), `sqlite3` sobre la base real para confirmar los
niveles guardados, y `dumpsys notification` + `aapt2 dump resources` para encontrar la causa
exacta del canal. Compilado limpio, `installDebug` en dispositivo real, sin crash verificado con
`adb logcat`.

**Confirmado por el usuario (2026-08-25, 21:07-21:15)**: 5 pruebas reales espaciadas 2 minutos —
Silencioso, Sonido (x2, una de ellas el recordatorio de franja), Alarma (x2, una de ellas un
Hábito viejo) — los 3 niveles sonaron como corresponde, verificado también en el log en vivo
(`recordatorios_sonido_v3` en las de Sonido, `recordatorios_alarma_v4` en las de Alarma). De
paso confirmó que "Pausar"/desactivar un Hábito con Alarma sonando ahora sí lo corta de verdad
(el otro bug arreglado esta misma ronda). Cierra el hueco.

## Bug real: creador de Familia bajado a Miembro solo + co-admins/historial — añadido 2026-08-27

El usuario creó una Familia, agregó un miembro, y reportó "dice Alguien - Miembro, no encuentro
cómo borrarla" — ni él mismo aparecía como admin. Confirmado con `sqlite3` sobre la base real:
**ningún miembro tenía `rol = 'ADMIN'`**, ni siquiera el creador.

**Causa real**: `SincronizarEspacioFamiliaUseCase.respaldarMiPresenciaRemota` — pensado para
"autorreparar" un Espacio viejo subiendo mi propia membresía si Firestore no la tenía — usaba
`RolEnEspacio.MIEMBRO` como valor por defecto cuando mi fila local (`encontrado`) resultaba
`null`. Si este método corre en una carrera de tiempos justo después de crear el espacio (antes
de que la fila local de ADMIN terminara de guardarse), pisaba mi propia membresía con MIEMBRO
tanto en Firestore como local — y el listener en vivo de `escucharMiembros` bajaba la copia
local ya buena a MIEMBRO también, vía `EspacioRepositoryImpl.agregarMiembro`, que sobrescribe
`rol` sin ninguna protección (a diferencia de `nombre`/`firebaseUid`, que si vienen `null`
preservan el valor ya guardado). **Arreglado en la fuente**: el fallback ahora usa
`Espacio.creadoPor` (dato estable, no depende de una carrera) — si nadie más es admin y yo soy
quien creó el espacio, me quedo/vuelvo ADMIN. Esto también autorrepara una Familia que ya haya
quedado corrupta por este bug, la próxima vez que ese Espacio vuelva a ser el activo.

**Aprovechando la pregunta, el usuario pidió un modelo de permisos más completo** (respuesta
detallada a `AskUserQuestion`):
- **Co-admins**: puede haber varios admins a la vez (no una sola persona) — nuevo
  `HacerAdminEspacioUseCase`/`QuitarAdminEspacioUseCase`, botones simétricos "Hacer admin"/
  "Quitar admin" por fila de miembro, ambos con confirmación (`ConfirmarEliminarDialog` ganó
  `titulo`/`textoConfirmar` opcionales para reusarse acá, no solo para eliminar).
- **El creador está protegido**: ni "Quitar" ni "Quitar admin" tienen efecto sobre quien creó el
  espacio si lo ejecuta OTRO admin — solo él mismo podría (`SalirDeEspacioFamiliaUseCase`, ya
  existente). Verificado en `EliminarMiembroEspacioUseCase`/`QuitarAdminEspacioUseCase`.
- **Solo el creador elimina el espacio completo**: `EliminarEspacioFamiliaUseCase` pasó de
  "cualquier admin" a `espacio.creadoPor == usuarioId` — un co-admin puede agregar/quitar gente
  pero no borrar todo el grupo.
- **Historial visible solo para admins**: nuevo `HistorialCambiosRepository` (dominio, primera
  vez que se LEE `HistorialCambios` — existía desde Fase 0.1 solo para escribir) +
  `ObtenerHistorialMiembrosEspacioUseCase`, filtrado a `accion = 'ELIMINAR'` sobre
  `entidad = 'espacio_miembro'` (a propósito sin incluir `CREAR`, que se dispara en cada
  sync/merge de rutina y ensuciaría el historial con ruido en vez de "quién quitó a quién").
  Botón "📜 Ver historial de quitados", solo visible si `soyAdmin`.

**Bug encontrado y arreglado mientras se construía esto**: `EspacioSyncRepositoryImpl.subirMiembro`
siempre escribe en el documento de **quien llama** (usa `firebaseAuth.currentUser.uid` como id
del documento, ignorando lo que diga el `EspacioMiembro` pasado) — funcionaba bien para todos los
usos anteriores (siempre alguien subiendo su PROPIA membresía), pero "Hacer admin"/"Quitar admin"
necesitan escribir en el documento de OTRA persona. Se agregó
`EspacioSyncRepository.actualizarRolMiembro(espacioId, miembroFirebaseUid, nuevoRol)` — un
`update` de un solo campo (`rol`) al documento correcto, sin tocar `subirMiembro` (para no
arriesgar sus usos ya establecidos).

**Reglas de Firestore ampliadas**: `miembros/{miembroFirebaseUid}` — `update` ahora permite,
además de uno mismo, que un admin cambie el campo `rol` de otra persona
(`esAdmin() && diff().affectedKeys().hasOnly(['rol'])` — ningún otro campo).

Compilado limpio, `installDebug` en dispositivo real, sin crash verificado con `adb logcat`.
Pendiente que el usuario republique las reglas y confirme que su Familia se autorreparó.

## Bajar admin a Miembro + confirmación + un bug de verificación real — añadido 2026-08-28

**"Quitar admin"** (bajar a un co-admin a Miembro normal, contraparte de "Hacer admin") y
confirmación (`ConfirmarEliminarDialog` ganó `titulo`/`textoConfirmar` opcionales) para ambas
acciones — a pedido del usuario ("no hay llave o pregunta de seguridad"). Nuevo
`QuitarAdminEspacioUseCase`, con la misma protección al creador que el resto (otro admin no
puede bajarle el admin al creador).

**Bug real encontrado de paso en código recién escrito**: `subirMiembro` siempre escribe en el
documento de QUIEN LLAMA (usa `firebaseAuth.currentUser.uid`, ignorando lo que diga el
`EspacioMiembro` pasado) — bien para todos los usos anteriores (siempre alguien subiendo su
propia membresía), pero "Hacer admin"/"Quitar admin" necesitan escribir en el documento de OTRA
persona. Se agregó `EspacioSyncRepository.actualizarRolMiembro(espacioId, miembroFirebaseUid,
nuevoRol)` — un `update` de un solo campo al documento correcto, sin tocar `subirMiembro`.
Reglas de Firestore ampliadas para permitir que un admin cambie solo el campo `rol` de otro.

**El episodio más importante de esta ronda no fue de la app — fue de cómo se estaba verificando
que las builds funcionaran.** El usuario probó "Quitar admin" y no aparecía en pantalla, en
ningún lado, ni siquiera invisible (se descartó tocando toda la fila). Investigado con
evidencia real, en capas:
1. `sqlite3` sobre la base del dispositivo — confirmó datos correctos.
2. `adb shell dumpsys package ... | grep lastUpdateTime` — mostró que la fecha de instalación
   NUNCA cambiaba entre intentos, ni con `./gradlew clean installDebug`.
3. `adb pull` del APK realmente instalado (`pm path` para encontrarlo) + extraer los `.dex` +
   `grep` binario por el texto `"Quitar admin"` — **cero coincidencias**. El APK instalado en el
   dispositivo era, byte a byte, una build anterior a la que se creía haber instalado.
4. Revisando `app/build/outputs/apk/` directamente — ni siquiera existía un APK local. La build
   nunca había llegado a empaquetar nada.
5. Recién ahí apareció la causa real: `./gradlew installDebug -q 2>&1 | tail -N` — el flag `-q`
   más el pipe a `tail` **ocultaban tanto errores reales de compilación como el código de
   salida real** (en un pipeline de shell, el código de salida que ve quien llama es el de
   `tail`, no el de `gradlew` — `tail` casi siempre "sale bien" aunque el comando anterior haya
   fallado). El error real: `EspacioSyncRepository.kt` — se agregó el parámetro
   `nuevoRol: RolEnEspacio` a la interfaz sin importar `RolEnEspacio`, lo que rompía la
   compilación desde la ronda anterior. Cada "compilado limpio ✅" reportado desde ese momento
   fue una falsa confirmación — el proyecto llevaba build tras build reinstalando la MISMA APK
   vieja sin que ninguna señal lo delatara, hasta que se comparó el binario real.

**Corrección permanente**: de acá en adelante, los comandos de compilar/instalar se corren SIN
pipes que puedan enmascarar el código de salida (ni `| tail`, ni `-q` combinado con pipe) —
dejando que el propio mecanismo de tareas en segundo plano reporte éxito/fallo real a partir del
código de salida verdadero, y revisando el archivo de salida completo cuando hace falta ver el
motivo de un fallo. Import agregado, recompilado (esta vez con el método correcto, que si
mostró `BUILD FAILED` real la primera vez que aún faltaba el celular conectado), instalado, y
confirmado con las mismas 3 capas de evidencia (fecha de instalación nueva, tamaño de APK
distinto, y el texto "Quitar admin" presente en el `.dex` real) antes de decirle al usuario que
ya estaba listo — nunca más solo "el comando no tiró error".

## Ajustes sincronizados entre celulares — añadido 2026-08-28

Cierra un hueco documentado desde hace varias rondas: sonido de check, día de Revisión semanal,
horas de recordatorio (cierre de día + las 3 franjas), posición de la barra inferior (2/3/4), y
duración máxima de la Alarma — antes 100% locales (DataStore), se perdían al cambiar de celular.

**Diseño**: nueva subcolección `usuarios/{firebaseUid}/ajustes/config` en Firestore —
deliberadamente NO como campos del documento raíz `usuarios/{firebaseUid}` (ahí vive el perfil,
y `subirPerfil` hace un `.set()` completo sin merge; agregar Ajustes ahí haría que cada subida
de uno pisara los campos del otro). `AjustesRepositoryImpl` (la única clase que toca DataStore)
ahora recibe `CompartirSyncRepository` inyectado, y cada setter relevante llama a un
`sincronizarConNube()` privado al final — sube una foto completa de todos los Ajustes
sincronizables cada vez que cambia cualquiera (más simple que rastrear campo por campo).

**Restaurar es una sola vez, no en cada apertura** (a diferencia de `PersonalSyncRepository`,
que sí corre en cada apertura porque ahí es un *merge* seguro) — Ajustes son escalares simples
sin versión/timestamp propio, así que restaurar en cada apertura arriesgaría pisar un cambio
recién hecho en ESE MISMO celular con una copia vieja de otro. Se ata al momento de vincular la
cuenta (`ReclamarCuentaConGoogleUseCase`, nuevo `RestaurarAjustesUseCase`), igual que el perfil.

**Deliberadamente fuera de esta ronda**: `espacioActivoId` NO se sincroniza — ya se había
decidido antes (2026-07-30) que ese valor vive solo en memoria (nunca en disco) para que cerrar
y volver a abrir la app siempre vuelva a Personal, evitando que el usuario piense que sus datos
se borraron. Sincronizarlo entre celulares chocaría con esa decisión ya validada. Tampoco
`ultimoHitoRachaCelebrado` (no es una preferencia real, es solo para no repetir una celebración).

Compilado sin pipe (ver lección de la sección anterior), `installDebug` confirmado con fecha de
instalación nueva, sin crash verificado con `adb logcat`.

## Retos familiares navegables por `espacioId` explícito — añadido 2026-08-28

Cierra el límite que había quedado documentado al agregar varias Familias por usuario
(2026-08-24): "🏆 Retos familiares" solo se podía abrir para la Familia que fuera el espacio
ACTIVO en ese momento — si administrabas una Familia distinta a la activa, veías un aviso de
"cambia de espacio primero" en vez del botón.

**Cambio**: `LulaDestinations.RETOS_FAMILIARES`/`CREAR_RETO_FAMILIAR` ganaron `{espacioId}` como
argumento de ruta (mismo patrón que `habito/{actividadId}`, etc.) — `RetosFamiliaresViewModel`/
`CrearRetoFamiliarViewModel` lo leen de `SavedStateHandle` en vez de resolverlo desde
`ObtenerSesionActualUseCase().espacioId` (el espacio activo global). `FamiliaScreen` ahora
navega siempre con `uiState.familiaSeleccionadaId` — la Familia que se está administrando, sin
importar cuál sea la activa. Se sacó el aviso "cambia de espacio primero", ya no hace falta.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, sin crash
verificado con `adb logcat`.

## "Recordarle" (permiso "Puede ver y recordar") — añadido 2026-08-29

Último de los 3 huecos elegidos el 2026-08-28 (junto con Ajustes sincronizados y Retos por
`espacioId`). Quien acompaña con permiso `PUEDE_VER_Y_RECORDAR` en "Lo que me comparten" ahora
tiene un botón real "🔔 Recordarle" — antes el permiso existía en el modelo de datos pero no
hacía nada.

**Diseño, mismo criterio "best-effort" que el resto de esta sincronización** (no hay
infraestructura FCM/push en la app): nueva colección Firestore `recordatoriosSolicitados/{id}`.
Quien acompaña escribe un documento (`deFirebaseUid`, `paraFirebaseUid`, `actividadId`,
`nombreActividad`, `deNombre`) vía `SolicitarRecordatorioUseCase` →
`CompartirSyncRepository.solicitarRecordatorio`. Del lado de quien comparte, `TopBarStatsViewModel`
(ya elegido antes para el aviso 📩 de solicitudes pendientes, por ser "efectivamente global")
agrega un listener en vivo (`escucharRecordatoriosSolicitados`) que, mientras la app esté
abierta, muestra una notificación local (`NotificationChannels.RECORDATORIOS_SONIDO`, mismo
canal que un recordatorio nivel Sonido normal) y borra el documento apenas se muestra —
best-effort real: si el celular de quien comparte no tiene la app en memoria en ese momento, el
aviso nunca llega (se queda el documento en Firestore hasta la próxima vez que se conecte el
listener, entonces se muestra tarde). `ActividadCompartidaRemota` ganó el campo `deFirebaseUid`
(faltaba, necesario para saber a quién dirigir el aviso).

Regla de Firestore agregada (`recordatoriosSolicitados/{id}`): crear solo si
`request.auth.uid == deFirebaseUid`; leer/borrar solo si `request.auth.uid == paraFirebaseUid`.
**Pendiente de publicar en Firebase Console** — no se ha pedido ni confirmado esta ronda.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva y el texto
"Recordarle"/"recordatoriosSolicitados" presente en el `.dex` real, sin crash verificado con
`adb logcat`.

## Notificaciones bidireccionales de invitación (Familia + Círculo de cuidado) — añadido 2026-08-29

Cierra un hueco real reportado por el usuario: invitar a alguien por correo/teléfono (sin estar
presente, a diferencia del QR) no avisaba a nadie — ni a quien invita cuando la otra persona
responde, ni llegaba nada nuevo a quien recibe salvo que abriera la app y por casualidad entrara
a revisar. El usuario pidió explícitamente que fuera "lo más motivador y entendible posible".

**Reutiliza toda la infraestructura existente de `SolicitudCompartir`** (ya cubre tanto Familia
como Círculo de cuidado, `TipoSolicitud.ESPACIO`/`ACTIVIDAD` — ver `InvitarAEspacioUseCase`) en
vez de construir un sistema nuevo. Piezas agregadas:

- `SolicitudCompartir` ganó `nombreQuienResponde` (nueva columna, migración Room 32→33) — antes
  solo se sabía `para` (correo/teléfono, feo para un mensaje) de quien acepta/rechaza; ahora
  `AceptarSolicitudCompartirUseCase`/`RechazarSolicitudCompartirUseCase` guardan el nombre real.
- `SincronizarYDetectarEventosSolicitudesUseCase` (nuevo): envuelve
  `escucharSolicitudes` comparando cada solicitud entrante contra lo que ya había en Room antes
  de aplicarla — detecta solo 2 eventos reales (`NuevaRecibida`, `Respondida`), nunca se repite
  al reabrir la app ni reconectar el listener (una vez aplicado el cambio a Room, la próxima
  comparación ya no encuentra diferencia — no hace falta una bandera aparte de "ya avisado").
- `TopBarStatsViewModel` corre este listener globalmente (mismo patrón que "Recordarle") y
  postea una notificación local con copy motivador según tipo+evento (invitación nueva,
  aceptada, rechazada) — canal `RECORDATORIOS_SONIDO`.
- Al aceptar (`CareCircleViewModel.aceptar`), además del cambio de estado, muestra de inmediato
  un diálogo de bienvenida motivador en pantalla (no espera al roundtrip de Firestore, ya está
  ahí mismo tocando el botón).
- El ícono 🔔 de la barra superior (antes 📩, solo visible si había algo pendiente) ahora es
  permanente con `BadgedBox` — es la única entrada real a "Mi círculo de cuidado", que ya
  mostraba tanto lo pendiente por responder como el estado de lo enviado.

No hizo falta tocar `firestore.rules`: la regla de `solicitudes_compartir` ya permite `update`
sin restricción de campos a quien envió o a quien recibe, así que el nuevo campo pasa igual.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, migración 32→33
aplicada sin error (verificado con `adb logcat`, datos existentes intactos), y el texto
"nombreQuienResponde" presente en el `.dex` real.

**Corrección el mismo día**: la primera versión de esto reusaba "Mi círculo de cuidado" como
destino del ícono 🔔, sin pantalla propia. El usuario aclaró que esperaba un historial real
(mandó como referencia el "Notificaciones" de una app de alarma: agrupado por fecha, leído/no
leído, queda guardado aunque ya se haya actuado). Se construyó de nuevo con tabla propia — ver
sección siguiente.

## Pantalla real de Notificaciones (historial permanente) — añadido 2026-08-30

Reemplaza el enfoque anterior (🔔 como acceso directo a "Mi círculo de cuidado") por un
historial de verdad: tabla nueva `notificacion` en Room (migración 33→34,
`NotificacionRepository`/`NotificacionDao`) — cada aviso que antes solo se posteaba al sistema
(y se perdía) ahora también queda guardado ahí (`emoji`, `titulo`, `cuerpo`, `fecha`, `leido`,
`solicitudId` opcional para poder llevar a "Mi círculo de cuidado" si sigue siendo una
invitación). `TopBarStatsViewModel.postearYRegistrar` es el único punto que posta Y guarda —
así ningún aviso nuevo puede quedar fuera del historial por accidente.

Pantalla nueva `NotificacionesScreen` (ruta `notificaciones`): agrupada por fecha ("Hoy"/"Ayer"/
fecha), filtro "Todo"/"No leído", punto rojo en lo no leído, tocar una fila la marca leída y, si
tiene `solicitudId`, navega a "Mi círculo de cuidado" (que sigue existiendo aparte, como pantalla
de GESTIÓN — aceptar/rechazar/cancelar — no de historial). El ícono 🔔 de la barra superior ahora
abre esta pantalla nueva, con el badge = no leídas (antes contaba solo solicitudes pendientes).
"Mi círculo de cuidado" se mantiene accesible desde el menú "⋮".

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, migración 33→34
sin error (verificado con `adb logcat`), y el texto "notificacionesNoLeidas" presente en el
`.dex` real.

## Ícono 🔔 con estado (color) + sincronizar el historial de Notificaciones — añadido 2026-08-30

Dos ajustes pedidos tras probar la ronda anterior:

**Ícono con estado**: el 🔔 (emoji) se veía amarillo siempre, sin distinguir a simple vista si
había algo nuevo. Se reemplazó por `Icon` real (Material, ya se usa `material-icons-extended`
en el proyecto): `Icons.Outlined.Notifications` (silueta neutra) sin nada pendiente,
`Icons.Filled.Notifications` con tinte amarillo (`0xFFFFC107`) apenas hay algo sin leer.

**Historial sincronizado**: antes la tabla `notificacion` era 100% local — el usuario preguntó
directamente qué pasaba al cambiar de celular, y la respuesta real era "el historial se pierde"
(lo pendiente se recupera solo porque `solicitud_compartir` sí sincroniza; lo ya leído/resuelto,
no). Se agregó sync completo, mismo patrón que el resto de "lo Personal"
(`PersonalSyncRepository`, subcolección `usuarios/{uid}/notificaciones/{id}`):
- `NotificacionRepositoryImpl.registrar` sube la notificación a Firestore además de guardarla
  local (best-effort, `runCatching`).
- `marcarLeida` también actualiza el campo `leido` remoto — así el estado de lectura también
  viaja entre celulares, no solo la existencia del aviso.
- `RestaurarDatosPersonalesUseCase` (corre en cada apertura, ya es un merge seguro para todo lo
  Personal) ahora también restaura notificaciones — `@Upsert` por id, nunca duplica.

Regla de Firestore agregada (`usuarios/{uid}/notificaciones/{id}`, mismo patrón que
`registrosDiarios`/`ajustes`): solo el dueño de la cuenta lee/escribe. **Publicada en Firebase
Console (2026-08-30).**

**Límite conocido, aceptado a propósito**: si `marcarNotificacionLeidaRemota` falla por estar
offline, la próxima restauración (merge) podría devolver esa notificación a "no leída" — mismo
tipo de compromiso "best-effort" que ya existe en el resto de esta sincronización, no se
construyó cola de reintentos para este caso.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, sin crash
verificado con `adb logcat`. Verificación visual del ícono pendiente — el celular de prueba
tenía bloqueo por patrón y no se forzó.

## Buscadores en Diario y Finanzas + desglose por categoría + exportar a Excel — añadido 2026-08-30

A pedido del usuario, explícitamente **sin gráficos** (la app usa texto/emoji, no charts —
confirmó que prefiere copiar los valores y graficarlos él mismo en Excel si quiere). Una idea de
"web para ver todo desde la computadora" quedó anotada para más adelante, no esta ronda.

**Diario**: `EntradaDiarioDao.buscar` (nuevo, `LIKE` sobre `texto` en la base de datos, no trae
todo a memoria) + `BuscarEntradasDiarioUseCase`. `DiaryListViewModel` alterna entre
`observarPorEspacio` (consulta vacía) y `buscar` (consulta con texto) vía `flatMapLatest` con
`debounce(250)` — evita disparar una consulta por cada tecla y cancela la anterior si llega una
más nueva ("un buscador eficiente que no se sienta lento", pedido explícito). `DiaryListScreen`
gana un `OutlinedTextField` de búsqueda; el estado vacío distingue "nunca escribiste nada" de
"no hay resultados para tu búsqueda".

**Finanzas**: mismo patrón — `FinanzasDao.buscar` (`LIKE` sobre `categoria`/`descripcion`) +
`BuscarMovimientosUseCase`, pero a propósito busca en **todo el historial**, no solo el
mes/rango visible (el pedido era "ver cuándo gasté eso", cruza períodos). `FinancesHistoryUiState`
gana `movimientosVisibles` (resultados de búsqueda si hay consulta activa, si no el período
normal) y `egresosPorCategoria`/`ingresosPorCategoria` (suma agrupada por categoría, de mayor a
menor, calculada en memoria sobre lo ya visible — sin necesidad de una consulta nueva). Mientras
se busca, la navegación por mes/rango se oculta (no aplica).

**Copiar a Excel**: `FinancesHistoryViewModel.textoParaCopiar()` arma el período/búsqueda
visible como texto separado por tabulador (fecha ISO, tipo, categoría, monto, descripción) —
pega directo como columnas en Excel/Sheets. Botón "📋 Copiar lo que estás viendo" usa
`ClipboardManager` (mismo mecanismo que "Correo copiado" en el escáner QR global).

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, sin crash
verificado con `adb logcat`, y "textoParaCopiar"/"egresosPorCategoria" presentes en el `.dex`
real.

## Rediseño de "Historial" de Finanzas — añadido 2026-08-30

El usuario reportó (con captura) que el encabezado de esta pantalla (buscador + chips + mes +
resumen + categorías + copiar) se comía ~40% de la pantalla en su celular, dejando muy poco
espacio real para ver el historial de movimientos.

**Causa**: la pantalla usaba una `Column` fija arriba de una `LazyColumn` — el encabezado nunca
scrolleaba, solo la lista de abajo, así que ese espacio quedaba perdido para siempre sin importar
cuánto se scrolleara.

**Arreglo**: todo pasó a una sola `LazyColumn` (encabezado como `item {}`, movimientos como
`items()`) — al scrollear, el encabezado se va con el resto y la lista gana toda la pantalla.
Además, el desglose por categoría (lo más largo del encabezado) ahora arranca **colapsado**
("📊 Por categoría ▸ Ver"), a pedido explícito del usuario ("poner como desplegable").

Verificado visualmente con una captura real del dispositivo (no solo compilación) — el
encabezado quedó compacto y "MOVIMIENTOS" con varias filas visibles aparece sin necesidad de
scrollear. Compilado sin pipe, instalado, sin crash.

## Racha ya no baja + calendario calcula solo — añadido 2026-08-29

Dos cambios pedidos juntos tras un caso real: el usuario tenía una racha de varios días, se le
olvidó cerrar un día (una jornada particularmente ocupada probando la app conmigo) y a la mañana
siguiente la vio en 0 — sintiéndolo como un castigo, no como algo que lo invite a seguir.

**Racha ahora es acumulativa, nunca baja** (`ObtenerProgresoDeHoyUseCase.calcularRachaActual`):
antes era "días CONSECUTIVOS cerrados" (se rompía a 0 si faltaba uno); ahora es el total de días
cerrados con ≥1 actividad cumplida, sin importar los huecos — saltarse un día simplemente no la
sube, nunca la baja. Decisión de producto explícita del usuario, con el respaldo de que un
contador que retrocede castiga algo que ya pasó en vez de motivar a seguir (mismo principio que
el "perdón de un día" de apps de hábitos como Duolingo).

Tres piezas nuevas para que el cambio se entienda (a pedido explícito, no solo el número solo):
- Tocar el 🔥 de la barra superior (antes solo navegaba a Historial) ahora muestra primero un
  diálogo explicando la mecánica, con un botón para seguir a Historial.
- `ObtenerProgresoDeHoyUseCase.diaAnteriorSinCerrar` (nuevo): true si ayer quedó sin cerrar Y la
  persona ya tiene el hábito de cerrar (para no molestar a alguien que recién instaló la app el
  día de hoy). Banner motivador en Hoy (color propio, no el rojo de error de los otros avisos)
  invitando a cerrarlo desde el calendario.
- **Cerrar un día pasado desde el Calendario ahora calcula solo** en vez de partir en 0/manual:
  `CerrarDiaViewModel` reutiliza `ObtenerAgendaDelRangoUseCase` (la misma reconstrucción
  histórica día-por-día que ya usan las vistas Semana/Mes del Calendario) para contar
  actividades completadas/totales de esa fecha puntual — el comentario anterior de "no hay forma
  confiable de recalcular" quedó desactualizado desde que esa reconstrucción existe. Los campos
  siguen editables por si hace falta corregir algo a mano.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, sin crash
verificado con `adb logcat`. Racha verificada contra los datos reales del dispositivo: pasó de
mostrar 0 a mostrar 14 (el total real de días cerrados con actividad, antes escondido por la
regla de "consecutivos").

## Alarma que no se apagó al tocar "Detener" durante una videollamada — diagnosticado 2026-08-30

Reporte real del usuario: estaba en videollamada de WhatsApp, sonó un recordatorio nivel Alarma,
tocó el botón que apareció, la notificación se ocultó, pero el sonido siguió.

**Evidencia real en `adb logcat`** (no adivinado): el proceso de Lula arrancó normal
(`RecordatorioReceiver` → `AlarmaSonidoService` en foreground), pero minutos después aparecieron
`"mediaplayer went away with unhandled events"` y `"A resource failed to call release"` — el
`MediaPlayer` de la alarma nunca pasó por nuestro código de limpieza (`detenerReproduccion()`,
que sí libera todo correctamente cuando se ejecuta); quedó huérfano y recién lo cerró el
recolector de basura, minutos tarde. Eso confirma que en algún momento `detener()` no llegó a
correr sobre la instancia que de verdad tenía el sonido activo — el registro se cortó antes de
capturar el detalle fino de por qué (el buffer de logcat es limitado y hay mucho tráfico durante
una videollamada), así que la causa exacta (¿doble entrega del disparo de `AlarmManager` bajo
presión de CPU/batería de la videollamada? ¿una carrera entre dos `onStartCommand`?) no quedó
100% confirmada — pero sí quedó confirmado que es real, no una percepción.

**Corrección aplicada, defensiva**: `AlarmaSonidoService.iniciar()`/`detener()`/
`detenerReproduccion()` ahora son `@Synchronized` — antes nada protegía el campo `mediaPlayer`
compartido si dos `onStartCommand` llegaban casi al mismo tiempo (ej. la misma alarma entregada
dos veces), que podían pisarse entre sí y dejar un reproductor sonando sin que ninguna
notificación visible ya apuntara a él. También: `stopSelf(startId)` en vez de `stopSelf()` sin
argumentos (más correcto con múltiples comandos pendientes), y logging explícito en cada punto
(`onStartCommand`, `iniciar()`, `detener()`, error real de `stop()`/`release()`) para que, si
esto vuelve a pasar, el log deje ver exactamente qué instancia tenía el reproductor y qué pasó,
en vez de quedar ambiguo como esta vez.

**Límite honesto**: si la causa real fue el sistema operativo matando el proceso bajo presión de
memoria/CPU de una videollamada activa (posible, Android puede hacerlo con casi cualquier
proceso en background bajo suficiente presión), esto no se puede prevenir al 100% desde la app.

Compilado sin pipe, `installDebug` confirmado con fecha de instalación nueva, sin crash
verificado con `adb logcat`.

