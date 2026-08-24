# Pendientes — qué falta y por qué

Vista rápida de todo lo que quedó a medias o sin empezar en las sesiones anteriores, para no
tener que rastrearlo dentro de `08-decisiones-tecnicas.md` (que ya es un documento largo,
pensado para leer "por qué se decidió X", no como lista de tareas). Cuando algo de esta lista
se construye, se borra de acá y el detalle de cómo se hizo queda en
`08-decisiones-tecnicas.md` como siempre.

**Prueba real con dos dispositivos — hecha (2026-08-23)**: primera vez probando con dos cuentas
de Google reales en dos celulares (Familia, código de invitación). Salieron 3 bugs reales, los 3
arreglados el mismo día — ver `08-decisiones-tecnicas.md`. Círculo de cuidado (compartir una
Tarea/Medicamento puntual) todavía no se probó así — sigue bloqueado por el mismo hueco de "ver
contenido compartido" de abajo.

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
Falta el mismo mecanismo para Círculo de cuidado (compartir una Tarea/Medicamento puntual) —
decidido con el usuario: se hace cuando se resuelva el hueco de "ver contenido compartido" de
abajo, para no construir una aceptación que no muestra nada real del otro lado. Ver
`08-decisiones-tecnicas.md`.

**Logo/ícono de la app — sin construir**: el usuario propuso un ícono que evoluciona solo con el
tiempo (semilla → brote → plantita, 3 etapas) usando el mismo mecanismo que apps como Genshin
Impact (varios íconos declarados, activados por código). Técnicamente viable, pero necesita 3
imágenes de ícono ya diseñadas/exportadas que Claude no puede generar — bloqueado hasta que
alguien las diseñe.

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
- **Ver el contenido real de lo que me compartieron por Círculo de cuidado** (compartir puntual
  de un hábito/tarea con un contacto, distinto de un Espacio Familia) — al aceptar una solicitud
  hoy solo sincroniza la solicitud y la conexión (la "capa social"), no el hábito/tarea/
  medicamento en sí. Sigue sin resolverse (a diferencia de Espacios Familia, que ya sincroniza
  su contenido) — necesitaría el mismo tipo de mapeo por Firestore + una pantalla nueva de "lo
  que otros comparten conmigo". Ver `08-decisiones-tecnicas.md`, 2026-08-19.
- **Invitación a personas sin la app instalada** (deep link + onboarding especial).
- **Estados `confirmado/sin_confirmar/omitido` visibles para quien acompaña** — depende del
  punto anterior (mostrar el contenido real primero).
- **Buscar usuarios existentes por nombre/correo** (compartir) — no hay ningún directorio de
  quién más usa Lula fuera del propio teléfono; hoy compartir sigue siendo por contacto
  (correo/teléfono) en texto libre.
- **Escanear un QR para conectar/invitar y que quede aceptado automáticamente** (estilo Yape) —
  **construido para Espacio Familia (2026-08-23)**, con código de tiempo de vida corto (60s,
  se renueva solo) en vez de uno permanente, para que guardar una foto del QR no sirva después.
  Ver `08-decisiones-tecnicas.md`. Para Círculo de cuidado (compartir una Tarea/Medicamento
  puntual) sigue pendiente — construirlo ahí requeriría antes resolver el punto de arriba (ver
  contenido real compartido), si no, aceptar no mostraría nada del otro lado.
- **Aviso "📩" de invitación pendiente** — corregido dos veces el mismo bug (filtraba por
  `usuarioId` en vez de por correo): primero en `observarPendientesPara` (DAO), después se
  encontró que `TopBarStatsViewModel` seguía llamándolo con el id viejo (2026-08-20). Ya debería
  activarse solo una vez haya una solicitud real pendiente — falta confirmarlo con una segunda
  cuenta real.
- **Fase 1.5 — Familia/Equipo**: invitar miembros de verdad (2026-08-20) y sincronizar el
  contenido — Tareas y Retos familiares (2026-08-21) — ya están construidos. Falta probar todo
  junto con una segunda cuenta/dispositivo real. Sigue pendiente: roles admin/miembro con
  sentido real (hoy quien invita queda admin, quien acepta siempre entra como `MIEMBRO` sin
  forma de cambiarlo), progreso de un Reto familiar con más de un participante real (falta
  probarlo con alguien de verdad), y sincronizar Hábitos/Medicamentos/Citas dentro de un Espacio
  Familia (a propósito fuera de alcance — son de uso personal, ver `08-decisiones-tecnicas.md`).
- **Compartir una Lista en seguimiento conjunto con un amigo puntual** (no solo dentro de un
  espacio Familia) — mismo patrón que ya existe en Retos familiares ("X de Y ya cumplieron
  hoy"), aplicado a una Lista. Pedido por el usuario 2026-08-15, ver `08-decisiones-tecnicas.md`.

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
- Espacio Familia: solo uno por usuario (no varios espacios familiares/equipo a la vez) — el
  selector y las pantallas de `features/family/` asumen esto. Ampliarlo a varios espacios es
  una extensión futura si hace falta, no un rediseño.
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
