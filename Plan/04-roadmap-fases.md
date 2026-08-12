# Roadmap de fases — Lula

Construir en orden. No avanzar a la fase siguiente hasta que la anterior sea funcional y
usable de forma real por el primer usuario (Giancarlo).

## Fase 0.1 — Núcleo personal

Objetivo: una app usable a diario, aunque simple.

Estado al 2026-07-25 (ver decisiones técnicas de la base en `08-decisiones-tecnicas.md`):

- ⬜ Onboarding completo (ver `06-onboarding.md`) — hoy usa un usuario semilla local en vez
  de este flujo; ver interfaz `AuthRepository` ya lista para conectar Firebase/onboarding
  real sin romper nada
- ✅ Pantalla Hoy con actividades por momento del día (Mañana/Tarde/Noche + Tareas de hoy +
  resumen rápido de gastos), estado vacío y con datos
- ✅ Hábitos: crear, editar, pausar/reanudar, eliminar, lista con tracker semanal (●○),
  detalle con racha e historial de 30 días — accesible desde la posición "Hábitos" del
  bottom bar
- ✅ Tareas: crear, editar, completar/pendiente, eliminar, lista y detalle — accesible desde
  "Ver todas las tareas →" en Hoy
- ✅ Finanzas básicas: registrar/editar/eliminar ingreso o egreso con categoría, pantalla
  Finanzas con balance del mes y gastos de hoy, historial del mes (editable/eliminable al
  tocar cada fila) — accesible desde la posición "Finanzas" del bottom bar. Filtro por otros
  periodos (no solo el mes en curso) queda pendiente para una siguiente pasada
- ✅ Cierre del día: resumen, 3 preguntas opcionales, puntuación, racha
- ✅ Historial simple: lista de días cerrados (fecha, puntos, actividades, las 3 respuestas
  de reflexión) — accesible tocando la insignia de racha 🔥 en Hoy o desde "Ver mi historial"
  al cerrar el día
- ✅ Dictado de campo en todo texto libre: nombre de hábito/tarea, categoría/descripción de
  movimiento, las 3 reflexiones de Cerrar mi día — vía el reconocedor de voz del sistema
  (`DictationTextField`, sin permiso de micrófono propio)
- 🟡 Sincronización local-first: **Room local-first** ✅; falta el cliente de sync contra
  Firestore (campo `sync_status` ya existe en el modelo, listo para conectar — backend
  decidido 2026-08-01: Firebase, no Sheets/n8n, ver `12-firebase-auth-y-sync.md`)
- 🟡 Zona Privada: **configurar PIN + desbloqueo con huella/PIN** ✅, gatea la sección
  Finanzas; falta el auto-bloqueo por inactividad y extender el gate a Diario/Notas privadas
  cuando existan (ver `08-decisiones-tecnicas.md`)
- ✅ Notificaciones/recordatorios: hora opcional al crear/editar Hábito o Tarea (con fecha),
  alarma exacta con `AlarmManager`, sobrevive a reinicios del teléfono (`BootReceiver`),
  tocar la notificación abre la app en el Hábito/Tarea correspondiente, 3 niveles de
  intensidad elegibles por el usuario (🔇 Silencioso / 🔔 Sonido / ⏰ Alarma) con acceso directo
  a Ajustes del sistema para personalizar el sonido de cada uno — ver
  `08-decisiones-tecnicas.md`

**Siguientes pasos recomendados, en orden** (1, 2, 3, 5, 6 y 7 completados el 2026-07-25/27 —
el 4, Onboarding, se saltó a propósito porque necesita un proyecto de Firebase del lado del
usuario; no bloquea el resto):
1. ~~Pantallas de lista/detalle de Hábitos (editar, pausar, tracker semanal) y de Tareas~~ ✅
2. ~~Pantalla Finanzas (balance del mes, historial filtrable por el mes en curso)~~ ✅
3. ~~Historial simple (ver `RegistroDiario` pasados)~~ ✅
4. Onboarding completo, reemplazando el usuario semilla — **pendiente de que el usuario
   configure un proyecto de Firebase (`google-services.json`)**
5. ~~Dictado de campo (SpeechRecognizer) en los campos de texto libre existentes~~ ✅
6. ~~Zona Privada con biometría/PIN (gatea Finanzas)~~ ✅
7. ~~Notificaciones/recordatorios para hábitos y tareas con horario~~ ✅
8. Sync real con Firestore, cuando el usuario cree el proyecto de Firebase (ver
   `12-firebase-auth-y-sync.md`, sección 7)
9. Con eso completo, Fase 0.1 queda funcionalmente terminada salvo Onboarding (bloqueado por
   Firebase) — el siguiente hito real es Fase 0.5 (`Rutinas`, `Metas`, Revisión semanal,
   Hábitos progresivos, Matriz de Eisenhower)

## Fase 0.5 — Mejora continua

Objetivo: la app empieza a ayudar a decidir, no solo a registrar.

Estado al 2026-07-27:

- ✅ Metas: crear (por hábito / por monto / por número / manual), lista y detalle con barra
  de progreso, agregar progreso manual, eliminar — accesible desde "Ver mis metas" en Hoy.
  Progreso "por hábito" se calcula solo desde el historial del hábito vinculado (últimos N
  días). Área de vida, fecha límite y **editar** una meta ya creada quedaron fuera de esta
  pasada (ver `08-decisiones-tecnicas.md`)
- ✅ Rutinas: agrupar Hábitos/Tareas ya existentes por referencia (ej. "Rutina de mañana"),
  lista/detalle con checklist, "Marcar rutina completa" en un toque — accesible desde "Ver mis
  rutinas" en Hoy y desde el menú `+`. No aparece dentro de Hoy mezclada con hábitos/tareas
  sueltos (ver `08-decisiones-tecnicas.md`)
- ✅ Revisión semanal: cumplimiento general de la semana en curso, racha máxima de la semana,
  hábito que mejor/peor le fue, 3 preguntas de reflexión (dictado) — accesible desde "Ver mi
  revisión semanal" en Hoy, sin gating al domingo (ver `08-decisiones-tecnicas.md`); precarga y
  permite editar la revisión ya guardada de la semana en curso, y tiene un historial de
  semanas pasadas ("📜 Ver semanas anteriores") — falta la pantalla "Progreso" más amplia que
  la envuelve según `02-pantallas.md`
- ✅ Tareas recurrentes: cada cuánto se repite (diaria/semanal/quincenal/mensual/bimestral/
  trimestral/anual) para pagos y trámites periódicos (luz, agua) — al marcarla hecha, vuelve
  sola a `SIN_CONFIRMAR` con la próxima fecha, como un hábito, y queda registrada en el
  historial. Ver `08-decisiones-tecnicas.md`
- ✅ Rachas y Constancia: Constancia (% de días activos en los últimos 30) como `StatPill` en
  Historial — la racha global ya existía. Ver `08-decisiones-tecnicas.md`
- ✅ Hábitos progresivos: tarjeta "¿Aumentamos?" en Hoy cuando toca revisar un hábito
  configurado como progresivo (Subir/Mantener/Recordarme después) — Lula pregunta, nunca
  decide sola. Ver `08-decisiones-tecnicas.md`
- ✅ Matriz de Eisenhower: vista "🗂️ Matriz" dentro de Tareas, agrupa por
  importante/urgente en 4 secciones. Ver `08-decisiones-tecnicas.md`
- ✅ Listas reutilizables: plantillas de ítems por chequear (ej. "Viaje", "Compras") que se
  "reinician" (desmarcan) para la próxima vez, sin duplicar la plantilla — accesibles desde
  "Ver mis listas" en Hoy y desde el menú `+`. Ver `08-decisiones-tecnicas.md`

Con esto, Fase 0.5 queda funcionalmente completa salvo la pantalla "Progreso" unificada de
`02-pantallas.md` (Constancia y Revisión semanal ya existen, pero repartidas en Historial y su
propia pantalla en vez de una sola vista) — pendiente si se pide más adelante.

## Fase 0.8 — Cuidado personal (solo para uno mismo)

Objetivo: extender `Actividad` a medicamentos y citas, todavía sin compartir con nadie.

Estado al 2026-07-28:

- ✅ Medicamentos con dos modos de frecuencia: por intervalo de horas (primera dosis + cada
  cuántas horas) o por relación con comidas (antes/después de desayuno/almuerzo/cena) — crear,
  editar, pausar/reanudar, eliminar, historial de tomas de los últimos 7 días
- ✅ Preferencia de horarios de comida guardada en el perfil (`Usuario.horaDesayuno/
  horaAlmuerzo/horaCena`), con prompt inline la primera vez que se elige "según las comidas"
  en vez de una pantalla de Perfil dedicada (fuera de alcance esta fase)
- ✅ Citas y controles: nombre, lugar, motivo, fecha y hora, recordatorio configurable
  (mismo día / un día antes / una semana antes) — crear, editar, eliminar
- ✅ Regla de producto respetada en todo el flujo: Lula solo programa y registra lo que el
  usuario indicó (nombre, dosis, horarios) — nunca sugiere cambiar dosis, horarios ni
  suspender un medicamento; el texto se muestra explícitamente en la pantalla de creación
- ✅ Pantalla "Mi salud" que agrupa medicamentos activos (con sus tomas de hoy y acceso a
  crear/editar) y próximas citas (con eliminar) — accesible desde "Ver mi salud" en Hoy y
  desde el menú `+`
- ✅ Tomas de medicamento de hoy integradas en Hoy, con 3 estados posibles por toma (⏳
  Pendiente / ✅ Tomada / ⏭️ Omito, ninguno es un castigo) — reutiliza la misma pantalla de
  acción que llega al tocar la notificación (`toma_accion`)
- ✅ Todo nace con `privacidad: solo_yo`
- ✅ Recordatorios de Medicamento (uno independiente por horario, ej. varias tomas al día) y
  Cita reutilizan la misma infraestructura de `NivelRecordatorio`/`AlarmManager`/
  `BootReceiver` que Hábitos y Tareas — ver `08-decisiones-tecnicas.md`

Con esto, Fase 0.8 queda funcionalmente completa. Quedan fuera de alcance a propósito: una
pantalla de "Perfil" dedicada para editar los horarios de comida fuera del flujo de crear un
medicamento, y una pantalla de detalle separada para Cita (se edita/elimina directo desde "Mi
salud").

## Fase 1.0 — Círculo de cuidado

Objetivo: activar el flujo de compartir, elemento por elemento, con consentimiento explícito.

Estado al 2026-07-29 — **base local construida, activación real pendiente de Firebase**
(ver decisión en `08-decisiones-tecnicas.md`: la app sigue sin autenticación real ni sync
entre dispositivos, así que compartir con una persona en OTRO teléfono todavía no puede
funcionar de punta a punta):

- ✅ `SOLICITUD_COMPARTIR`: enviar (desde "🤝 Compartir seguimiento" en el detalle de Hábito,
  Tarea o Medicamento) y cancelar/revocar. **Aceptar/rechazar queda pendiente** — solo tiene
  sentido cuando la otra persona tiene cuenta propia
- ⬜ Invitación a personas sin la app instalada (deep link + onboarding especial) — bloqueado
  por Firebase
- 🟡 Pantalla "Mi círculo de cuidado": **"Quién me acompaña a mí" (personas a las que
  compartiste algo) funcional** ✅ — lista, estado, cancelar/revocar; "Personas que acompaño"
  es un estado vacío honesto por ahora (necesita que la otra persona también use Lula)
- ✅ Gestión y cancelación de una solicitud enviada, en cualquier momento
- ⬜ Estados `confirmado/sin_confirmar/omitido` visibles para quien acompaña — depende de que
  haya datos de otra persona sincronizados a este dispositivo, no aplica todavía

Con esto, la mitad de Fase 1.0 que **no depende de un backend** queda construida y lista para
activarse en cuanto exista autenticación real — ver `08-decisiones-tecnicas.md` para el
detalle de qué falta exactamente y por qué.

## Fase 1.5 — Familia / Equipo

Objetivo: espacios compartidos con identidad propia, sin perder la privacidad individual.

Estado al 2026-07-30 — **base local construida, igual que Fase 1.0: activación real (una
segunda persona de verdad en otro dispositivo) pendiente de Firebase**:

- ✅ Crear espacio `familia` (el usuario como único admin), renombrarlo y eliminarlo (borrado
  en cascada de todo lo que tenía dentro)
- ✅ Selector de espacio en la navegación (Personal ⇄ Familia), con indicador de color propio
  visible en cualquier pantalla (no solo Hoy) — cambiar de espacio re-escopea Hoy, Tareas,
  Metas, Rutinas, Listas, Finanzas y Calendario solos, sin tocar esas pantallas
- ✅ Retos familiares: crear, ver progreso "x de y cumplieron hoy", marcar el propio
  cumplimiento — con un solo participante real hasta que existan invitaciones
- 🟡 Calendario compartido y Gastos compartidos: funcionan reutilizando las pantallas ya
  existentes de Calendario/Finanzas re-escopeadas al espacio Familia — falta lo de "compartido"
  en el sentido de diferenciar por persona (color/ícono), porque hoy solo hay una persona real
- ⬜ Invitar miembros de verdad — bloqueado por Firebase, mismo motivo que Fase 1.0
- ⬜ Tareas del hogar con múltiples responsables (selector "Responsables" + "Se completa
  cuando: Cualquiera/Todos deben confirmar") — con un solo miembro real no hay nada que
  elegir todavía; se construye junto con las invitaciones
- 🟡 Roles admin/miembro: el campo existe y funciona (`RolEnEspacio`), pero hoy todos son
  admin porque solo hay un miembro real — sin sentido real hasta que haya un segundo

Con esto, la mitad de Fase 1.5 que no depende de un backend queda construida — ver
`08-decisiones-tecnicas.md`, 2026-07-30, para el detalle completo de qué se construyó y por
qué, y `10-pendientes.md` para lo que sigue bloqueado.

## Extensiones posteriores a Fase 1.5 (2026-08-01 a 2026-08-07)

Trabajo real construido después de las fases numeradas de arriba, que no encaja en una sola
fase del roadmap original — detalle completo con fecha exacta de cada punto en
`08-decisiones-tecnicas.md`:

- ✅ **Mi propósito** (Misión/Visión/Propósito): 8 preguntas guiadas, editables y borrables de
  a una, siempre del espacio Personal. Sin síntesis narrativa con IA todavía (botón
  deshabilitado, "próximamente").
- ✅ **Cuentas y conexiones — base local** (`11-cuentas-y-conexiones.md`): `Usuario` ampliado
  (consentimientos, mayoría de edad), tabla `Conexion`, pantalla "Eliminar mi cuenta", textos
  legales con pantalla para leerlos. **Decisión de backend real: Firebase** (Auth + Firestore,
  `12-firebase-auth-y-sync.md`), bloqueada por un paso que solo el usuario puede hacer (crear
  el proyecto en Firebase Console).
- ✅ **Formulario compacto** (`SelectorRow` + `ModalBottomSheet`) replicado a Crear
  Medicamento/Tarea/Hábito/Cita/Fecha importante, con protección "descartar cambios al salir"
  en las 10 pantallas "Crear X" de la app.
- ✅ **Citas recurrentes ("cursos")**: una Cita puede tener varias sesiones en un patrón de
  días de la semana (radioterapia, sesiones de masaje), cada sesión reprogramable
  individualmente. Nueva tabla `SesionCita`.
- ✅ **Recordatorio persistente de Medicamento**: insiste cada N minutos hasta marcar la toma
  o que termine el día, configurable por medicamento.
- ✅ **Hábitos rediseñado**: tarjetas con ícono automático, racha propia por hábito, letras de
  día reales, agrupado por momento del día, mensaje motivacional.
- ✅ **Recordatorio diario configurable** ("🔥 Recordarme cerrar mi día") — primer recordatorio
  de la app no ligado a ningún hábito/tarea/cita/medicamento en particular.
- ✅ Resumen de ingresos/gastos/balance + rango de fechas manual en el Historial de Finanzas.

## Fase 2.0 — Asistente (voz y chat)

Objetivo: conectar todo lo construido con interpretación de intención por voz/texto,
reutilizando el patrón ya validado en Ernesto/Mayia.

- Asistente conversacional generalizado a cualquier `tipo` de `Actividad`
- 4 modos de interacción: manos libres, dictado + edición, silencioso, filtro de ruido
  siempre activo (ver `07-asistente-voz.md`)
- Modo sin conexión con interpretación local (Whisper/modelo ligero) y sincronización posterior
- Confirmación explícita antes de ejecutar acciones sensibles (compartir, eliminar, montos altos)

## Fases futuras (no detalladas aún)

- Plan Equipos orientado a pequeñas empresas (mejora continua + hábitos de equipo, no
  competencia directa con Trello/Asana/Monday)
- IA Premium: análisis de patrones y recomendaciones proactivas
- Línea de vida como vista dedicada
