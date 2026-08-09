# Guía visual — Lula

Referencia visual creada el 2026-07-26 a partir de analizar Duolingo (la app de referencia de
gamificación) y "Me+" (app de rutinas/hábitos). Objetivo: que Lula se sienta viva y cálida sin
copiar las mecánicas de esas apps que chocan con la filosofía de Lula ("todo intento vale, no
hay castigos", ver `Plan/01-arquitectura.md`).

## Qué copiamos de la referencia (sí aplicar)

- **Fila de "stats" con íconos circulares** (racha 🔥, dinero 💰, etc.) — insignias tipo
  cápsula, siempre visibles, en vez de texto plano. Componente: `core/ui/StatPill.kt`.
- **Cada sección de la app tiene su propio color de marca** — así se reconoce de un vistazo,
  sin leer la etiqueta (bottom nav, Duolingo/Me+). Ver paleta abajo.
- **Barra de progreso gruesa tipo "píldora"**, con el color de marca — no la línea delgada
  default de Material3. Componente: `core/ui/LulaProgressBar.kt`.
- **Estado vacío con "mascota" emoji** en vez de solo texto — más cálido. Componente:
  `core/ui/EmptyState.kt`.
- **Botón `+` central del menú de agregar**, ya existente en `navigation/AddMenuSheet.kt`.
- Banners de celebración/motivación (pendiente aplicar más adelante, ej. en Cerrar mi día).

## Qué NO copiamos (choca con la filosofía de Lula)

- **Nada de leaderboard/ranking contra otras personas** ("quedaste en el puesto #7" de
  Duolingo) — genera comparación y presión social, lo opuesto a "todo intento vale".
- **Nada de rojo de alerta/advertencia** en rachas o progreso (el ícono de racha con ⚠️ rojo
  de Duolingo, que avisa "vas a perder tu racha") — Lula nunca amenaza con perder algo. El
  rojo prácticamente no se usa en la paleta; cuando haga falta un acento fuerte (ej.
  "urgente" en tareas) se usa naranja cálido, no rojo.
- **Nada de mensajes de fallo o comparación** ("Fallaste", "%peor que el promedio") — ya
  estaba prohibido en el copy (`01-arquitectura.md`), se extiende a lo visual: ningún ícono,
  color o animación debe comunicar "perdiste algo".

## Paleta de marca (`ui/theme/Color.kt`)

Un color fijo por significado, reutilizado siempre igual en toda la app (no se reasignan
colores por pantalla):

| Color | Uso | Valor (claro) |
|---|---|---|
| Violeta (`LulaPrimary`) | Marca, acciones primarias, pestaña "Hoy" | `#7C6FF0` |
| Naranja (`LulaRacha`) | Racha, energía, gastos (nunca alerta) | `#FF9F45` |
| Verde (`LulaHabito`) | Hábitos, confirmación, ingresos | `#4CC38A` |
| Azul (`LulaTarea`) | Tareas | `#4FA3FF` |
| Dorado (`LulaFinanzas`) | Finanzas, balance | `#E0A425` |
| Rosado (`LulaAsistente`) | Asistente, celebración/cierre de día | `#EF7BA0` |

Cada color tiene una versión "contenedor" clara/oscura (`LulaXxxContainerLight/Dark`) para
fondos de insignias y círculos de navegación, siguiendo el patrón de Material 3.

**Dynamic color (Material You) está desactivado a propósito** en `Theme.kt` — Lula tiene
identidad visual propia y no debe cambiar de color según el fondo de pantalla del usuario.

## Componentes reutilizables (`core/ui/`)

- `StatPill(emoji, valor, colorContenedor)` — insignia cápsula para stats.
- `LulaProgressBar(progreso)` — barra de progreso gruesa animada.
- `EmptyState(emoji, titulo, subtitulo?, textoBoton?, onBotonClick?)` — estado vacío cálido.
- `ColoredEmojiIcon(emoji, colorContenedor)` — emoji en círculo de color.
- `SectionLinkRow(emoji, color, texto, onClick)` — fila "ir a otra sección" con ícono a
  color y flecha, para enlaces internos tipo "Ver todas mis tareas →".
- `HoraRecordatorioSelector(horaSeleccionada, onHoraSeleccionada)` — chip "Sin recordatorio"
  + `TimePicker` real en un diálogo.

Usar estos tres en toda pantalla nueva en vez de recrear el patrón a mano — mantiene la app
visualmente consistente sin duplicar código (ver también la lección de Mayia en
`08-decisiones-tecnicas.md` sobre una sola fuente de verdad, aplicada aquí a UI).

## Lección aprendida: contraste de texto sobre color fijo

`StatPill` dejaba el texto sin color explícito → heredaba `LocalContentColor` (blanco en modo
oscuro) sobre un contenedor pastel claro fijo, y quedaba casi invisible. **Regla: cualquier
componente que pinta su propio fondo con un color fijo (no ligado al tema) debe fijar también
el color del texto/ícono que va encima — nunca heredarlo del tema.** Ya corregido en
`StatPill.kt` (texto `#2B2B2B` fijo).

## Componente `ColoredEmojiIcon`

Extraído de `LulaBottomBar` a `core/ui/ColoredEmojiIcon.kt` para reusarlo también en
`AddMenuSheet` (cada opción del menú `+` tiene su ícono a color, no solo texto violeta) — una
sola fuente de verdad para "emoji dentro de un círculo de color", en vez de duplicar el patrón.

## Aplicado hasta ahora (2026-07-27)

Bottom nav (íconos a color por sección, círculos de 44dp), menú `+` (ícono a color por
opción), pantalla Hoy (stats, progreso, estado vacío, racha clicable → historial, enlace a
Tareas con `SectionLinkRow`), Hábitos/Tareas/Finanzas (estado vacío), Historial (insignia de
puntos), Cerrar mi día (racha en `StatPill` al confirmar), selectores de fecha/hora reales
(`DatePicker`/`TimePicker`) en Crear Hábito y Crear Tarea, categorías de Finanzas por chips
con "Ahorro" destacado en `StatPill` propio cuando es mayor a 0.
**Pendiente para ir sumando en próximas sesiones**:
- Detalle de Hábito/Tarea y pantallas de creación — todavía usan `FilterChip`/`Button`
  default de Material3, sin el tratamiento cálido.
- Cerrar mi día — el mensaje "Buen trabajo..." sigue siendo texto plano; podría llevar un
  banner de color de fondo, no solo la insignia de racha que ya tiene.
- Ícono/mascota de estado del día (🙂/✅/⏳/💤) descrito en `01-arquitectura.md` — todavía no
  implementado en ninguna pantalla.
- Iconografía: seguimos usando emoji como ícono (sin `material-icons-extended`) — funciona
  bien y es liviano, mantenerlo así salvo que se necesite algo que el emoji no represente
  bien.
