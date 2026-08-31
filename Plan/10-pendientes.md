# Pendientes — qué falta y por qué

Vista rápida de todo lo que quedó a medias o sin empezar en las sesiones anteriores, para no
tener que rastrearlo dentro de `08-decisiones-tecnicas.md` (que ya es un documento largo,
pensado para leer "por qué se decidió X", no como lista de tareas). Cuando algo de esta lista
se construye, se borra de acá y el detalle de cómo se hizo queda en
`08-decisiones-tecnicas.md` como siempre.

**Pendiente, pedido por el usuario (2026-08-24)**: elegir un color (de una paleta) por Espacio
Familia, para distinguirlas en la lista (hoy todas se ven igual, verde) — y también un color
propio para el usuario. No construido todavía, queda para una ronda futura.

**Pendiente, pedido por el usuario (2026-08-24)**: que un Medicamento se marque solo como
"histórico" (`activa = false`) en cuanto pasa su `fechaFin`, en vez de quedar `activa = true`
para siempre confiando solo en el filtro de fecha (`horariosParaFecha`) para que no vuelva a
sonar. Hoy funciona bien igual (los vencidos no pueden disparar alarma), pero el usuario espera
que quede como cerrado/histórico de verdad, mismo patrón que ya existe para Tarea vinculada a
Medicamento/Cita (`CerrarTareasVinculadasVencidasUseCase`). No construido todavía.

**Prueba real con dos dispositivos — hecha (2026-08-23)**: primera vez probando con dos cuentas
de Google reales en dos celulares (Familia, código de invitación). Salieron 3 bugs reales, los 3
arreglados el mismo día — ver `08-decisiones-tecnicas.md`. Círculo de cuidado (compartir una
Tarea/Medicamento puntual, con el contenido real ya construido) queda pendiente de esa misma
prueba de punta a punta.

**Quitar/salir de Familia, dejar de ver algo compartido, sync al editar — construido (2026-08-23)**:
admin puede quitar a un miembro, cualquiera puede salir de un espacio Familia, el destinatario de
Círculo de cuidado puede "Dejar de ver esto" sin esperar a quien comparte, y editar (no solo
marcar) algo compartido ahora sí se re-sube a Firebase. Reglas nuevas publicadas. Falta probar de
punta a punta con los dos celulares reales: quitar miembro (como admin), salir de un espacio,
"Dejar de ver esto", y confirmar que editar algo compartido (ej. cambiar hora de una Cita) se ve
del otro lado. Ver `08-decisiones-tecnicas.md`.

**Respaldo del Espacio Personal — completo (2026-08-22)**: Hábitos, Tareas, Rutinas,
Medicamentos (con tomas), Citas (con sesiones de curso), Fechas importantes, Finanzas, Diario,
Notas, Metas, Listas, Mi propósito, y el historial de Cerrar mi día/Revisión semanal. Sin
restricción todavía (se corta detrás de premium cuando exista el cobro). Ya no queda ningún tipo
del Espacio Personal sin respaldar.

**Registro obligatorio + preguntas de onboarding — construido (2026-08-23)**: bienvenida, cuenta
(Google, correo mágico como "próximamente"), privacidad, 5 preguntas para conocerte, resumen.
Falta: el paso "Hábitos sugeridos" del diseño original (`Plan/06-onboarding.md`, elegir 2-5
hábitos según las respuestas 4a/4b) — quedó fuera de esta ronda a propósito, es lógica de
producto separada del gate de registro en sí. Y el enlace mágico por correo sigue sin poder
completarse de verdad: necesita un dominio propio configurado en Firebase (Dynamic Links, la
forma vieja de resolver esto sin dominio, fue dado de baja por Google) — bloqueado hasta que el
usuario tenga/configure uno.

**Código de invitación a Espacio Familia con tiempo de vida corto — construido (2026-08-23)**:
escanear y quedar dentro al instante, sin paso de aceptar, código de 60s que se renueva solo.

**"Compartir seguimiento" con QR instantáneo (Círculo de cuidado) — construido (2026-08-24)**:
mismo mecanismo que Familia — QR como opción primaria del diálogo (antes pedía escribir nombre/
correo/teléfono antes de mostrar cualquier QR, y ese QR ni siquiera era escaneable por la app).
Código de 3 minutos, escanearlo crea la solicitud ya `ACEPTADA` directo en Firestore — sin esperar
que la otra persona "acepte". **Alcance recortado el mismo día a pedido del usuario**: Hábito,
Meta y Rutina son "más personales" y ya NO tienen botón de compartir en absoluto (se sacó por
completo, no solo el QR); Tarea, Medicamento y Cita sí lo mantienen — el caso de cuidado real
(medicamentos/citas de alguien a quien acompañas) y Tarea (que "reemplaza" a una meta compartida
hasta cumplirse). Falta probarlo de punta a punta con los dos celulares reales.

**Logo/ícono de la app que evoluciona con el tiempo — construido (2026-08-23)**: semilla (0-29
días) → plantita sin flor (30-59 días, o 60+ sin racha activa) → flor (60+ días con racha activa
ahora mismo). 3 `activity-alias` en el manifest, cambia solo en cada apertura de la app. Ver
`08-decisiones-tecnicas.md`.

**Círculo de cuidado: ver el contenido real compartido — construido (2026-08-23)**: Hábito,
Tarea, Rutina, Medicamento (con tomas), Cita (con sesiones) y Fecha importante, con pantalla
nueva "Lo que me comparten". Cierra el hueco que estaba documentado acá desde hace varias
rondas. Falta:
- Probarlo de punta a punta con los dos celulares reales (compartido arriba).
- **Compartir una Meta por Círculo de cuidado sigue roto** (bug encontrado de paso, no de esta
  ronda): `MetaDetailScreen` ofrece "🤝 Compartir seguimiento" igual que Hábito/Tarea, pero una
  Meta no vive en la tabla `Actividad` — al aceptar, la búsqueda por `elementoId` no encuentra
  nada y no pasa nada. Necesita su propio camino de aceptar/sincronizar contenido (Meta no tiene
  ni registro de tomas/sesiones, así que sería más simple que el resto).
- **"Puede ver y recordar" — construido (2026-08-29)**: botón "🔔 Recordarle" en "Lo que me
  comparten" (solo visible con ese permiso). Best-effort vía Firestore (`recordatoriosSolicitados`)
  + listener en `TopBarStatsViewModel` que muestra una notificación local mientras la app de
  quien comparte esté abierta — no es push real, no hay FCM en esta app. Ver
  `08-decisiones-tecnicas.md`. **Falta publicar la regla de Firestore en Firebase Console** y
  probarlo de punta a punta con los dos celulares reales.

**Perfil de usuario — arreglado (2026-08-23) + Ajustes sincronizados (2026-08-28)**: el nombre
real y los horarios de comida se suben y se recuperan bien en un celular nuevo, y el registro se
salta solo si la cuenta ya lo había completado antes en otro lado. Sonido de check, horas de
recordatorio, barra inferior y duración de Alarma ahora también viajan (se restauran una sola
vez al vincular la cuenta). A propósito sigue sin sincronizar el **espacio activo** — vive solo
en memoria por una decisión anterior (2026-07-30, para que cerrar/abrir la app siempre vuelva a
Personal). Ver `08-decisiones-tecnicas.md`.

## 1. Bloqueado por backend (necesita Firebase + algo de sync)

Nada de esto se puede terminar de verdad mientras la app siga siendo un solo usuario semilla
local sin servidor. Están construidas las partes que sí funcionan sin backend; esto es lo que
falta activar en cuanto exista:

- **Onboarding real**, reemplazando el usuario semilla (`AuthRepository` ya está listo para
  conectar la implementación real sin romper nada). Toda la parte local-first de
  `11-cuentas-y-conexiones.md` ya está construida (2026-08-01): `Usuario` ampliado
  (consentimientos, mayoría de edad), tabla `Conexion` nueva, pantalla "Eliminar mi cuenta",
  borradores de Política de Privacidad/Términos de Servicio con pantalla en la app para leerlos
  — ver `08-decisiones-tecnicas.md`. Falta: (1) que alguien con conocimiento legal real revise
  los textos (hoy tienen placeholders — nombre del responsable, correo de contacto — sin
  completar), (2) publicarlos en una URL pública (Play Store lo exige para la Política de
  Privacidad), y (3) recién después, el login real en sí. **Decidido con el usuario
  (2026-08-01): se usa Firebase (Auth + Firestore)** — diseño completo en
  `12-firebase-auth-y-sync.md`. Bloqueado ahora mismo por una acción que solo el usuario puede
  hacer: crear el proyecto en Firebase Console y pasar el `google-services.json` (pasos exactos
  en la sección 7 de ese documento) — hasta que eso llegue, no se puede escribir el código de
  conexión real.
- **Sync a Firestore de `Conexion`/`SolicitudCompartir` — construido (2026-08-19)**: aceptar/
  rechazar real, `Conexion` se crea al aceptar, y todo sincroniza con Firestore mientras
  Círculo de cuidado está abierta (`CompartirSyncRepository`). Ver `08-decisiones-tecnicas.md`.
  Falta probar de punta a punta con una segunda cuenta/dispositivo real.
- **Sync de contenido de Espacios Familia — construido (2026-08-21)**: Tareas del hogar y Retos
  familiares (paso 5 de `12-firebase-auth-y-sync.md`), `EspacioSyncRepository` nuevo. Ver
  `08-decisiones-tecnicas.md`. Falta confirmarlo con una segunda cuenta/dispositivo real.
- **Ver el contenido real de lo que me compartieron por Círculo de cuidado — construido
  (2026-08-23)**: pantalla "Lo que me comparten", ver el bloque de arriba. Falta probarlo con
  una segunda cuenta/dispositivo real, y compartir una Meta sigue roto (ver arriba).
- **Invitación a personas sin la app instalada** (deep link + onboarding especial).
- **Estados `confirmado/sin_confirmar/omitido` visibles para quien acompaña — construido
  (2026-08-23)**: "Lo que me comparten" ya muestra el estado actual (hecho hoy, tomas, sesiones).
- **Buscar usuarios existentes por nombre/correo** (compartir) — no hay ningún directorio de
  quién más usa Lula fuera del propio teléfono; hoy compartir sigue siendo por contacto
  (correo/teléfono) en texto libre.
- **Escanear un QR para conectar/invitar y que quede aceptado automáticamente** (estilo Yape) —
  **construido para Espacio Familia (2026-08-23)**, con código de tiempo de vida corto (60s,
  se renueva solo) en vez de uno permanente, para que guardar una foto del QR no sirva después.
  Ver `08-decisiones-tecnicas.md`. Para Círculo de cuidado (compartir una Tarea/Medicamento
  puntual) sigue pendiente — construirlo ahí requeriría antes resolver el punto de arriba (ver
  contenido real compartido), si no, aceptar no mostraría nada del otro lado.
- **Pantalla real de Notificaciones — construida (2026-08-29/30)**: historial permanente
  (tabla `notificacion` en Room), agrupado por fecha, leído/no leído, con notificación local en
  ambos sentidos (nueva invitación recibida, y respuesta a la que envié) vía un listener global
  en `TopBarStatsViewModel`, copy motivador, diálogo de bienvenida al aceptar. El ícono 🔔 abre
  esta pantalla (badge = no leídas); "Mi círculo de cuidado" sigue aparte en el menú "⋮" como
  pantalla de gestión. Ver `08-decisiones-tecnicas.md`. Falta confirmarlo con una segunda cuenta
  real (bloqueado ahora mismo por el bug de "Continuar con Google" de abajo).
- **Fase 1.5 — Familia/Equipo**: invitar miembros de verdad (2026-08-20) y sincronizar el
  contenido — Tareas y Retos familiares (2026-08-21) — ya están construidos. Roles admin/miembro
  con sentido real — **construido (2026-08-27/28)**: varios co-admins, "Hacer admin"/"Quitar
  admin" con confirmación, el creador protegido (nadie más lo puede quitar ni bajarle el admin),
  solo el creador elimina el espacio completo, historial "quién quitó a quién" visible solo para
  admins. Ver `08-decisiones-tecnicas.md`. Falta probar todo junto con una segunda cuenta/
  dispositivo real. Sigue pendiente: progreso de un Reto familiar con más de un participante real
  (falta probarlo con alguien de verdad), y sincronizar Hábitos/Medicamentos/Citas dentro de un
  Espacio Familia (a propósito fuera de alcance — son de uso personal, ver
  `08-decisiones-tecnicas.md`).
- **Compartir una Lista en seguimiento conjunto con un amigo puntual** (no solo dentro de un
  espacio Familia) — mismo patrón que ya existe en Retos familiares ("X de Y ya cumplieron
  hoy"), aplicado a una Lista. Pedido por el usuario 2026-08-15, ver `08-decisiones-tecnicas.md`.
- **"Continuar con Google" se queda pegado en un celular específico** (reportado 2026-08-30,
  Xiaomi/POCO con HyperOS, Android 16) — en otros 2 celulares instaló y funcionó normal, así que
  no es un bug de la app/Firebase en general. Candidatos: Google Play Services desactualizado en
  ese celular, sin cuenta de Google agregada, o señal débil (la captura mostraba velocidad de
  datos muy baja). La app usa Credential Manager (`GoogleSignInHelper.kt`), no la API vieja.
  Pendiente de diagnosticar con ese celular conectado por USB — el usuario lo probará después.

## 2. Piezas de UI que quedaron afuera de una fase ya "completa"

**Copiar una Lista a otra persona vía QR — construido (2026-08-20)**, ver
`08-decisiones-tecnicas.md`.

- Rutinas dentro de Hoy mezcladas con hábitos/tareas sueltos (a propósito no están, para no
  duplicar la visualización).
- Dictado por voz sin conexión — un intento de arreglarlo con `EXTRA_PREFER_OFFLINE` resultó
  ser una regresión peor y se revirtió; el caso sin conexión sigue sin resolver.
- **Multimedia (fotos, dibujo/pizarra) — descartado a propósito, no solo pospuesto.** Notas
  quedó con editor de texto nomás (sin dibujo a mano alzada); Diario tuvo adjuntar fotos
  construido y **revertido** el mismo día (ver `08-decisiones-tecnicas.md`, 2026-07-30) — el
  usuario decidió que Lula se enfoca en texto y tablas, sin multimedia, por el costo/
  complejidad de administrar imágenes en la nube cuando exista sync real (hoy todo es local).
  No reconstruir sin que el usuario lo pida de nuevo explícitamente.
- Calendario: la vista Semana muestra secciones apiladas por día (no una grilla de horas tipo
  Google Calendar); suficiente para ver "qué hay cada día de la semana", pero no para ver
  huecos libres de horario como en un calendario de citas real.
- Tareas del hogar (espacio Familia): no tiene todavía el selector "Responsables" (checkboxes
  de miembros) ni "Se completa cuando: Cualquiera / Todos deben confirmar" de
  `02-pantallas.md` — se dejó afuera a propósito porque con un solo miembro real hoy no hay
  nada que elegir; tiene sentido construirlo junto con las invitaciones reales.
**Varios Espacios Familia por usuario — construido (2026-08-24)**: antes la pantalla se quedaba
solo con la primera Familia (`firstOrNull`); ahora `FamiliaScreen` lista todas ("Tus espacios
familiares"), cada una con su propio "Administrar" (miembros, invitar, QR, renombrar, eliminar,
salir) — pensado para el caso real de una persona con varias familias (la que formó, la de sus
padres, la de su pareja). No fue un rediseño de datos/sync (ya soportaban N espacios sin cambios,
solo la UI se quedaba con uno) — ver `08-decisiones-tecnicas.md`. "🏆 Retos familiares" ya
navega por `espacioId` explícito (construido 2026-08-28) — se puede abrir para cualquier Familia
que se esté administrando, no solo la que sea el espacio activo. Queda pendiente lo mismo para
Tareas del hogar (menor prioridad, no reportado como confuso todavía).
- Aviso "tienes pendientes en Familia" en Hoy Personal (ver `08-decisiones-tecnicas.md`,
  2026-07-30): hoy es un contador simple (hábitos + tareas de hoy sin confirmar), no una vista
  unificada de todo lo pendiente en ambos espacios a la vez. Si más adelante hace falta algo
  más completo (ej. fusionar Hoy de los dos espacios en una sola lista, marcada por espacio),
  es un cambio más grande a evaluar con el usuario primero.
- Mi propósito: no sintetiza las respuestas en un párrafo de Misión/Visión/Propósito narrativo
  — solo las muestra ordenadas por pregunta. El botón "🤖 Armar y presentar con IA" ya está en
  la pantalla pero deshabilitado ("próximamente") — falta el workflow de n8n del otro lado
  (todavía no armado a propósito, el usuario prefiere terminar de estabilizar el modelo de
  datos primero — ver "usuarios pendientes" más abajo — para no tener que modificar n8n dos
  veces) y el cliente HTTP en la app (hoy no existe ningún llamado de red, ver
  `08-decisiones-tecnicas.md`, 2026-08-01). (Lo de "borrable" — borrar una respuesta puntual o
  todo de una — ya se construyó el 2026-08-01. Las preguntas se corrigieron el mismo día: 7
  "personales" arman Misión y Visión juntas + 1 pregunta nueva directa de Propósito; las 6
  preguntas de "objetivos" que estaban mal puestas ahí se movieron a Crear Meta, como ayuda de
  referencia.)

- **"Citas históricas" en Mi salud**: hoy la pantalla solo tiene "Próximas citas" (incluye
  sesiones pendientes de un curso) — no existe ninguna sección para ver citas puntuales ya
  pasadas/cumplidas ni el historial completo de sesiones de un curso ya terminado (el detalle
  de la Cita sí muestra todas sus sesiones, pero no hay una vista agregada de "citas pasadas"
  como lista). Mencionado por el usuario 2026-08-07, quedó fuera de alcance esa ronda porque
  el arreglo urgente era que un curso en progreso no desapareciera de la lista, no un
  historial completo nuevo.
- **Ícono/mascota de estado del día** (🙂/✅/⏳/💤, descrito en `01-arquitectura.md`) — sigue sin
  implementarse en ninguna pantalla.
- **Distinción visual de actividad propia/de apoyo/compartida en Hoy** — el modelo lo soporta
  (`propietario`/`responsables[]`) pero no hay ningún ícono/indicador en la UI todavía; solo
  tiene sentido real con una segunda persona (bloqueado por backend, ver sección 1).
- **Método FODA (Fortalezas/Oportunidades/Debilidades/Amenazas + Aspiraciones/Resultados)** —
  propuesto por el usuario 2026-08-10 como algo para llenar de a poco, con la duda de si
  ayudaría a las personas. Decisión (a confirmar recién cuando se construya): **sí, pero como
  extensión de "Mi propósito"**, mismo patrón de preguntas guiadas (`respuestas: Map<String,
  String>`), no como módulo nuevo separado. F/O/D/A encajan directo ahí. "Aspiraciones y
  Resultados" **no** necesita un sistema de medición propio — una Aspiración se convierte en
  una `Meta` (ya existe en Lula) y el Resultado es el progreso que Metas ya calcula solo
  (`comoSeMide`/`valorActual`/`valorObjetivo`); construir algo aparte solo duplicaría esa
  lógica. Riesgo a tener en cuenta si se construye: un FODA completo es una herramienta de
  "sentarse a pensar" más pesada que el ritmo diario de Lula — sin conectarlo de vuelta a algo
  accionable (crear un hábito/tarea/meta desde ahí), corre el riesgo de ser un formulario que
  se llena una vez y nunca se vuelve a abrir. El usuario lo pidió explícitamente como algo para
  "después", pensado para alguien ya más metido en la app (no para el onboarding ni las
  primeras sesiones de uso) — no construir sin que lo pida de nuevo explícitamente.

## 3. Fases o funciones enteras sin empezar

- **Fase 2.0 — Asistente (voz y chat)** completa. A propósito se hace al final, después de que
  el modelo de datos deje de moverse — el usuario prefiere no tener que retocar Fase 2.0 por
  cambios de esquema hechos después. Con Notas (título/orden), la deuda técnica de migraciones
  y Propósito personal ya construidos (2026-07-30), el modelo de datos está más estable.
- **Fases futuras sin detallar**: Plan Equipos (pequeñas empresas), IA Premium, Línea de
  vida como vista dedicada.
