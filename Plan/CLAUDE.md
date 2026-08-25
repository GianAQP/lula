# Lula — Guía del proyecto para Claude Code

## Qué es Lula - Leer jose

Lula es una app Android (Kotlin) de **mejora continua personal**: organiza hábitos, rutinas,
tareas, metas, finanzas y cuidado familiar, y ayuda a la persona a medir su progreso diario
y semanal para mejorar poco a poco, sin presión ni castigos.

**Filosofía del producto**: Pequeñas acciones. Todos los días. Mejora continua.
**Principio de diseño transversal**: Todo intento vale. No hay castigos. Mañana se vuelve a intentar.

El ciclo central que organiza todo el producto:

```
PLANIFICAR → EJECUTAR → REGISTRAR → CERRAR EL DÍA → MEDIR → REVISAR (semanal) → AJUSTAR
```

Hábitos, tareas, finanzas y cuidado no son módulos sueltos: son piezas que alimentan este ciclo.

## Stack técnico

- **Plataforma**: Android nativo
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: Clean Architecture (data / domain / features)
- **Persistencia local**: Room
- **Inyección de dependencias**: Hilt
- **Backend real, construido y en uso (decidido 2026-08-01, Firebase Auth + Firestore)** —
  reemplaza el plan original de Google Sheets vía n8n para sincronización de datos. n8n se
  mantiene como plan solo para el webhook del asistente conversacional / "armar con IA" de Mi
  propósito (Fase 2.0), no para sync de datos. Ver `Plan/12-firebase-auth-y-sync.md`.
- **Autenticación**: Google Sign-In construido y funcionando (Credential Manager API). Correo
  mágico sin contraseña sigue solo diseñado, no construible todavía — necesita un dominio propio
  configurado en Firebase (Dynamic Links, la forma vieja de resolverlo sin dominio, fue dado de
  baja por Google).
- **Registro obligatorio al abrir la app (2026-08-23)**: `OnboardingScreen` gatea la entrada —
  ya no se salta directo a Hoy con el usuario semilla. Si la cuenta de Google ya había
  completado el registro en otro dispositivo, lo salta solo (trae el perfil real de la nube en
  vez de volver a preguntar). Ver `Plan/06-onboarding.md`, `Plan/08-decisiones-tecnicas.md`.
- **Modelo de datos**: local-first, sincronización a la nube en segundo plano — Familia/
  Conexiones (varias personas, listener en vivo, incluye código de invitación de 60s para
  Familia), Círculo de cuidado (solicitud/aceptación **y**, desde 2026-08-23, el contenido real
  de lo compartido — pantalla "Lo que me comparten"), y el Espacio Personal completo (todos los
  tipos de Actividad + Finanzas/Diario/Notas/Metas/Listas/Mi propósito/Cerrar mi día/Revisión
  semanal — un solo dispositivo activo a la vez, push+restaurar-una-vez, sin restricción de
  premium todavía). Ver `Plan/12-firebase-auth-y-sync.md` y `Plan/08-decisiones-tecnicas.md`.

## Regla de diseño no negociable

Existe una entidad genérica `Actividad` (ver `Plan/01-arquitectura.md`) que representa
hábito, tarea, rutina, medicamento, cita, evento y fecha importante. **Todo módulo nuevo debe
reutilizar esta estructura — nunca crear una tabla paralela.** Esto es lo que permite que el
proyecto escale de personal → cuidado de familiares → familia/equipo sin reconstruir la base
de datos.

## Documentación del proyecto

Este documento es un resumen de entrada. El detalle completo está separado por tema:

| Archivo | Contenido |
|---|---|
| `Plan/01-arquitectura.md` | Modelo de datos completo, capas de la app, lógica de sincronización, reglas de privacidad |
| `Plan/02-pantallas.md` | Especificación de cada pantalla: contenido exacto, estados, navegación |
| `Plan/03-vocabulario.md` | Glosario de términos del dominio (Actividad, Espacio, Constancia, etc.) |
| `Plan/04-roadmap-fases.md` | Las 6 fases del proyecto, qué incluye cada una y en qué orden construir |
| `Plan/05-modelo-negocio.md` | Freemium: qué es gratis, qué es premium, estrategia de lanzamiento |
| `Plan/06-onboarding.md` | Flujo completo de registro, preguntas, casos especiales (invitación) |
| `Plan/07-asistente-voz.md` | Asistente conversacional: 4 modos, arquitectura, reglas de comportamiento |
| `Plan/08-decisiones-tecnicas.md` | Decisiones de implementación del núcleo técnico (Room, lecciones de MayiaApp aplicadas, usuario semilla) |
| `Plan/09-guia-visual.md` | Paleta de marca, componentes reutilizables (`StatPill`, `LulaProgressBar`, `EmptyState`) y qué mecánicas de gamificación evitar (leaderboards, rojo de alerta) |
| `Plan/10-pendientes.md` | Lista viva de qué falta y por qué — bloqueado por backend, piezas de UI que quedaron afuera de una fase "completa", fases sin empezar. Revisar antes de preguntar "¿qué falta?" |
| `Plan/11-cuentas-y-conexiones.md` | Diseño de "usuarios pendientes": datos personales + consentimientos legales, tabla `Conexion` (cómo se conectan familia/amigos), preguntas de onboarding vs. Mi propósito, checklist de Play Store |
| `Plan/12-firebase-auth-y-sync.md` | Diseño de login real (Google + correo mágico), recuperación de cuenta, y qué sincroniza con Firestore vs. qué se queda 100% local (el límite es `Espacio.tipo`) |

## Orden de construcción recomendado

Se construye por fases (ver `Plan/04-roadmap-fases.md`), empezando por **Fase 0.1 — Núcleo
personal**. No avanzar a la fase siguiente hasta que la anterior esté funcional y usable por
Giancarlo como primer usuario real.

**Antes de escribir código de una pantalla o función nueva**, revisar:
1. `Plan/03-vocabulario.md` — para usar los nombres correctos de entidades y campos
2. `Plan/01-arquitectura.md` — para confirmar que el dato encaja en el modelo existente
3. `Plan/02-pantallas.md` — para el contenido y comportamiento exacto esperado

## Convenciones de código

- Nombrar entidades y campos en el código igual que en `Plan/03-vocabulario.md` (en español,
  consistente con el dominio del producto), salvo convenciones técnicas propias de Kotlin/Android
  que exijan inglés (nombres de clases base, interfaces del framework, etc.)
- Cada tipo de `Actividad` (hábito, tarea, medicamento...) se maneja mediante el mismo modelo
  base con campos adicionales específicos — no subclases separadas sin relación entre sí
- Todo dato sensible (finanzas, diario, medicamentos, notas privadas) nace con
  `privacidad: solo_yo` por defecto
- Todo campo de texto libre debe soportar dictado de voz desde el MVP 0.1

## Estado actual del proyecto

Fase de diseño conceptual completada. En implementación: **Fase 0.1 — Núcleo personal**
(ver progreso detallado en `Plan/04-roadmap-fases.md`, que lista ✅ hecho / ⬜ pendiente
ítem por ítem — esa es la referencia para seguir y revisar el avance).

**Base técnica construida** (2026-07-25): arquitectura Clean Architecture completa
(`core/data/domain/features/di/navigation`), base de datos Room con las 20 entidades del
modelo de datos y sus índices/FKs, Hilt, usuario semilla local (sin Firebase todavía),
auditoría (`HistorialCambios`) conectada desde el primer commit, navegación con bottom bar
y menú `+`, y las pantallas Hoy / Crear hábito / Crear tarea / Registrar movimiento / Cerrar
mi día funcionando de punta a punta sobre datos reales. Decisiones técnicas y lecciones de
MayiaApp aplicadas están en `Plan/08-decisiones-tecnicas.md`. Sin sync a n8n/Sheets todavía
(Room 100% local).

**Hábitos y Tareas completos** (2026-07-25): lista/detalle/editar/pausar/eliminar para
Hábitos (con tracker semanal y racha por hábito, vía la nueva tabla `registro_actividad` —
ver `Plan/08-decisiones-tecnicas.md`) y lista/detalle/editar/completar/eliminar para Tareas.

**Finanzas completa** (2026-07-26): pantalla Finanzas (balance del mes, gastos de hoy),
historial del mes con edición/eliminación al tocar cada movimiento — accesible desde el
bottom bar.

**Primera pasada visual** (2026-07-26): paleta de marca cálida y propia (sin dynamic color),
bottom nav con íconos a color por sección, componentes reutilizables `StatPill` /
`LulaProgressBar` / `EmptyState` aplicados en Hoy / Hábitos / Tareas / Finanzas — inspirado en
Duolingo y Me+ pero sin sus mecánicas de presión (sin leaderboard, sin rojo de alerta). Se
corrigió además un bug de contraste (texto invisible sobre fondo claro fijo en modo oscuro) y
se agregaron íconos a color en el menú `+`. Detalle completo, la lección de contraste y qué
falta vestir todavía en `Plan/09-guia-visual.md`.

**Historial simple** (2026-07-26): pantalla con los días cerrados (fecha, puntos, actividades
cumplidas, las 3 respuestas de reflexión) — se accede tocando la insignia 🔥 de racha en Hoy o
con "Ver mi historial" al cerrar el día.

**Dictado de voz y Zona Privada** (2026-07-26): dictado por reconocedor del sistema
(`DictationTextField`) en todo campo de texto libre existente; Zona Privada con PIN (hash
SHA-256, nunca texto plano) + biometría (`BiometricPrompt`, requirió pasar `MainActivity` a
`FragmentActivity`) gateando la sección Finanzas. Auto-bloqueo por inactividad queda
pendiente. Detalle en `Plan/08-decisiones-tecnicas.md`.

**Notificaciones/recordatorios** (2026-07-27): hora opcional al crear/editar Hábito o Tarea
con fecha, programada con `AlarmManager` (`RecordatorioScheduler`). Los hábitos se
reprograman solos día a día; un `BootReceiver` los vuelve a programar todos tras reiniciar el
teléfono (si no, dejarían de sonar en silencio). Pide permiso de notificaciones una vez al
abrir la app (Android 13+). Detalle en `Plan/08-decisiones-tecnicas.md`.

Con esto, los pasos 1, 2, 3, 5, 6 y 7 del roadmap de Fase 0.1 (`Plan/04-roadmap-fases.md`)
están completos — el 4 (Onboarding) se saltó a propósito porque requiere que el usuario
configure un proyecto de Firebase (`google-services.json`); no bloqueó el resto del avance.
Solo queda pendiente en Fase 0.1: Onboarding (bloqueado por Firebase) y Sync con n8n/Sheets
(paso 8, cuando el workflow esté listo del lado de n8n).

**Fix + pulido a pedido del usuario** (2026-07-27): se corrigió un bug real (el checkbox de
un hábito en Hoy no se actualizaba en vivo — el `Flow` de Room no escuchaba la tabla
`registro_actividad`, ver la lección en `Plan/08-decisiones-tecnicas.md`). Se reemplazaron
los chips de hora/fecha fijos por un `TimePicker`/`DatePicker` reales. Se agregaron
categorías predefinidas en Finanzas con encaje automático de categorías dictadas por voz
("comida"/"Comida"/"comidas" caen en la misma), y un `StatPill` de "Ahorro" destacado cuando
el usuario ahorra en el mes. Bottom nav con íconos más grandes (36dp → 44dp).

**Fase 0.5 iniciada — Metas** (2026-07-27): crear (por hábito/monto/número/manual), lista y
detalle con progreso, agregar progreso manual, eliminar — accesible desde "Ver mis metas" en
Hoy. El progreso "por hábito" se calcula solo desde el historial del hábito vinculado,
reusando la misma lógica del tracker de Hábitos. Falta **editar** una meta ya creada, área de
vida y fecha límite — ver `Plan/08-decisiones-tecnicas.md`.

**Fix de notificación + 3 niveles de intensidad** (2026-07-27): se corrigió un bug real
(tocar una notificación de recordatorio no abría la app, faltaba `.setContentIntent(...)` en
`RecordatorioReceiver`) — ahora abre Lula directo en el Hábito/Tarea del recordatorio. Además,
a pedido del usuario, cada recordatorio ahora elige su propio nivel de insistencia:
🔇 Silencioso / 🔔 Sonido / ⏰ Alarma (`NivelRecordatorio`), con 3 canales de notificación fijos
(Android no permite cambiar el sonido de un canal ya creado por código) y un acceso directo
"🔊 Sonido de mis recordatorios" en Hoy que abre los Ajustes del sistema para personalizar cada
canal. Detalle completo y el trade-off del nivel "Alarma" (single-shot, no un despertador con
loop) en `Plan/08-decisiones-tecnicas.md`.

**Rutinas** (2026-07-27): tercera pieza de Fase 0.5. Agrupan Hábitos/Tareas ya existentes por
referencia (ej. "Rutina de mañana") — lista/detalle con checklist y un botón "Marcar rutina
completa" que marca todos los ítems incluidos de una vez. El estado de "completada" se calcula
en vivo (cuántos ítems están `CONFIRMADO` ahora), nunca se guarda aparte — misma filosofía de
una sola fuente de verdad que el progreso "por hábito" de Metas. Accesible desde "🧩 Ver mis
rutinas" en Hoy y desde el menú `+`; a propósito no aparece mezclada dentro de la lista de Hoy
(ver el porqué en `Plan/08-decisiones-tecnicas.md`).

**Revisión semanal** (2026-07-27): cuarta pieza de Fase 0.5. Resumen de la semana en curso
(cumplimiento general, racha máxima de la semana, hábito que mejor/peor le fue) más las 3
preguntas de reflexión de `02-pantallas.md` (dictado disponible) — accesible desde "🗓️ Ver mi
revisión semanal" en Hoy, sin el gating al domingo que especifica el documento (se puede
revisar la semana en cualquier momento, se guarda igual sobre el mismo registro de esa
semana). Nota técnica: se evitó `LocalDate.dayOfWeek` de kotlinx-datetime porque delega en
`java.time.DayOfWeek`, que necesita API 26+ y el proyecto no tiene desugaring — se calculó el
día ISO con aritmética propia en su lugar. Detalle en `Plan/08-decisiones-tecnicas.md`. De
Fase 0.5 quedan pendientes: pantalla "Progreso"/Constancia, Hábitos progresivos, Matriz de
Eisenhower.

**Ronda de feedback de uso real en dispositivo** (2026-07-27): el usuario probó la app en su
celular y reportó 6 problemas. El más grave era un crash real al crear una Meta o guardar la
Revisión semanal — diagnosticado con `adb logcat -b crash` (no adivinado): `MetaEntity` y
`RegistroSemanalEntity` no tenían `@Serializable`, y `AuditLogger` las serializa por reified
generics en cada escritura. Corregido; queda documentada como lección junto a la de Hilt
`@Provides`, porque ninguna de las dos la detecta el compilador. También: tocar una
notificación ahora abre una pantalla de acción rápida (Ya lo hice / Recuérdame en 15 min) en
vez de un formulario de edición; el nivel Alarma ya se ve sobre la pantalla bloqueada; "Sonido
de mis recordatorios" se movió a una pantalla Ajustes nueva detrás de un menú "⋮" en Hoy (ya
no vive en el flujo diario); "Ver mis metas"/"Ver mis rutinas" en Hoy solo aparecen si ya hay
al menos una (se agregó "Meta" al menú `+` para no perder el descubrimiento); y se aclaró con
texto explicativo la confusión entre Metas "por hábito" y la diferencia Rutina/Hábito. Detalle
completo en `Plan/08-decisiones-tecnicas.md`.

**Segunda ronda de feedback en dispositivo real** (2026-07-28): 8 puntos más, resueltos en
conjunto. Notificación de Alarma: ya no se queda sonando después de que el usuario toca una
acción (faltaba cancelar la notificación al abrir la pantalla de acción rápida, no solo al
tocarla). Sonido de check opcional en Hoy (`Switch` en Ajustes, DataStore propio,
`ToneGenerator` del sistema, sin mp3 bundleado). Dictado blindado con `Toast` cuando falla en
vez de fallar en silencio. Teclado numérico agregado a los 4 campos que faltaban. **Tareas
recurrentes** (pagos periódicos como luz/agua): nuevo `RecurrenciaTarea`
(diaria/semanal/quincenal/mensual/bimestral/trimestral/anual) — al marcarla hecha, se
reprograma sola con la próxima fecha y queda en el historial, igual que un Hábito. **Listas
reutilizables** (viaje, compras): nueva entidad `Lista`/`ListaItem` fuera del modelo de
`Actividad` (mismo precedente que Meta) — plantilla de ítems que se "reinicia" (desmarca) para
la próxima vez, sin duplicar. Revisión semanal: ahora precarga y permite editar lo ya guardado
de la semana en curso (antes siempre arrancaba en blanco). Detalle completo de las 8 en
`Plan/08-decisiones-tecnicas.md`.

**Tercera ronda de feedback en dispositivo real** (2026-07-28): todas las pantallas de
formulario/detalle (13 archivos) ganaron `verticalScroll` — ninguna tenía, así que con
teclado abierto o mucho contenido seleccionado el botón de guardar quedaba fuera de la
pantalla, inalcanzable. El menú "⋮" de Hoy abría el `DropdownMenu` en el lugar equivocado
porque estaba anclado a un `Row` de ancho completo en vez de al ícono — corregido envolviendo
ícono+menú en su propio `Box`. Nuevo acceso rápido "Ver todas mis tareas/hábitos" desde
Crear Tarea/Hábito. Corregido un bug real en `TasksListScreen`: el `Checkbox` estaba dentro
de la misma fila clickeable que navegaba al detalle, y los dos gestos competían — el check no
se veía marcar. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Cuarta ronda de feedback en dispositivo real** (2026-07-28): quedaba un segundo bug real
detrás de "el check no cambia" — 12 ViewModels compartían un patrón de sesión cacheada que
podía descartar la acción en silencio si se tocaba muy rápido apenas se abría la pantalla
(condición de carrera, no relacionada con el bug de `TasksListScreen` de la ronda anterior).
Corregido en los 12 con un helper `sesionActual()` resuelto dentro de la propia corrutina —
ahora es una regla general para todo ViewModel nuevo (ver
`Plan/08-decisiones-tecnicas.md`). También de esa ronda: sonido de check + tachado
extendido a Tareas/Rutina/Lista (antes solo Hoy); confirmación obligatoria antes de eliminar
en las 7 acciones de eliminar de la app (`ConfirmarEliminarDialog`); accesos rápidos "Ver ya
creados" en las 4 pantallas de Crear que faltaban; y una pantalla nueva "📜 Ver semanas
anteriores" para leer las revisiones semanales guardadas (nunca se borraban, solo faltaba
dónde leerlas). Se agregó una sección de convenciones de UI de cumplimiento obligatorio para
toda pantalla nueva en `Plan/08-decisiones-tecnicas.md` — léela antes de construir la
siguiente pantalla.

**Fase 0.5 completa** (2026-07-28): las 3 piezas que quedaban. **Constancia** (% de días
activos en los últimos 30, independiente de la racha) como `StatPill` en Historial. **Matriz
de Eisenhower**: vista "🗂️ Matriz" dentro de Tareas que agrupa las mismas tareas
(`importante`/`urgente` ya existían, sin cambios de esquema) en HACER/PROGRAMAR/DELEGAR/
POSPONER — 4 secciones apiladas en vez del 2x2 del documento, más legible en celular.
**Hábitos progresivos**: nuevo `duracionActualMin`/`proximaRevisionEpochDay` en
`ActividadDetalle.Habito` (`version = 7`) — un hábito se vuelve "progresivo" solo si el
usuario llena los 4 campos detrás de un chip "📈 Aumentar con el tiempo" en Crear Hábito.
Cuando toca revisar, aparece una tarjeta en Hoy ("Completaste X de Y días... ¿Aumentamos?")
con 3 botones (Subir/Mantener/Recordarme después) — Lula pregunta, nunca decide sola. Detalle
técnico completo (incluida la decisión de no extender la reactividad de Hoy a
`habito_detalle`, y la lección de auditoría aplicada preventivamente esta vez) en
`Plan/08-decisiones-tecnicas.md`. Fase 0.5 queda funcionalmente completa salvo la pantalla
"Progreso" unificada de `02-pantallas.md` (sus piezas ya existen, repartidas).

**Fase 0.8 completa** (2026-07-28): `Actividad` se extendió a Medicamento y Cita, todavía
`privacidad: solo_yo` (`version = 8`, nueva tabla `toma_medicamento` — una toma es por
horario, no por día, porque un medicamento puede tener varias dosis diarias). Medicamento
tiene dos modos de frecuencia (cada cierto número de horas, o según las comidas con los
horarios de comida guardados en `Usuario` y pedidos inline la primera vez que hacen falta) y
reutiliza el mismo `NivelRecordatorio`/`AlarmManager` de Hábito/Tarea, con una alarma
independiente por cada horario del día. Cita tiene fecha/hora, lugar, motivo y anticipación
configurable. Pantalla nueva "Mi salud" agrupa medicamentos activos (con sus tomas de hoy) y
próximas citas; las tomas de hoy también aparecen directo en Hoy, con 3 estados igual de
válidos (Pendiente/Tomada/Omito, nunca un castigo) reutilizados también en la pantalla de
acción al tocar la notificación. La regla de producto — Lula solo recuerda y registra lo que
el usuario indicó, nunca sugiere cambiar dosis ni horarios — se muestra explícitamente en la
pantalla de crear medicamento. De paso se corrigió un bug real: eliminar un medicamento con
varias tomas diarias no cancelaba todas sus alarmas (`EliminarActividadUseCase` solo conocía
la clave simple, no la compuesta por horario). Detalle técnico completo en
`Plan/08-decisiones-tecnicas.md`.

**Feedback de Fase 0.8 en dispositivo real** (2026-07-28): 6 correcciones (`version = 9`).
Menú "⋮" se movió al `Scaffold` del `NavHost` para que aparezca en toda la app, no solo en
Hoy. Dictado por voz: ahora detecta pantalla bloqueada antes de intentar abrir el micrófono
(antes solo "parpadeaba" sin aviso) — un intento de arreglar además el caso sin conexión con
`EXTRA_PREFER_OFFLINE` resultó ser una regresión peor (rompía el dictado por completo en
teléfonos sin modelo de voz descargado) y se revirtió al toque tras probarlo. Chips de toma
de Medicamento rediseñados para que **solo el estado activo lleve
color** (antes los 3 —pendiente/tomada/omitida— se veían igual de coloridos). Bug real de
contraste corregido: las tarjetas con color de marca (`Card` con `containerColor` fijo a la
variante "Light") quedaban con texto invisible en modo oscuro — nuevo helper
`ui/theme/ThemedContainer.kt` que elige la variante correcta según el tema. Cita ahora admite
**varios recordatorios, cada uno a su propia hora** (antes solo un offset a la misma hora de
la cita) — mismo patrón de alarma compuesta por recordatorio que ya usaba Medicamento, y se
corrigió el mismo bug de alarmas huérfanas al eliminar/editar. Detalle técnico completo en
`Plan/08-decisiones-tecnicas.md`.

**Segunda vuelta de feedback sobre Fase 0.8** (2026-07-28): "Presiono Crear y no pasa nada."
Causa real: `CrearMedicamentoViewModel`/`CrearCitaViewModel` tenían validaciones (nombre
vacío, falta la hora de la primera dosis, ningún modo "según las comidas" seleccionado) que
simplemente hacían `return` sin avisar nada — parecía un botón roto. Ahora exponen
`mensajeError: StateFlow<String?>` y la pantalla lo muestra con `Toast` explicando qué falta.
De paso, aclarado (no era bug): una Cita no aparece dentro de la lista de Hoy a propósito,
solo en "Mi salud" — Hoy únicamente muestra el enlace "Ver mi salud" una vez que existe algo.

**Fase 1.0 — Círculo de cuidado, base local** (2026-07-29): el usuario pidió avanzar a Fase
1.0 y luego 1.5; antes de empezar se marcó un bloqueo real — compartir entre dos personas en
teléfonos distintos necesita autenticación real y algún tipo de sync, y la app sigue siendo
100% local con el usuario semilla (sin Firebase conectado). Se acordó construir ahora la
mitad de Fase 1.0 que **no** depende de eso: `SolicitudCompartir` pasó de un stub con campos
`String` sin tipar (viviendo dentro de `RetoFamiliar.kt` desde Fase 0.1) a un modelo propio
con enums reales (`PermisoCompartir`, `EstadoSolicitud`, `CanalEnvio`); nuevo botón "🤝
Compartir seguimiento" en el detalle de Hábito/Tarea/Medicamento que crea la solicitud
(queda `PENDIENTE`, nunca se auto-acepta — sería falsear el estado); pantalla nueva "Mi
círculo de cuidado" (menú "⋮") con la sección "Quién me acompaña a mí" totalmente funcional
(listar, cancelar, revocar) y "Personas que acompaño" como estado vacío honesto en vez de
datos simulados, porque eso sí necesita los datos de otra persona sincronizados a este
dispositivo. Detalle técnico completo, incluida la corrección de un bug de mayúsculas/
minúsculas en una query que llevaba desde Fase 0.1 sin usarse, en `Plan/08-decisiones-tecnicas.md`.

**Quinta ronda de feedback en dispositivo real** (2026-07-29), 7 puntos. La más importante:
**el menú "⋮" que se movió al `Scaffold` la ronda anterior había quedado invisible en toda la
app** — `enableEdgeToEdge()` ya estaba activo, y el `Box` a medida de `LulaTopBar` no
manejaba el inset de la barra de estado (a diferencia de `TopAppBar` de Material3, que lo
hace solo), así que el ícono se dibujaba detrás del reloj/batería del sistema. Corregido con
`.statusBarsPadding()`. También: los chips de toma de Medicamento pasaron a usar un
`Checkbox` real (el texto "✅ Tomada" ya traía el visto incrustado aunque nada estuviera
marcado); **bug real de datos viejos** en las 5 pantallas de detalle (Hábito/Tarea/Rutina/
Meta/Medicamento) — Navigation Compose reutiliza el mismo ViewModel al volver de Editar, así
que hacía falta un `recargar()` explícito llamado desde `LaunchedEffect(Unit)` en la pantalla;
el historial de Finanzas ahora muestra fecha y día de la semana; "Mañana" (momento del día)
se aclaró a "Por la mañana" porque sonaba a "día siguiente"; la flecha "→" de `SectionLinkRow`
se agrandó (afecta a todas las filas "Ver mis X"); y el campo "Objetivo" de una Meta en modo
Manual ahora explica que siempre es un número. Detalle técnico completo en
`Plan/08-decisiones-tecnicas.md`.

**Compartir "moderno" + stats globales** (2026-07-29): el usuario pidió modernizar cómo se
comparte (QR, WhatsApp/Telegram, buscar usuarios existentes) y mover racha/gastos de Hoy al
mismo nivel que el menú "⋮". Mismo límite de siempre: sin backend, buscar usuarios existentes
y que un QR escaneado complete el acceso solo son imposibles (no hay servidor que empareje
dos teléfonos) — se construyó la mitad que sí funciona hoy: generar un QR local
(`com.google.zxing:core`, dependencia nueva, sin cámara ni permisos) con el texto de
invitación, y el selector nativo de Android (`Intent.ACTION_SEND`) para mandarlo por
WhatsApp/Telegram/cualquier app instalada — `core/ui/InvitacionEnviadaDialog.kt`, reemplaza
el `Toast` que había en los 3 flujos de "Compartir seguimiento". Racha/gastos de hoy se
movieron a `LulaTopBar` (`TopBarStatsViewModel` nuevo) — visibles en toda la app, no solo Hoy
— y el ícono 💰 ahora sí navega a Finanzas. Se agregó también un aviso "📩" de invitación
pendiente, conectado a una query que ya existía desde Fase 0.1 pero que va a estar en 0 hasta
que haya backend. Detalle técnico completo en `Plan/08-decisiones-tecnicas.md`.

**Reordenamiento de documentación** (2026-07-29): `08-decisiones-tecnicas.md` ya pasó las
1000 líneas — a pedido del usuario, se sacó todo lo que era "lista de qué falta" (estaba
mezclado entre las decisiones técnicas, difícil de escanear) a un documento nuevo,
`Plan/10-pendientes.md`, organizado en 4 categorías: bloqueado por backend, piezas de UI
que quedaron afuera de una fase "completa", fases sin empezar, y deuda técnica. Cuando algo
de esa lista se construye, se borra de ahí — el "por qué se hizo así" sigue registrándose en
`08-decisiones-tecnicas.md` como siempre.

**Fechas importantes construido completo** (2026-07-29): era la única de las 3 opciones del
menú `+` (Nota, Fecha importante, Cita) que ya tenía modelo de dominio y entidad Room desde
la base de Fase 0.1 — se completó repositorio, casos de uso, notificaciones y pantallas,
siguiendo el mismo patrón de Medicamento/Cita. Novedad técnica: primera actividad con
recurrencia semanal/anual (no diaria) — `RecordatorioScheduler` nunca muta `fechaBase`,
siempre recalcula la próxima ocurrencia desde el original. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Notas construido completo** (2026-07-29): última de las 3 opciones del menú `+` sin flujo
real. Escribir/sobrescribir, copiar y compartir (WhatsApp u otro) para notas de texto libre.
Por ahora solo texto — el usuario preguntó por un modo de dibujo tipo pizarra/Paint y se
decidió dejarlo como función futura aparte (ver `Plan/10-pendientes.md`), en vez de meterlo
en esta ronda. `LulaDatabase` sube a versión 10. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Calendario construido completo** (2026-07-29): vista Día/Semana/Mes intercambiable "tipo
Google Calendar" (a pedido explícito del usuario, que rechazó la alternativa más simple de
una sola vista de Agenda), con grilla mensual completa y navegación ◀/▶/Hoy. Agrupa TODO lo
programado — Hábitos, Tareas, Medicamentos, Citas, Fechas importantes — en un solo lugar,
agregando cada tipo una sola vez por rango visible (no una consulta por día). Entrada desde
Hoy siempre visible (a diferencia de otros enlaces "Ver mis X", que solo aparecen si ya hay
algo). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Diario construido completo** (2026-07-29): última función entera que quedaba en
`Plan/10-pendientes.md`. A diferencia de Notas, vive detrás de Zona Privada (PIN/biometría) —
nueva entrada "📓 Diario" en el menú "⋮". Formulario con título opcional, selector de área de
vida (primera pantalla que expone esa tabla, sembrada con 7 áreas desde el primer arranque),
fecha editable con el mismo `DatePicker` de Fecha importante/Cita, y texto libre con dictado.
Sin fotos por ahora (el campo ya existe en el modelo, falta la UI de cámara/galería — ver
`Plan/10-pendientes.md`). `LulaDatabase` sube a versión 11. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Lote de 6 pendientes de la sección 2** (2026-07-29): a elección del usuario, de la lista de
`Plan/10-pendientes.md`. (1) Notas ahora vive detrás de Zona Privada, igual que Diario/
Finanzas. (2) Tarea puntual muestra "Completada el {fecha}" (columna nueva
`fechaCompletado`, `LulaDatabase` sube a versión 12). (3) Botón "🤝 Compartir seguimiento" en
Rutina y Meta (ya estaba en Hábito/Tarea/Medicamento). (4) Revisión semanal solo se activa el
día configurado en Ajustes (por defecto domingo) — antes se podía editar cualquier día. (5)
Zona Privada se re-bloquea sola tras ~3 min en segundo plano (nueva dependencia
`lifecycle-process`). (6) Metas ahora se pueden editar (nombre, área de vida, fecha límite) —
`comoSeMide` queda fijo una vez creada. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Diario: orden, calendario y "+"** (2026-07-29): 3 ajustes a pedido del usuario tras probar
Diario. Ya ordenaba por fecha (no por cuándo se guardó), se agregó desempate estable para el
mismo día. Nueva pantalla `DiaryCalendarScreen` — grilla mensual propia del Diario (no la
comparte con el Calendario general), marca días con entrada (📝) vs vacíos, tocar un día
abre esa entrada o crea una nueva ya fechada ese día. "📓 Diario" ahora también está en el
menú "+" (antes solo en "⋮"), pero sigue pidiendo la clave de Zona Privada igual que siempre.
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**4 pendientes más de la sección 2** (2026-07-29): (1) Finanzas ahora tiene historial
navegable mes a mes (nuevo `FinancesHistoryViewModel`; "Este mes" en la pantalla principal
sigue fijo al mes en curso). (2) Ajustes avisa si falta permiso de notificaciones o de alarmas
exactas, con atajo directo a Ajustes del sistema. (3) Metas "por monto" ahora se calculan
solas sumando lo registrado en Finanzas con categoría "Ahorro" (ya no hay que agregarlo a
mano). (4) Listas ahora guarda una foto de cada uso al tocar "Reiniciar lista" — nueva
pantalla "📜 Ver historial de usos anteriores", `LulaDatabase` sube a versión 13. Detalle
completo en `Plan/08-decisiones-tecnicas.md`.

**Sonidos propios de Lula** (2026-07-29): los recordatorios de nivel Sonido y Alarma ahora
suenan con dos `.wav` propios del usuario (`res/raw/lula_mensaje`,
`res/raw/lula_alarma_gorrion_habla_ventana`) en vez del tono del sistema. Los canales de
notificación llevan sufijo `_v2` para que dispositivos que ya habían creado los canales viejos
(con tono de sistema) reciban el sonido nuevo — Android nunca deja cambiar el sonido de un
canal ya creado. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Nivel Alarma en loop** (2026-07-29): a pedido del usuario ("es alarma no mensaje nomás"), la
alarma ahora suena en loop hasta detenerla, no un disparo único. Nuevo `AlarmaSonidoService`
(foreground service con `MediaPlayer` propio, ya que un `NotificationChannel` solo puede
reproducir su sonido una vez) — se corta con el botón "🔕 Detener alarma", tocando la
notificación para abrir la app, o deslizándola. Nuevo permiso
`FOREGROUND_SERVICE_MEDIA_PLAYBACK` en el manifiesto. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Últimos 4 de la sección 2** (2026-07-30): (1) Cita ahora tiene su propia pantalla de
detalle (editar/eliminar/🤝 Compartir seguimiento), igual que Medicamento/Tarea/Rutina/Meta.
(2) Barra inferior personalizable en Ajustes — 3 filas de chips, una por posición, con las 7
opciones de `02-pantallas.md`. (3) Nueva pantalla "📊 Progreso" (Cumplimiento, Racha máxima,
Constancia 30 días, Puntos esta semana + acceso a la Revisión semanal completa) — nueva puerta
de entrada, Historial y Revisión semanal siguen intactas. (4) Diario permitió adjuntar fotos
por unas horas — **revertido el mismo día** (ver siguiente entrada). Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Enfoque del producto: solo texto y tablas, sin multimedia** (2026-07-30): el usuario se
tomó una pausa para pensar el rumbo general y decidió que Lula no necesita fotos ni dibujo —
ni ahora ni "para más adelante" sin pedirlo de nuevo — para cumplir su propósito central
(ayudar a construir hábitos, ahorrar y llegar a metas). Motivo: una vez que exista sync real a
la nube, imágenes complican mucho más el almacenamiento/administración que texto. Se revirtió
por completo "Diario: adjuntar fotos" (Coil afuera, `ImagenUtils.kt` borrado, las 2 pantallas y
2 casos de uso vuelven a su forma sin `fotos`) — el campo `fotos`/`fotosJson` del modelo queda
igual que antes de esa ronda (siempre vacío, sin UI que lo llene). Esta es una decisión de
producto duradera, no solo del día: al evaluar qué construir de `Plan/10-pendientes.md` de acá
en adelante, priorizar lo que refuerza hábitos/ahorro/metas, no multimedia. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Mi perfil — horarios de comida** (2026-07-30): nueva pantalla dedicada (menú "⋮" → "🧑 Mi
perfil", sección CUENTA de `02-pantallas.md`) para editar Desayuno/Almuerzo/Cena fuera del
flujo de crear un medicamento — antes solo se podían fijar la primera vez, de paso, mientras se
creaba un medicamento "según las comidas". Toda la lógica de guardado ya existía
(`ActualizarHorariosComidaUseCase`); la pantalla solo la reutiliza. Nombre/correo se muestran
de solo lectura (todavía no hay onboarding real). Cierra el punto de Fase 0.8 en la sección 2.

**Tarea vinculada a Medicamento/Cita, con cierre automático** (2026-07-30): a pedido del
usuario ("cuidar a alguien por un tiempo"), una Tarea ahora puede vincularse a un Medicamento o
Cita al crearla/editarla (mismo patrón que Meta↔Hábito: campo `actividadVinculadaId` en
`tarea_detalle`, FK con `SET_NULL` — si se borra el Medicamento/Cita, la Tarea no desaparece,
solo pierde el vínculo). Se muestra en ambos sentidos: la Tarea dice "🔗 Vinculada a: X", y el
Medicamento/Cita lista sus "📝 Tareas vinculadas". Cuando el vínculo ya cumplió su ciclo de vida
(Cita: ya pasó su fecha; Medicamento: ya pasó `fechaFin`), la Tarea se marca sola como
completada — `CerrarTareasVinculadasVencidasUseCase`, corrido cada vez que se abre Hoy, sin
tarea en segundo plano. `LulaDatabase` versión 13→14.

**Hoy: pendientes primero, completados aparte** (2026-07-30): a pedido del usuario, Hoy separa
lo pendiente (se queda arriba, agrupado por momento del día como antes) de lo ya hecho (hábitos,
tareas y tomas de medicamento), que ahora vive en una sola sección "✅ Ya hechos hoy (n)" al
final, colapsada por defecto. Nada se oculta para siempre ni se castiga (`Plan/CLAUDE.md`): se
puede expandir y desmarcar, y al desmarcar algo vuelve arriba, a su sección de pendientes — es
un cambio puramente de visualización en `HomeScreen.kt`, `HomeViewModel`/`HomeUiState` no
cambiaron. Para ver otros días (ayer, mañana) se usa el Calendario que ya existía (Vista
Día/Semana/Mes con ◀/▶/Hoy) — a propósito no se agregó una pestaña "mañana" nueva, para no
duplicar esa función.

**Fase 1.5 — base local de Familia/Equipo** (2026-07-30): siguiendo el mismo criterio que
"Mi círculo de cuidado" (construir solo lo que es 100% mío, sin inventar una segunda persona),
se agregó: crear un espacio Familia (vos como único admin), un selector de espacio (Personal/
Familia) que ahora sí conmuta qué datos ve toda la app — Hoy, Tareas, Finanzas, Calendario se
re-escopean solos porque ya usaban `ObtenerSesionActualUseCase` para resolver `espacioId` — y
Retos familiares (crear, ver progreso "x de y cumplieron hoy", marcar el propio cumplimiento).
Hoy muestra "👨‍👩‍👧 Hoy en {espacio}" cuando el espacio activo no es Personal. Queda
bloqueado, igual que "Personas que acompaño" en Círculo de cuidado: invitar a alguien de
verdad, que aparezca un segundo miembro real, roles admin/miembro con sentido, y progreso de
un reto con más de un participante real — todo eso necesita cuentas/sync que todavía no
existen. `LulaDatabase` versión 14→15. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Ronda de feedback tras probar Fase 1.5 en dispositivo real** (2026-07-30), 7 puntos:
(1) el espacio activo ahora vive solo en memoria (antes en disco) — cerrar y abrir la app
vuelve a Personal solo, en vez de quedarse en Familia y hacer pensar que los datos se
borraron. (2) Banda de color propio ("👨‍👩‍👧 Estás en {espacio}") movida de Hoy a
`LulaTopBar`, visible en cualquier pantalla, no solo Hoy — antes el usuario se "perdía" al
cambiar de pantalla sin ningún indicio de seguir en Familia. (3) Notas y Diario ahora siempre
usan el espacio Personal (`SesionActual.espacioPersonalId`, nuevo), nunca el espacio activo —
son privados por naturaleza (Zona Privada), no tiene sentido que "sigan" a Familia. (4) Espacio
Familia: ahora se puede renombrar y eliminar (borrado en cascada real — ver
`08-decisiones-tecnicas.md`). (5) Hoy Personal muestra un aviso "tienes N pendientes en tu
espacio Familia" si corresponde, para no tener que adivinar ni cambiar de espacio solo para
mirar. (6) Fecha límite ahora visible en la lista de Tareas y su detalle, y en la lista de
Metas — antes solo se veía dentro del formulario de editar. (7) Hoy ahora muestra Citas y
Fechas importantes programadas para hoy (reusando `ObtenerAgendaDelRangoUseCase`, el mismo
motor del Calendario) — antes no aparecían ahí aunque fueran justo ese día. Aparte: Revisión
semanal vuelve sola a la pantalla anterior al guardar, en vez de quedarse sin ningún aviso de
que se guardó. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Roadmap actualizado + Notas con título/orden + deuda técnica + Mi propósito** (2026-07-30):
(1) `04-roadmap-fases.md` ya reflejaba Fase 1.5 con su spec original sin actualizar — se
actualizó con el estado real (base local construida, qué falta y por qué), mismo estilo que
Fase 1.0. (2) Notas ganó título opcional (antes solo se derivaba de la primera línea) y
reordenar con flechas ▲▼ (a propósito no arrastrar/soltar — el usuario pidió lo más simple y
entendible posible). (3) Deuda técnica: se reemplazó `fallbackToDestructiveMigration` por
migraciones reales de Room de acá en adelante (no retroactivas a versiones 1-15, sin datos
reales de nadie que proteger), y un test (`AuditLoggerSerializableTest`) que falla si una
entidad usada con `AuditLogger.registrar<T>()` no tiene `@Serializable`, escaneando el código
fuente en vez de depender de reflection. (4) **Mi propósito** (nuevo, alcanzable desde Mi
perfil): 13 preguntas guiadas para armar Misión/Visión/Propósito de a poco, en dos secciones
("Mi propósito" con 7 preguntas de reflexión, "Mi visión" con 6 más el consejo de redactar en
presente/afirmativo/"yo") — cada una editable por separado, sin necesidad de completarlas
todas de una vez. `LulaDatabase` versión 15→17 (16: Notas, 17: `proposito_personal`). Detalle
completo en `Plan/08-decisiones-tecnicas.md`.

**Bug real: la app no abría después de las migraciones** (2026-08-01): el usuario reportó
pantalla en blanco y cierre inmediato al abrir, incluso reinstalando desde cero. Diagnosticado
con `adb logcat` en su dispositivo real (no adivinado): `IllegalArgumentException`, Room
rechazaba la configuración porque `fallbackToDestructiveMigrationFrom(1..15)` incluía la
versión 15, la misma versión de inicio de `MIGRATION_15_16` — Room no permite que un número de
versión esté en las dos listas a la vez, y truena en tiempo de ejecución, no al compilar. Se
corrigió el rango a `1..14` (el límite siempre debe terminar un número antes de la versión de
inicio de la migración real más vieja). Verificado con la app instalada de verdad en el
dispositivo del usuario vía `adb`, no solo compilado — confirmado que abre sin crashear.

**Mi propósito — borrable** (2026-08-01): faltaba lo de "borrable" acordado el 2026-07-30. Se
agregó borrar una respuesta puntual (botón "Borrar" en la pantalla de esa pregunta, solo
visible si ya tiene respuesta) y borrar todo el propósito de una (botón en la lista principal,
solo visible si hay al menos una respuesta) — ambos con `ConfirmarEliminarDialog`, mismo patrón
que el resto de la app.

**Mi propósito — preguntas corregidas + Metas ganó su ayuda + tabla de planes** (2026-08-01):
el usuario corrigió el mapeo de preguntas que había quedado mezclado. (1) Las 7 preguntas
"personales" (apasiona, valores, habilidades, qué quiero lograr, tipo de vida, impacto, feliz)
arman Misión y Visión **juntas**, no separadas en dos secciones como había quedado. Se sumó una
pregunta nueva y directa de Propósito ("¿Cuál siento que es mi propósito de vida?"), porque las
7 solas apuntan a autoconocimiento, no necesariamente al "para qué". (2) Las 6 preguntas de
"armar objetivos" (qué quiero hacer/ser/ver/tener, adónde ir, qué deseo compartir) y los 3
consejos de redacción (presente, afirmativo, "yo") **no eran de Mi propósito** — son ayuda para
definir una Meta. Se movieron a `CrearMetaScreen` como una sección colapsable "💡 ¿No sabes
cómo definirla? Ver ideas", texto de referencia nomás, no se guarda nada. (3) Se agregó el
botón "🤖 Armar y presentar con IA" en Mi propósito, **deshabilitado a propósito**
("próximamente") — todavía no hay ningún llamado de red en la app (decisión desde Fase 0.1) ni
workflow de n8n del otro lado; el usuario prefiere terminar de estabilizar el modelo de datos
primero, para armar n8n una sola vez contra un esquema ya firme. (4) `05-modelo-negocio.md`
pasó de lista con viñetas a una tabla comparativa Gratis/Premium Individual/Premium Familia,
con un límite pensado específicamente para el costo de IA: "armar y presentar con IA" da 1-2
usos gratis y después pide Premium (a diferencia del resto de "Mi propósito", que es solo
texto guardado, sin costo, y sigue ilimitado gratis). Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Cuentas y conexiones — documento de diseño** (2026-08-01): nuevo `Plan/11-cuentas-y-
conexiones.md`, lo que el usuario llamó "usuarios pendientes" — pidió específicamente diseñar
esto antes de tocar código, con Claude aportando mirada de programador, Play Store, marketing y
psicología. Cubre: qué datos personales agrega `Usuario` (mayoría de edad vía checkbox, no
fecha exacta; consentimiento de Términos; consentimiento de datos de salud separado del
genérico, por ser categoría sensible en Play Store), tabla nueva `Conexion` (recuerda quién
quedó conectado con quién después de aceptar una `SolicitudCompartir`, para que Círculo de
cuidado/Familia puedan mostrar personas y no solo elementos sueltos compartidos), por qué las
preguntas de onboarding se quedan como están (no agregar más — el enganche viene de la victoria
rápida del primer día, no de más preguntas) y un puente sutil hacia Mi propósito para después,
y un checklist de Play Store (Data Safety, eliminar cuenta desde la app, políticas publicadas).
Define el orden de implementación: ampliar `Usuario` → tabla `Conexion` → pantalla "Eliminar mi
cuenta" → borradores de política/términos → recién ahí evaluar Firebase real.

**Cuentas y conexiones — pasos 1 a 3 construidos y verificados en dispositivo real** (2026-08-01):
siguiendo el orden de `Plan/11-cuentas-y-conexiones.md`, se construyó toda la parte local-first
de ese documento. `Usuario` ganó `confirmoMayorDe13`/`terminosAceptadosEn`/
`consentimientoDatosSaludEn` (`MIGRATION_17_18`, versión 18); tabla `Conexion` nueva (sin FK a
`usuario` a propósito, `usuarioB` va a ser casi siempre otra persona); `ProfileScreen` ganó una
sección "🔒 Privacidad y legal" (checkbox + 2 consentimientos con botón "Aceptar") y una "⚠️ Zona
de peligro" con "🗑️ Eliminar mi cuenta" (`clearAllTables()` + `reiniciarApp()` nuevo, que mata el
proceso y relanza para que no queden ViewModels con ids cacheados de filas ya borradas).
Verificado con `installDebug` + `adb logcat` sin crash, y los 3 consentimientos confirmados
persistentes tras `am force-stop` + relanzar. El flujo de borrado en sí se probó solo hasta el
diálogo de confirmación (se canceló a propósito para no perder datos reales del dispositivo de
prueba) — pendiente probarlo de punta a punta con datos descartables. Quedan pendientes los
pasos 4-5 del documento (textos legales, evaluar Firebase), ver `Plan/10-pendientes.md`. Detalle
completo en `Plan/08-decisiones-tecnicas.md`.

**Cuentas y conexiones — paso 4: textos legales redactados y con pantalla propia** (2026-08-01):
`domain/legal/TextosLegales.kt` nuevo, con el borrador completo de Política de Privacidad,
Términos de Servicio, y un texto corto de consentimiento de Datos de salud (los tres citan la
Ley N° 29733, con placeholders entre corchetes para la identidad legal real — falta revisión
legal antes de publicar). Nueva pantalla `LegalTextScreen` (ruta `texto_legal/{tipo}`) para leer
cada documento completo, con el botón "Aceptar" ahí mismo (Términos, Datos de salud) en vez del
atajo que aceptaba sin mostrar texto que se había construido antes — ese atajo se sacó de
`ProfileScreen`/`ProfileViewModel`, la lógica de aceptar quedó en un solo lugar
(`LegalTextViewModel`). Cada fila de consentimiento en "Mi perfil" ahora es clickeable entera y
navega a leer el documento ("Leer y aceptar →" / "Ver →"). Compiló e instaló sin errores; la
verificación visual completa en el dispositivo quedó a medias porque el celular de prueba pasó a
uso normal a mitad de la sesión — sin riesgo grande porque no hay migración de Room de por medio
(la versión de la base se quedó en 18). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Firebase Auth + sync — documento de diseño, paso 5** (2026-08-01): el usuario confirmó seguir
con login real, recuperación de cuenta y que Familia/Círculo de Cuidado funcionen entre personas
de verdad, usando **Firebase (Auth + Firestore)** — nuevo `Plan/12-firebase-auth-y-sync.md`.
Decisión clave: el límite de qué sincroniza es `Espacio.tipo` — los Espacios `PERSONAL` (diario,
Mi propósito, finanzas, hábitos) nunca tocan Firestore, solo `Conexion`/`SolicitudCompartir` y
el contenido de Espacios `FAMILIA` suben, porque son justamente lo que necesita más de un
dispositivo para tener sentido. El usuario semilla no se reemplaza, se "reclama" (mismo `id`
local, se le agrega correo/login real) para no romper ningún FK. Bloqueado ahora mismo: crear el
proyecto de Firebase requiere la cuenta de Google del usuario, Claude no puede hacerlo — se le
pasaron los pasos exactos en la consola (sección 7 del documento) y queda a la espera del
`google-services.json` antes de escribir código de conexión real.

**Ronda de feedback de uso real — 5 puntos** (2026-08-05): Finanzas ganó selector de fecha
(antes solo registraba "ahora", sin poder cargar un gasto de un día anterior). Medicamentos
ganó "Cantidad de dosis" como alternativa a elegir fecha de fin a mano — causa raíz real
encontrada: los horarios se repiten completos cada día entre `fechaInicio`/`fechaFin`, así que
elegir manualmente el día donde cae la última dosis también traía de más las tomas de ese
mismo día (el ejemplo del usuario: 4 dosis recetadas terminaban contándose como 6). De paso se
encontró y arregló un bug real en `RecordatorioReceiver`: reprogramaba la alarma de un
Medicamento para siempre, nunca revisaba `fechaFin` ni si estaba pausado. Diario perdió
"Título" y "Área de vida" — el usuario no les encontró sentido ("tiene un título pero es un
diario"), ahora es solo fecha + texto libre, como un cuaderno. Para el sonido de Alarma/Sonido
que a veces no suena o se corta: se encontraron y corrigieron dos huecos reales en
`AlarmaSonidoService` (errores del `MediaPlayer` que se tragaban en silencio sin loguear nada,
sin pedir audio focus) y se agregó un atajo nuevo en Ajustes para excluir a Lula de la
optimización de batería del fabricante (hipótesis más probable para el patrón reportado en un
Motorola) — pero **no se pudo confirmar la causa exacta con un `logcat` en vivo** porque no
coincidió ningún recordatorio real disparándose durante la sesión; queda pendiente repetir la
prueba la próxima vez que suene un recordatorio de verdad. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Segunda ronda de feedback de uso real — 6 puntos** (2026-08-05): incluyó un bug real de
pérdida de datos — "Cerrar día" nunca cargaba las respuestas ya guardadas de hoy al reabrirlo,
así que "Actualizar cierre del día" partía en blanco y guardar sin escribir nada borraba lo que
ya había (arreglado: ahora precarga desde `RegistroDiario` existente). También se encontró la
misma causa raíz del bug de dosis de la ronda anterior en un SEGUNDO lugar
(`ObtenerAgendaDelRangoUseCase`/Calendario) que el fix anterior no había cubierto — recordatorio
de que un mismo cálculo vivía duplicado en más de un sitio. Se centralizaron dos criterios que
estaban divergiendo entre pantallas: qué cuenta para "Progreso de hoy"/"Cerrar día"
(`actividadCuentaParaHoy`, ahora Hábito+Tarea-de-hoy+Cita-de-hoy en los dos lugares) y cómo se
calcula el progreso de una Meta (`ObtenerMetasConProgresoUseCase`, usado por Hoy y por "Tus
metas"). Citas ganaron un botón para marcarse cumplidas (reutilizando `MarcarActividadUseCase`,
ya existía para Hábito/Tarea). Metas ahora se ven en Hoy con su barra de progreso, antes solo
había un enlace sin números. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Metas — urgencia por fecha límite + reconocimiento de hitos** (2026-08-05): a pedido del
usuario ("con criterio de experto en hábitos, cómo hacemos que esto motive de verdad"), se
agregaron dos piezas a Hoy — nunca pantallas nuevas: (1) la fila de una Meta se resalta con
"⏳ Faltan N días" solo en la última semana antes de `fechaLimite` (o si ya venció), nunca antes,
para no volverla presión constante; (2) al cruzar 25/50/75/100% del objetivo, una tarjeta breve
y positiva ("🎉 ¡Vas al 50%!") con un botón "Genial" — nuevo campo `Meta.ultimoHitoCelebrado`
(`MIGRATION_19_20`, versión 20) para que no se repita. Se cuidó que editar una meta no resetee
el hito ya celebrado (`ActualizarMetaUseCase` ahora preserva ese valor en vez de reconstruir la
`Meta` desde cero). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Tercera ronda de feedback de uso real — 7 puntos** (2026-08-06): otro bug real confirmado —
una Tarea sin fecha límite, una vez completada, se quedaba apareciendo como "hecha hoy" para
siempre (no se usaba `Actividad.fechaCompletado`, que ya existía justo para esto). Corregido en
el mismo criterio compartido de la ronda anterior (`actividadCuentaParaHoy`). También: Citas
ahora muestran su estado en Hoy y ganaron un botón "No se cumplió" (`OMITIDO`, no es un fallo);
Hoy y Calendario comparten un solo mapeo de emojis por tipo/estado (antes duplicado); Metas ya
completadas dejan de aparecer en Hoy; la lista de Metas ordena pendientes primero; los ítems de
Citas/Fechas importantes vencidos sin marcar se resaltan en rojo. El usuario mostró capturas de
otra app con un patrón de formulario más compacto (filas colapsadas que se expanden al tocar) —
se le dio opinión honesta de que es mejor pero es un rediseño grande, no se construyó nada
todavía, queda pendiente elegir una pantalla piloto. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Piloto de formulario compacto — Crear Medicamento construido y verificado** (2026-08-06): el
usuario eligió empezar el rediseño de formularios (punto anterior) por Crear Medicamento. Los
bloques que antes estaban siempre desplegados (Frecuencia, Termina, Recordatorio) pasaron a
filas colapsadas (`core/ui/SelectorRow.kt`, nuevo, reutilizable) que abren un `ModalBottomSheet`
al tocarlas — mismo componente que ya usaba `AddMenuSheet` para el menú "+". Verificado en el
dispositivo real (no solo compilado): se instaló, se abrió la pantalla, se probó el sheet de
Frecuencia con su `TimePicker` anidado, y se confirmó que la fila colapsada se actualiza en vivo
sin cerrar el sheet. Queda a decidir con el usuario si se replica en Crear Tarea/Hábito/Cita.
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Formulario compacto replicado a Tarea, Hábito, Cita, Fecha importante** (2026-08-06): el
usuario confirmó que le gustó el piloto de Medicamento y pidió aplicarlo a todas. Se replicó el
mismo patrón (`SelectorRow` + `ModalBottomSheet`) en las 4 pantallas que tenían un clúster de
configuración que valía la pena compactar — Fecha+recordatorio en Tarea, Duración+Recordatorio
en Hábito, Recordatorios (multi-anticipación) en Cita, Recordatorio en Fecha importante.
Deliberadamente no se tocó Lista/Rutina/Meta/Movimiento — son pantallas simples donde el patrón
no aporta. Verificado visualmente en 3 de las 4 (Tarea, Hábito, Cita); Fecha importante quedó
sin captura porque el celular pasó a uso normal a mitad de la verificación, pero comparte
exactamente el mismo código y compiló sin errores. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Cuarta ronda de feedback — recordatorio persistente de Medicamentos + Citas recurrentes**
(2026-08-06): el usuario reportó, con un caso real ("son las 17:00 y esa toma debió ser a las
14:00"), que faltaba insistencia en los recordatorios de Medicamento — ahora las tomas vencidas
sin marcar se pintan en rojo en Hoy (mismo criterio que ya tenían Citas/Fechas importantes), y
cada medicamento puede configurarse como "persistente" (insiste cada N minutos hasta que se
marque la toma o termine el día — el usuario eligió explícitamente ese límite). También describió
dos casos de cuidado reales — radioterapia (solo días laborables, 20 dosis) y masajes
(lunes/miércoles/viernes por 2 meses, después baja a 2x/semana y luego a 1x/semana) — que pidió
modelar en Cita. Rechazó la alternativa simple ("generar 20 citas sueltas") a favor de un curso
único con sesiones reprogramables individualmente, y dejó a criterio de esta sesión cómo resolver
la reprogramación: se decidió que mover una sesión solo afecta a esa sesión, nunca al resto del
programa. Cita ganó modo "curso" (`esCurso`, `diasSemana`, `horaSesion`,
`cantidadSesionesTotal` — todos opcionales, no afectan citas puntuales existentes) y una tabla
nueva `sesion_cita` (una fila por sesión, mismo principio que `toma_medicamento`), con progreso
("van 9 de 20") visible en el detalle de la Cita y en Hoy/Calendario (que ya comparten una sola
fuente, `ObtenerAgendaDelRangoUseCase`, así que no hubo que duplicar la lógica de conteo). Un
curso sin cantidad fija (masajes) se auto-extiende solo cuando le queda poco margen, sin que el
usuario tenga que volver a tocarlo. `LulaDatabase` sube a versión 22. Compiló sin errores
(`compileDebugKotlin`, `EXIT_CODE=0`); no se alcanzó a verificar visualmente en dispositivo real
porque no había ningún celular conectado (`adb devices` vacío) al terminar esta sesión — queda
pendiente instalar y probar el flujo completo (crear un curso, marcar/reprogramar sesiones) en
cuanto el dispositivo esté disponible. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Quinta ronda — rediseño de Hábitos, vencidos en Hoy, protección "salir sin guardar"**
(2026-08-06): el usuario trajo una propuesta de rediseño de Hábitos armada con otro chat de
Claude — se construyó lo que tenía definición clara: tarjetas (antes filas planas) con ícono
automático por palabra clave, racha propia por hábito (antes solo había una racha global que no
cuadraba con los círculos de abajo — ahora cada tarjeta calcula la suya del mismo historial),
letras de día reales sobre los círculos con "hoy" resaltado, agrupado por Mañana/Tarde/Noche, y
un mensaje motivacional que nunca es de reproche ni con 0% de la semana. "Constancia %" quedó
afuera — ya existe un concepto distinto con ese nombre en Progreso y el usuario mismo dudaba de
cómo debía calcularse acá. Se investigó el chip "💰 S/0" que el usuario reportó en cero después
de haber registrado dinero — no es un bug: es reactivo de verdad y muestra gastos de HOY
(egresos), no un total acumulado ni ingresos; queda pendiente confirmar con el usuario si
debería mostrar otra cosa. Tarea (y Hábito) vencidos sin marcar ahora se pintan en rojo en Hoy,
mismo criterio que ya tenían Cita/Fecha importante/Medicamento (antes no los cubría). El pedido
más importante de esta ronda: el usuario perdió una Tarea completa porque tocó "Listo" en un
sheet (que solo cierra el sheet) y salió pensando que ya había guardado — se construyó
`core/ui/DescartarCambiosAlSalir.kt` (primer uso de `BackHandler` en todo el código) y se
replicó, a pedido explícito, en las 10 pantallas "Crear X" de la app. Compiló sin errores
(`compileDebugKotlin`, `EXIT_CODE=0`); no se alcanzó a instalar en dispositivo real por falta
de conexión (`adb devices` vacío). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Sexta ronda — 7 puntos de uso real** (2026-08-07): teclado tapando el campo "¿Cuántas
sesiones?" en varios sheets largos (faltaba scroll + `imePadding`); una Cita de curso
desaparecía de "Mi salud" apenas empezaba porque se filtraba por la fecha de la primera sesión
nada más, ahora usa las sesiones reales y muestra "Van X de Y"; en el detalle de una Cita de
curso, las sesiones ganaron color propio por estado y un botón "Deshacer" (antes, marcar una
por error no se podía corregir); en Tareas, pendientes y hechas ahora están en secciones
separadas con encabezado (antes se mezclaban sin orden); "Descartar cambios al salir" (ronda
anterior) solo cubría creación — el usuario probó en modo edición (Metas) y no se activó, se
reemplazó por comparación de snapshot real que cubre los dos casos; se confirmó que la racha
muestra 0 hasta cerrar el día (no es un bug, así se definió desde el principio), y a pedido del
usuario se construyó un recordatorio diario nuevo y configurable ("🔥 Recordarme cerrar mi
día", apagado por defecto) que se salta solo si el día ya se cerró; Finanzas → Historial ganó
resumen de ingresos/gastos/balance (antes solo listaba movimientos sueltos) y un modo de rango
de fechas manual; y se agregó logging alrededor del arranque del servicio de alarma para poder
diagnosticar la próxima vez que "suene un segundo y se corte" (sospecha: exención de batería no
concedida en ese celular). Compiló sin errores (`compileDebugKotlin`, `EXIT_CODE=0`); no se
alcanzó a instalar en dispositivo real por falta de conexión. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Séptima ronda — diagnóstico real de alarmas + 6 puntos más** (2026-08-10): con el
dispositivo conectado, se diagnosticó a fondo (con `adb`/Logcat, no adivinando) por qué una
alarma de Tarea "no sonó nada" — la causa real es que el permiso de notificaciones
(`POST_NOTIFICATIONS`) nunca quedó concedido en ese celular; Android no vuelve a preguntar
después de una negación, así que la app la pedía al abrir pero nunca se enteraba de que seguía
sin concederse. Se agregó un banner nuevo en Hoy que avisa esto de forma proactiva (antes solo
se veía si el usuario entraba por su cuenta a Ajustes) y logging para diagnosticar más rápido
la próxima vez. Además: el Calendario ya no muestra un Hábito en fechas anteriores a cuando se
creó; Listas ganó flechas de reordenar (como Notas); ahora se puede llenar o actualizar el
cierre de un día anterior desde Calendario, no solo el de hoy; una sesión futura de un curso de
Cita ya no muestra el botón de "marcar cumplida" (se veía como una fila de checks verdes
confusa); se sumó un recordatorio configurable por franja del día (mañana/tarde/noche, aparte
del de cerrar el día); y se dio opinión sobre incorporar el método FODA más adelante como
extensión de Mi propósito (documentado, no construido). Compiló sin errores
(`compileDebugKotlin`, `EXIT_CODE=0`); el intento de instalar en el dispositivo real falló
porque el teléfono pasó a estado offline a mitad de la instalación — queda pendiente
confirmar visualmente esta ronda en cuanto el dispositivo vuelva a estar disponible. Detalle
completo en `Plan/08-decisiones-tecnicas.md`.

**Octava ronda — corrección de Listas + etiqueta "Hoy" en Calendario** (2026-08-10): las
flechas de reordenar que se agregaron a Listas en la ronda anterior quedaron en el lugar
equivocado (reordenaban los ítems de adentro de una lista); el usuario aclaró que lo que
quería reordenar eran los títulos de lista. Se movieron ahí (`ListaEntity` gana `orden`,
migración de base de datos incluida) y, adentro de cada lista, los ítems ahora se autoordenan
solos (no marcados adelante, marcados al final) en vez de tener flechas manuales. Además, en
Calendario el texto "Hoy" aparecía debajo de la fecha sin importar el día que se estuviera
viendo (era en realidad un botón fijo de "ir a hoy" con ese texto, confuso porque parecía
describir el día mostrado); ahora la vista Día muestra el nombre real del día ("Martes") o
"Hoy, lunes" solo cuando de verdad es hoy, y el atajo para volver a hoy solo aparece cuando
hace falta (viendo otro día). El usuario probó esto en el teléfono real y encontró un bug
puntual: la primera lista de la pantalla no se reordenaba con la flecha ▲, mientras que las de
más abajo sí — la migración que agregó `orden` a Lista le puso 0 a todas las listas viejas por
igual, así que quedaron empatadas (intercambiar 0 con 0 no cambia nada). Arreglado con una
migración nueva que reparte un `orden` único respetando el orden que ya se veía. Instalado y
confirmado en la ronda siguiente. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Novena ronda — alarma, medicamento y tareas vencidas/completadas (2026-08-11)**: 6 puntos de
uso real más. El sonido del nivel Alarma se cortaba a menos de un segundo con el teléfono
inactivo (confirmado: fabricantes agresivos con batería, sobre todo el Motorola de prueba,
vuelven a dormir la CPU o matan el `Service` a mitad de reproducción) — se agregó un
`WakeLock` mientras suena y un banner proactivo en Hoy avisando si falta la excepción de
optimización de batería (ya existía el atajo en Ajustes, pero nadie lo veía sin entrar por su
cuenta). Un Medicamento cada 8 horas mostraba una toma de más el primer día (antes de la hora
real de inicio) y perdía la última — causa raíz: el cálculo de horarios de un día completo "da
la vuelta a la medianoche" y esa parte no se estaba recortando en el primer día; se centralizó
todo el cálculo en una sola función nueva (`horariosParaFecha`) usada por Calendario, "Tomas de
hoy" y el recordatorio del día siguiente, para que no se vuelva a desalinear en 3 lugares
distintos. Una Tarea vencida de ayer sin hora de recordatorio configurada no se pintaba en rojo
en Hoy (solo miraba la hora, nunca la fecha límite) — ahora también cuenta la fecha. Una Tarea
que se completa días después de vencida ahora se marca en Calendario en el día real en que se
hizo, no en su fecha límite original; y se agregó un checkbox en Calendario (solo en días
pasados) para completar una Tarea atrasada con la fecha de ESE día en vez de "ahora" — a pedido
explícito del usuario, y solo para Tarea (Hábito sigue siendo estrictamente "por día"). Por
último, se confirmó leyendo el código (no adivinando) que Lula ya mantiene el horario fijo
original de un Medicamento después de una toma atrasada, sin recorrerlo — que es el criterio
médico correcto; no hizo falta ningún cambio ahí. Compiló sin errores y se instaló en el
dispositivo real (moto g(9) plus, sin uso activo al momento de instalar). Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Causa real del sonido de Alarma cortado, encontrada con `logcat` en vivo** (2026-08-11, mismo
día): el WakeLock/banner de la Novena ronda no arreglaron el problema — el usuario lo confirmó
probando de nuevo. En vez de seguir agregando mitigaciones a ciegas, se capturó `logcat` en
vivo mientras el usuario reproducía el bug con hora exacta. El log mostró la causa real: el
`fullScreenIntent` de la notificación de Alarma (que Android dispara SOLO, sin que la persona
toque nada, para mostrar la alerta sobre la pantalla bloqueada) reusaba el mismo `PendingIntent`
que "tocar la notificación para detener la alarma" — así que la app se detenía la Alarma a sí
misma casi al instante (~300ms) cada vez que el teléfono estaba bloqueado, que es justo cuándo
Android dispara esa apertura automática (con el teléfono en uso no la dispara, por eso ahí
sonaba bien). Nada que ver con batería, Doze, ni con el sonido personalizado — un bug de lógica
propio. Arreglado dándole al `fullScreenIntent` su propio `PendingIntent`, sin la instrucción de
detener. Compiló e instaló en el dispositivo real; falta que el usuario confirme con la misma
prueba de antes. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Segundo hallazgo encadenado, mismo día**: con ese arreglo instalado, la Alarma ya no se
cortaba sola, pero ahora sonaba sin parar — ni los botones de la pantalla ni la tecla de
encendido la detenían (se la cortó de emergencia con `adb shell am force-stop` para no dejarla
sonando mientras se investigaba). Causa: las pantallas de acción de un recordatorio (Hábito/
Tarea: "Ya lo hice"/"Recuérdame en 15"/"Ver en Hoy"; Medicamento: "Ya la tomé"/"La omito"/"Ver
en Mi salud") solo cancelaban la notificación visual al abrirse, nunca llamaban a
`AlarmaSonidoService.intentDetener` — y "Ver en Mi salud" ni siquiera pasaba por el ViewModel.
Arreglado: se movió el corte de la Alarma a cada una de las 6 acciones reales (no al abrir la
pantalla, eso sería volver al bug anterior). Cita queda afuera a propósito por ahora — no tiene
pantalla de acción dedicada, solo el botón de la notificación, documentado como decisión de
alcance, no bug. Compiló e instaló en el dispositivo real. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Cuarta ronda del día** (2026-08-11): el reporte de que el medicamento de 6 tomas seguía sin
mostrar nada el día 13 resultó ser un dato viejo, no un bug — se confirmó sacando una copia real
de la base del dispositivo (`adb exec-out run-as ... cat databases/lula.db` + `sqlite3` local,
nunca `adb shell` normal para binarios, corrompe el archivo) y viendo que esa actividad puntual
se había creado antes de que el arreglo de horarios quedara instalado, así que su `fechaFin`
sigue guardado con la fórmula vieja; basta con editarla y guardar de nuevo. Aparte, se corrigió
un checkbox real: el "⬜"/"✅" de cada sesión de Cita era decorativo (no hacía nada al tocarlo),
la acción real estaba en un botón aparte — ahora es un `Checkbox` de Material real, tocarlo
marca/desmarca directo. Por último, el usuario pidió opinión (no construcción) sobre rediseñar
Metas para que Hoy no se llene si hay muchas — se le mostró que gran parte de lo que pedía ya
existe (fecha límite opcional, orden por fecha, sección de completadas) y se propuso: filtrar
Hoy a solo metas urgentes, atajos rápidos de fecha, aplazar sin entrar a editar, y contador en
"Completadas". Queda pendiente de que el usuario confirme antes de construir nada de Metas.
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Construcción completa de Metas** (2026-08-11, mismo día): el usuario confirmó el alcance
completo (a diferencia del resto de la ronda, esto sí se pidió construir, no solo opinar) y se
hicieron las 6 partes propuestas. Las 6 preguntas de ayuda para pensar una meta (ya existían
como texto) ahora también etiquetan la meta con una categoría, con ejemplos reales por
categoría al elegirla. Se armó un selector de fecha rápido reutilizable (+1 semana/mes/3 meses/
año, más el calendario de siempre), usado tanto al crear una meta como en un botón nuevo
"🔜 Aplazar" en el detalle. Las metas completadas ahora se agrupan por categoría con su propio
conteo, más un total arriba ("✅ Completadas (8)") — ver varias juntas, con número, refuerza la
sensación de avance, como pidió el usuario. Se agregó un aviso sonoro opcional el día que llega
la fecha límite de una meta (Meta vive en su propia tabla, aparte de Actividad, así que el
recordatorio usa el mismo patrón "bypass" ya usado para cierre de día/franja, no el camino
normal de Hábito/Tarea). Y Hoy ya no muestra todas las metas en progreso sin filtrar — solo las
urgentes (últimos 7 días o vencidas); el resto vive solo en la pestaña Metas, para que no se
llene si hay muchas. Compiló sin errores y se instaló en el dispositivo real (moto g(9) plus,
sin uso activo al momento de instalar). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Rutinas/Tareas sin filtrar + rediseño completo de Metas** (2026-08-12): dos bugs de
crecimiento sin límite — el selector "¿Qué actividades agrupa?" al crear una Rutina traía TODAS
las tareas y hábitos alguna vez creados sin filtrar las ya completadas, y "✅ HECHAS" en Tareas
(además de la vista Matriz, que no filtraba nada) acumulaba el historial entero en vez de solo
lo vigente. Ambos arreglados con filtros por estado/fecha de completado. Aparte, rediseño grande
de Metas a pedido detallado del usuario: Crear Meta pasó de un formulario largo a filas
compactas tipo Medicamento (Categoría es ahora el primer paso obligatorio, fecha límite dejó de
ser opcional, y el aviso pasó de un simple sí/no a nivel completo Silencioso/Sonido/Alarma,
reutilizando la misma lógica de Alarma que el resto de la app); "Ver mis metas" ahora agrupa
TODO por categoría desde el principio (antes eran dos listas separadas) con un contador
"(1/3)" en vez de la barra de progreso, que el usuario dijo que no llamaba la atención; y en Hoy
las metas urgentes ganaron el mismo estilo compacto más un botón directo "🔜 Reprogramar".
Compiló sin errores a la primera pese al tamaño del cambio; pendiente instalar en el dispositivo
real, que se desconectó justo antes. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Causa real de "los recordatorios no suenan al día siguiente"** (2026-08-13): el usuario
reportó que TODOS los tipos de recordatorio dejaban de sonar después de varias horas. Se
diagnosticó en vivo con el dispositivo conectado (no adivinando): las alarmas SÍ estaban bien
programadas en `AlarmManager`, el teléfono llevaba 20 días sin reiniciarse, la app no estaba
"detenida" ni restringida por batería — y una prueba controlada (forzar el modo de reposo más
profundo de Android con `adb` justo antes de la hora de una alarma real) mostró que suena
perfecto incluso en el peor caso de inactividad. El dato que resolvió el caso: el usuario aclaró
que las fallas reales pasan "usando el celular", no con la pantalla dormida. Causa real:
Motorola (confirmado en el dispositivo de prueba) puede "forzar detener" la app en segundo
plano para ahorrar batería, lo que cancela TODAS las alarmas pendientes — igual que un reinicio,
pero sin disparar `BOOT_COMPLETED`, así que `BootReceiver` (la única reparación que existía)
nunca se enteraba. Arreglado: la lógica de "reprogramar todo" se sacó a un caso de uso
compartido nuevo y ahora también se llama cada vez que se abre la app (antes solo al reiniciar
el teléfono) — de paso se encontró que Meta nunca se reprogramaba en absoluto, ya corregido
también. Compiló e instaló en el dispositivo real. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Rediseño de Ajustes en tarjetas** (2026-08-13): el usuario mostró 3 capturas de otras apps
(cuenta de Honor, una app de botica, una app de salud) donde cada sección de ajustes vive en su
propia tarjeta, no en un solo scroll continuo. `SettingsScreen.kt` se reagrupó siguiendo ese
mismo patrón — nuevo `TarjetaAjustes` local, 5 tarjetas: "🔔 Recordatorios y permisos", "🗓️
Revisión y cierre del día", "🔔 Recordarme revisar Lula", "✅ Marcar en Hoy", "🧭 Personalizar mi
navegación" — mismo contenido de antes, solo agrupado visualmente. Compiló e instaló en el
dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Perfil en tarjetas + separar "marcar" de "ver/editar" en Hoy** (2026-08-13): siguiendo con el
mismo pedido de elegancia visual, la tarjeta de Ajustes se hizo compartida
(`core/ui/TarjetaSeccion.kt`) y se reusó en `ProfileScreen.kt`, que ganó una tarjeta nueva "👥 Mi
espacio" con acceso directo a Círculo de cuidado y Familia/Espacios (antes no aparecían en
Perfil para nada). En `HomeScreen.kt` se agregó una flecha "›" a las filas de Citas/Fechas
importantes/Metas (se abren para ver/editar) para distinguirlas de un vistazo de las filas con
checkbox (Hábitos/Tareas/Medicamentos, que se marcan). Compiló e instaló en el dispositivo real.
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Hoy en tarjetas + check verde + resaltado de vencidos** (2026-08-13): tercera ronda del mismo
pedido de elegancia. `HomeScreen.kt` se reagrupó en tarjetas: "✅ Para marcar hoy" (todo con
checkbox), "📌 Metas y agenda de hoy" (todo lo que solo se abre para ver/editar), "✅ Ya hechos
hoy" (ahora tarjeta propia) y "🔎 Explorar más" (los enlaces del final). El checkbox marcado pasó
de violeta a verde (`LulaHabito`, mismo verde de hábitos) y las filas vencidas ahora tienen fondo
resaltado, no solo texto rojo, para ubicar rápido qué falta marcar. Compiló e instaló en el
dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Ícono de Hábito: de "✅" a "🌱"** (2026-08-13): con el checkbox ya verde, dos checks pegados
en la misma fila (el emoji de tipo y el checkbox real) confundían. Se cambió el emoji de Hábito
a "🌱" en los 7 lugares donde vivía (estaba duplicado en vez de estar solo en la función
compartida `emojiTipoActividad()`). Compiló e instaló en el dispositivo real. Detalle completo
en `Plan/08-decisiones-tecnicas.md`.

**Ícono de Tareas en "Personalizar mi navegación" era el de Calendario** (2026-08-13): el chip
de Tareas en Ajustes usaba "📅" (el mismo de Calendario) en vez de "📝" como en el resto de la
app — inconsistencia real, no una decisión nueva, corregida en `OpcionBottomBar.kt`. Compiló e
instaló en el dispositivo real.

**Ícono de Citas: de "📅" a "🩺"** (2026-08-13): mismo motivo que Tareas, Citas también usaba el
símbolo de Calendario. Se cambió a "🩺" en los 4 lugares donde vivía, y de paso se realineó el
ícono de "Círculo de cuidado" en Perfil (que había quedado en "🩺" y ahora chocaba) a "👥" — el
mismo que ya usaba el chip de "Personalizar mi navegación". Compiló e instaló en el dispositivo
real.

**Recordatorios que seguían sonando después de terminados/eliminados** (2026-08-14): dos bugs
reales, diagnosticados leyendo el código, no adivinando. (1) `ReprogramarTodosLosRecordatoriosUseCase`
(el arreglo del 2026-08-13 para "los recordatorios se duermen") reprogramaba TODAS las horas de
un Medicamento activo en cada apertura de la app sin revisar `fechaFin` — un tratamiento ya
terminado volvía a sonar completo. (2) Eliminar un Medicamento no cancelaba su cadena de
"insistir" (recordatorio persistente) si estaba activada. Además del arreglo puntual de ambos,
se agregó una guardia general en `RecordatorioReceiver`: antes de mostrar cualquier
recordatorio, revisa si la actividad todavía existe, está activa, no está ya confirmada, y (si
es Medicamento) sigue vigente hoy — así ninguna vía de cancelación necesita ser perfecta. De
paso, a pedido del usuario, borrar algo ahora deja rastro en Calendario (usando la auditoría
`historial_cambios` que ya existía) en vez de desaparecer sin dejar huella. Compiló y se instaló
en el dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Botón "Hoy" confuso en Historial de Finanzas** (2026-08-14): aparecía siempre debajo del mes,
incluso viendo el mes actual, y confundía. Ahora solo aparece cuando se está viendo otro mes
(mismo criterio que ya usa el Calendario con "Ir a hoy"). Compiló y se instaló en el dispositivo
real.

**Ronda de feedback de uso real, 5 puntos, verificados con datos reales del dispositivo**
(2026-08-14): se sacó la base de datos real (`adb exec-out run-as ... cat databases/lula.db`)
antes de tocar código, para no adivinar. (1-2) Las dos rachas en 0 (global y de Hábito) no son
un bug — ambas cuentan hacia atrás desde HOY y se cortan si hoy todavía no se actuó, aunque haya
racha real en curso desde ayer; no hay ningún tope de "21 días". Quedó como observación para
decidir con el usuario (contradice un poco "ningún intento se castiga"), no se cambió el
comportamiento todavía. (3) El "eliminado" que aparecía sin que el usuario borrara nada era
real, pero de OTRA actividad (una "ampicilina" de prueba eliminada a las 00:01am), no de la que
acababa de crear — confirmado con los IDs y timestamps reales, no encontré el "6:00 am" que
describió en los datos del medicamento nuevo. (4) Bug real arreglado: una Tarea sin fecha límite
completada hoy nunca aparecía en Calendario porque el código salía antes de mirar
`fechaCompletado`. (5) `CitaDetailScreen.kt` (sesiones de un curso de citas) rediseñado: check
verde en vez de morado, la sesión de hoy se resalta con fondo, "Reprogramar" pasó al lado
derecho, menos espacio en blanco entre filas. De paso, Finanzas ganó una sección "Hoy" que
muestra ingresos Y egresos de hoy (antes solo egresos, por eso un ingreso registrado "no
aparecía"). Compiló e instaló en el dispositivo real. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Ronda de 6 puntos de uso real** (2026-08-17): (1) Alarma sin duración configurable — hoy suena
en loop indefinido, quedó como propuesta a confirmar. (2) Aclarado cómo modelar un evento
recurrente-pero-irregular (mercado sábado/domingo, a veces entre semana): Hábito con días
específicos para el caso regular, Tarea puntual para lo irregular — ya soportado, sin código
nuevo. (3) Arreglado: la sección "¿Acompaña a un medicamento o cita?" en Crear Tarea vivía
siempre desplegada y se veía muy cargada — ahora colapsada con el mismo patrón `SelectorRow` que
el resto del formulario. (4) Arreglado: crear un Hábito/Tarea sin nombre fallaba en silencio, sin
avisar — ahora muestra un mensaje. (6) Se evaluó (y se recomendó no adoptar tal cual) una
propuesta de ChatGPT para las 6 preguntas de Metas — no encaja con lo liviano que es hoy ese
selector. Además, diagnóstico completo de un reporte de "no sale mensaje al cerrar el día" —
resultó no ser un bug (racha real confirmada en 7, hito ya celebrado antes, sin crashes en el
log). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Duración máxima de Alarma + ejemplos de las 6 preguntas de Metas** (2026-08-17): nueva opción
en Ajustes "⏱️ Silenciar alarma después de" (1/5/10/15/20/25 min o Nunca, igual que el reloj
nativo de Android) — antes el nivel Alarma sonaba en loop sin límite. De paso, los ejemplos de
las 6 preguntas de Metas (Hacer/Ser/Ver/Tener/Ir/Compartir) pasaron de ser muy específicos de un
perfil emprendedor a ejemplos más universales, mismo estilo "Yo..." de siempre. Compiló e
instaló en el dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Confirmado con captura: el mensaje de cierre de día sí funcionaba** (2026-08-17): 3 días de
diagnóstico terminaron con el usuario mandando una captura donde se ve "Otro día a tu favor."
justo arriba de la racha — nunca hubo bug, el mensaje corto se confundía visualmente con la
píldora de racha de al lado. Además, se completó el punto 4 de la ronda anterior: las 10
pantallas "Crear X" de la app (faltaban Meta, Rutina, Lista, Reto familiar, Movimiento
financiero) ahora avisan con un Toast qué campo obligatorio falta, en vez de fallar en silencio.
Compiló e instaló en el dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Racha en 0 al empezar el día + signo en gastos de la barra superior** (2026-08-15): el usuario
confirmó el problema que había quedado como observación — "ayer tenía 3" y hoy antes de cerrar
el día se veía "0". Ahora, si hoy todavía no se cerró/marcó, la racha (global y por hábito)
arranca a contar desde ayer en vez de desde hoy, así no desaparece hasta que de verdad se rompe.
También se agregó el signo "-" al gasto de hoy en la barra superior. Sobre premios por
persistencia (inspirado en Duolingo) quedó como pregunta abierta, sin implementar. Compiló e
instaló en el dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Hitos de racha: pantalla grande de celebración + mensajes variados** (2026-08-15): construida
la Fase 1 de la estrategia de gamificación guardada en memoria (ver
`project_gamificacion_premios_persistencia`) — sin gemas todavía, a propósito. Nuevo
`core/utils/MensajesRacha.kt` centraliza los mensajes (al azar, varios pools: diario, por hito
7/21/30/60+, y aviso de "casi llegas" cuando falta 1 día). Al cerrar el día y cruzar un hito,
`CerrarDiaScreen.kt` muestra una pantalla completa de celebración (plantita 🌱→🌿→🌳 que crece,
mensaje al azar, botón fijo "Voy a seguir 🌱") en vez de la vista chica normal. Compiló e instaló
en el dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Compartir una Lista como texto plano** (2026-08-15): de 4 formas de compartir una Lista que
planteó el usuario, 2 necesitan cuentas reales (quedan en `10-pendientes.md`) y 1 (copiar vía
QR/enlace) no necesita backend pero tampoco se construyó todavía. La que sí se construyó ahora:
compartir/copiar la lista como texto plano (WhatsApp, correo, lo que sea), botones "📋 Copiar"/
"📤 Compartir" en `ListDetailScreen.kt`, mismo patrón ya usado en Notas. Compiló e instaló en el
dispositivo real. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Firebase Auth + Firestore: integración Gradle inicial** (2026-08-19): primer paso real hacia
cuentas de usuario (Google Sign-In + enlace mágico por correo, sin contraseña clásica todavía),
siguiendo `12-firebase-auth-y-sync.md`. Se conectó el SDK de Firebase al proyecto
(`google-services.json`, BoM de Firebase, `firebase-auth`, `firebase-firestore`, Credential
Manager para Google Sign-In) — todavía sin código de autenticación real, solo la base de Gradle.
Compiló e instaló en el dispositivo real, la app arranca sin crash con Firebase inicializado.
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Firebase Auth: login con Google real + "reclamar cuenta"** (2026-08-19): sobre la base de
Gradle del paso anterior, ahora sí hay login real. `Usuario` gana `firebaseUid` (migración de
Room v26→v27), `AuthRepositoryLocalImpl` se reemplazó por `AuthRepositoryFirebaseImpl`, y nueva
tarjeta "🔑 Cuenta" en Perfil con botón "Continuar con Google" (Credential Manager, no la API
vieja) que vincula la cuenta de Google al usuario semilla que ya existía — mismo `id` local,
ningún FK se toca. Compiló, migración verificada en el dispositivo real (`PRAGMA table_info`). Al primer intento el
botón falló ("No se pudo iniciar sesión con Google") — diagnosticado con logcat en vivo: al
proyecto de Firebase le faltaba la huella SHA-1 del certificado de firma del APK. Con el SHA-1
del debug keystore agregado en Firebase Console, el login funcionó — confirmado con logcat
(`FirebaseAuth: Notifying id token listeners...`) y leyendo la base de datos real del
dispositivo (correo/metodoLogin/firebaseUid quedaron seteados sobre la misma fila de siempre).
Sigue pendiente correo mágico + sync de Firestore, y agregar el SHA-1 de release cuando se
publique. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Círculo de cuidado: aceptar/rechazar real + sync a Firestore + reglas de seguridad**
(2026-08-19): al ir a sincronizar `Conexion`/`SolicitudCompartir` apareció un hueco más grande
de lo esperado — nada en la app aceptaba o rechazaba una solicitud todavía (diseñado en Fase 1.0,
nunca conectado). Se construyó: aceptar/rechazar real (crea la `Conexion`, da acceso en la
actividad si vive en este dispositivo), un bug real corregido de paso (`observarPendientesPara`
filtraba por `usuarioId` en vez de por correo, nunca iba a encontrar nada), y sync completo a
Firestore (`CompartirSyncRepository` nuevo, listener en vivo mientras Círculo de cuidado está
abierta). Nuevo archivo `firestore.rules` en la raíz del repo — reemplaza la regla temporal de
denegar-todo, el usuario debe pegarlo en Firebase Console. Compiló, migración a Room v28
verificada en el dispositivo real, pantalla abierta sin crash con el listener corriendo.
Deliberadamente fuera de esta ronda (documentado en `10-pendientes.md`): cuando aceptas una
solicitud todavía no ves el contenido real del hábito/tarea compartido, solo la solicitud y la
conexión sincronizan — mostrar el detalle real es un paso más grande aparte. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Invitar de verdad a un Espacio Familia** (2026-08-20): antes de sincronizar el contenido de
Familia (paso 5 del plan) apareció el bloqueo real de siempre — no se podía invitar a nadie de
verdad. Se construyó reutilizando toda la infraestructura de `SolicitudCompartir` de Círculo de
cuidado (nuevo `TipoSolicitud` ACTIVIDAD/ESPACIO) en vez de duplicarla: `InvitarAEspacioUseCase`,
`EspacioRepository.agregarMiembro`, `AceptarSolicitudCompartirUseCase` bifurcado por tipo,
formulario real en `FamiliaScreen`. Bug real encontrado y arreglado con logcat en vivo: la app se
cerraba al aceptar una invitación porque `EspacioMiembroEntity` nunca se había marcado
`@Serializable` (nada la auditaba hasta ahora) — corregido, y se reparó a mano el dato que quedó
a medias por el crash. Segundo intento: aceptar una invitación a Familia funcionó de punta a
punta, verificado con la base de datos real del dispositivo (fila nueva en `espacio_miembro`).
Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Compartir por código QR: Listas, "mi código para conectar", botón global de escanear**
(2026-08-20): comparando con Yape, el usuario pidió QR real (no solo generar, también escanear).
Se construyó: compartir una Lista por QR (100% local, sin backend — el escaneo la importa como
copia nueva), "mi código para conectar" en Perfil (QR con el correo, para no escribirlo a mano
al compartir/invitar), y **un solo botón de escanear en la barra superior** (visible en toda la
app) que detecta solo qué tipo de código es, en vez de un botón distinto por pantalla. Escaneo
real vía Code Scanner de Google Play Services (sin permiso de cámara propio). Se aclaró una
diferencia de diseño importante: escanear en persona SÍ puede saltarse "aceptar" para lo 100%
local (Lista), pero conectar personas/Familia mantiene el paso de aceptar de siempre — abrir esa
puerta necesitaría reglas de Firestore nuevas y, aun así, quien invita no se enteraría de nada
hasta que el contenido del Espacio sincronice de verdad (sigue pendiente). Bug real corregido de
paso: el aviso "📩" de la barra superior seguía filtrando por `usuarioId` en vez de correo. Los
íconos de QR pasaron de emoji a íconos reales (`material-icons-extended`, solo para estos dos)
porque el usuario mostró una app de referencia y el emoji no se entendía. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Sync de contenido de Espacio Familia — Tareas y Retos familiares** (2026-08-21): último paso
grande del plan de Firebase. Solo se sincroniza lo que Familia ya ofrece hoy (Tareas del hogar,
Retos familiares) — Hábitos/Medicamentos/Citas se quedan fuera a propósito. Nuevo
`EspacioSyncRepository` (push al crear/actualizar/marcar, listener en vivo mientras el Espacio
Familia sea el activo, alojado en `TopBarStatsViewModel` que es efectivamente global). Reglas de
seguridad reales para `espacios/**` (antes bloqueado por completo) — cada quien solo escribe su
propia membresía, el resto exige ser miembro. Bug real encontrado con logcat: un Espacio Familia
creado en una sesión anterior (antes de que este sync existiera) no tenía membresía en Firestore
y todo fallaba con `PERMISSION_DENIED` — se agregó un respaldo que sube el Espacio + mi
membresía antes de escuchar, así uno viejo también funciona sin recrearlo. Confirmado con
Firebase Console: el documento del espacio, la subcolección `miembros`, y una Tarea nueva con
todos sus campos, todo verificado ahí. Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Respaldo del Espacio Personal — Hábitos y Tareas** (2026-08-21): a raíz de la pregunta "¿qué
pasa si cambio de celular?", se construyó el respaldo real de lo Personal — sin restringirlo a
premium todavía (el cobro no existe aún; se corta cuando llegue, con las mismas reglas de
seguridad). A diferencia de Familia (varias personas, necesita escuchar en vivo), lo Personal es
de un solo dispositivo a la vez — nuevo `PersonalSyncRepository` sube cada cambio y **restaura
una sola vez** (no listener) al vincular la cuenta y en cada apertura de la app. Alcance de esta
ronda: Hábitos (con su historial día a día, la racha) y Tareas — lo que más dolería perder.
Bug real corregido con logcat en vivo: reglas de Firestore nuevas sin publicar causaron
`PERMISSION_DENIED` en el primer intento. Confirmado con Firebase Console: el Hábito con todos
sus campos y su registro de racha, verificados ahí. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.

**Recuperar la cuenta por completo** (2026-08-22): el usuario pidió cerrar el hueco de "qué
pasa si cambio de celular" hasta el final. `PersonalSyncRepository` ganó Rutina, Medicamento (con
tomas), Cita (con sesiones de curso), Fecha importante, y el historial de Cerrar mi día/Revisión
semanal — ya no queda ningún tipo del Espacio Personal sin respaldar. Se restaura también la
membresía a Espacios Familia en un celular nuevo (puntero `misEspacios`, sin necesitar
`collectionGroup` con índice compuesto). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Registro obligatorio + primera prueba real con dos celulares** (2026-08-22/23): se construyó
el registro completo (`Plan/06-onboarding.md`: Bienvenida → Cuenta → Privacidad → 5 preguntas →
Resumen) — la app ya no arranca directo en Hoy con el usuario semilla. Familia ganó QR de
invitación + botón de WhatsApp, y un código de unión de 60 segundos que se renueva solo
("escanear y quedar dentro", sin paso de aceptar — con mitigación real del riesgo de un QR
permanente). Primera vez probando con dos cuentas de Google reales en dos celulares — salieron 3
bugs reales, los 3 arreglados el mismo día: nombre de miembro de Familia mostraba el id local en
vez del nombre real, un canal de notificación de Alarma con un sonido pegado desde Ajustes del
sistema del dispositivo, y contraste de texto en el registro. Se agregó también un ícono de app
que evoluciona solo con el tiempo (semilla → plantita → flor, según antigüedad de cuenta y racha
activa) y se arregló la sincronización del perfil (nombre real y "ya me registré antes" ahora
viajan de verdad entre celulares). Detalle completo en `Plan/08-decisiones-tecnicas.md`.

**Círculo de cuidado: ver el contenido real compartido** (2026-08-23): cierre del hueco más
grande que quedaba pendiente de Firebase — antes, aceptar una solicitud de Círculo de cuidado
solo sincronizaba la "capa social" (quién pidió, quién aceptó), nunca el hábito/tarea/medicamento
en sí. Se construyó con el mismo nivel de detalle que ya tenía Familia (Hábito, Tarea, Rutina,
Medicamento, Cita, Fecha importante) y una pantalla nueva, "Lo que me comparten". Gap relacionado
encontrado de paso, no resuelto: compartir una Meta por Círculo de cuidado sigue roto (Meta no
vive en la tabla `Actividad`). Detalle completo en `Plan/08-decisiones-tecnicas.md`,
`Plan/10-pendientes.md`.

**Quitar/salir de Familia, dejar de ver algo compartido, y sync al editar** (2026-08-23): tres
preguntas del usuario tras probar lo anterior destaparon huecos reales. Ahora un admin puede
quitar a un miembro de Familia y cualquiera puede salir por su cuenta (`EspacioMiembro` ganó
`firebaseUid`, `MIGRATION_31_32`, para saber qué documento borrar en Firestore); el destinatario
de algo compartido por Círculo de cuidado puede "Dejar de ver esto" sin depender de que quien
comparte revoque primero; y se confirmó que "Revocar acceso" ya borraba de verdad en Firebase.
De paso se encontró y arregló que **editar** (no solo marcar) un Hábito/Tarea/Rutina/
Medicamento/Cita/Fecha importante compartido no se estaba re-subiendo — los 6
`Actualizar*UseCase` no llamaban a `SincronizarSiEstaCompartidaUseCase`, solo lo hacían los
casos de Marcar. Reglas de Firestore nuevas (`esAdmin()`, delete ampliado en `miembros` y
`actividadesCompartidas`) publicadas por el usuario. Detalle completo en
`Plan/08-decisiones-tecnicas.md`.
