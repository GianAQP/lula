# Especificación de pantallas — Lula

> Actualizado 2026-08-08 para que cuadre con el código real (auditoría). Las secciones sin
> nota de fecha siguen siendo diseño vigente, verificado contra el código en esta fecha.

## Navegación

### Barra inferior (5 posiciones, solo íconos)

```
[ Hoy 🏠 ]  [Posición 2]  [ + ]  [Posición 3]  [Posición 4]
```

- **Hoy**: fija, siempre presente, posición 1.
- **Posiciones 2, 3, 4**: configurables. Opciones disponibles: 🎙️ Asistente, ✅ Hábitos,
  📊 Progreso, 💰 Finanzas, 📅 Tareas, 🎯 Metas, 👥 Círculo de cuidado.
- **`+` central**: no configurable, es un menú de acción rápida, no una sección.

**Valor por defecto** (antes de que el usuario personalice):
```
Hoy | 🎙️ Asistente | + | ✅ Hábitos | 💰 Finanzas
```

### Menú `+` (modal/bottom sheet, no pantalla completa)

Lista real (`navigation/AddMenuSheet.kt`), todas con flujo real conectado:

```
¿Qué quieres agregar?
✅ Hábito · 📝 Tarea · 🧩 Rutina · 🎯 Meta · 📋 Lista ·
📉 Gasto · 📈 Ingreso · 🗒️ Nota · 💊 Medicamento · 📅 Cita ·
🎉 Fecha importante · 📓 Diario
🎙️ Hablar con Lula   (placeholder, Fase 2.0 — hoy lleva a "Próximamente")
```

Nota/Diario pasan primero por el gate de Zona Privada, no van directo al editor.

### Fila de espacio activo (bajo la barra de estado, no siempre visible)

Cuando el espacio activo no es Personal, una banda de color propio ("👨‍👩‍👧 Estás en
{espacio}") aparece arriba de todo, en cualquier pantalla — no solo en Hoy. Toca para volver a
Personal. Ver `08-decisiones-tecnicas.md`, 2026-07-30.

### Fila de stats (🔥/💰), fija en todas las pantallas, no solo Hoy

`LulaTopBar` — mismo nivel que el menú "⋮", visible siempre: `🔥 {racha} días` (toca → abre
Historial) y `💰 S/ {gastos de HOY}` (toca → abre Finanzas; **solo egresos de hoy**, no un
balance acumulado ni ingresos). Un ícono "📩" aparece si hay una invitación pendiente (hoy
siempre 0, sin backend).

### Esquina superior — tres puntos (⋮)

Menú real (`LulaTopBar`), más simple que el diseño original — no hay agrupado en
CUENTA/CONFIGURACIÓN/AYUDA, ni "Cerrar sesión"/"Preguntas frecuentes"/"Sincronización"
(no hay login real todavía, ver `12-firebase-auth-y-sync.md`):

```
🧑 Mi perfil          → datos personales, consentimientos, "Eliminar mi cuenta"
👥 Mi círculo de cuidado
📓 Diario
👨‍👩‍👧 Familia / Espacios
⚙️ Ajustes            → ver sección "Ajustes" más abajo
```

### Patrón de formulario compacto (2026-08-06)

Las pantallas "Crear X" con varios campos de configuración (Frecuencia, Recordatorio,
Duración...) usan una fila colapsada tipo `"🔔 Recordatorio — Cada 8h desde 08:00 ›"`
(`core/ui/SelectorRow.kt`) que al tocarla abre un `ModalBottomSheet` con el contenido completo,
en vez de mostrar todo desplegado siempre. Aplicado en Crear Medicamento, Tarea, Hábito, Cita,
Fecha importante — **no** en Lista/Rutina/Meta/Movimiento (financiero), que son formularios
simples de 1-2 campos donde el patrón no aporta.

**Protección "salir sin guardar"**: las 10 pantallas "Crear X" de la app interceptan el botón
de atrás cuando hay contenido sin guardar (comparando un snapshot del formulario contra su
estado inicial) y piden confirmación antes de descartar — cubre tanto crear algo nuevo como
editar algo existente. Ver `core/ui/DescartarCambiosAlSalir.kt`.

---

## Onboarding

Ver detalle completo en `06-onboarding.md`. Resumen de pasos:
Bienvenida → Cuenta → Permisos y privacidad → Preguntas (4-5) → Hábitos sugeridos →
Resumen → Hoy.

---

## Hoy (pantalla principal)

**Estado real actual** (`features/home/HomeScreen.kt`) — la racha/gastos ya no viven acá, se
movieron a `LulaTopBar` (fija en toda la app, ver "Navegación" arriba):

```
👨‍👩‍👧 Tienes N pendiente(s) en tu espacio Familia   (solo si aplica, ver 08-decisiones-tecnicas.md)

Progreso de hoy
████████░░ {%} — {completadas} de {total}
   (total = hábitos + tareas de hoy + citas de hoy; medicamentos NO cuentan acá, se cuentan
    aparte por toma — ver `actividadCuentaParaHoy`, una sola fuente compartida con Cerrar día)

🎉 ¡Vas al 50% de "{meta}"!  [Genial]        (tarjeta de hito, solo si cruzó 25/50/75/100%)

🎯 Tus metas en progreso     (barra + "⏳ Faltan N días" si está en la última semana o vencida)

🎉 FECHAS IMPORTANTES DE HOY
📅 CITAS DE HOY               (incluye sesiones de un curso de Cita: "{nombre} (sesión N/Y)")
   ⚠️ en rojo si ya pasó su hora y sigue sin marcar

POR LA MAÑANA / POR LA TARDE / POR LA NOCHE
☑/☐ {hábito}                  ⚠️ en rojo si tiene recordatorio vencido sin marcar
TAREAS DE HOY
☑/☐ {tarea}                   ⚠️ en rojo si tiene recordatorio vencido sin marcar

💊 {medicamento} — {horario}  [Tomado] [Omitido]
   ⚠️ en rojo si ya pasó la hora y sigue sin marcar (si tiene "recordatorio persistente"
      activado, además insiste cada N minutos hasta marcarlo o que termine el día)

✅ Ya hechos hoy (n)          (todo lo CONFIRMADO se saca de las listas de arriba, pero sigue
                                acá abajo, con opción de deshacer — nunca se borra ni se castiga)

[Botón fijo abajo: Cerrar mi día]
```

**Estado vacío:**
```
"Todavía no tienes actividades para hoy."
[Botón: Agregar algo para hoy] → abre menú +
[📅 Ver calendario]
```

Comportamiento: al tocar un checkbox, cambia de estado al instante (sin confirmación extra) y
el % de progreso se actualiza en vivo. "Vencido" (⚠️, texto en rojo) es el mismo criterio en
toda la pantalla: `estado == SIN_CONFIRMAR` y ya pasó la hora de recordatorio — nunca se oculta
ni se mueve de lugar, solo se resalta.

**Distinción visual de actividad propia / de apoyo / compartida** (ver `01-arquitectura.md`):
una actividad "de apoyo" (ej. "Recordar medicamento de Mamá") o "compartida" (ej. "Ordenar
sala — con María") debería mostrar un indicador visual distinto (ícono de persona, nombre
pequeño) frente a una actividad puramente personal, para que el usuario distinga de un
vistazo el origen de cada ítem.

**Ícono de estado del día** (mascota/badge, ver `01-arquitectura.md`): visible en la parte
superior de Hoy o como ícono de la app — refleja si el día está en curso, completado,
con pendientes, o si llevan varios días sin abrir la app. Siempre con tono positivo,
nunca de castigo.

---

## Cerrar mi día

```
"Cerremos tu día"

Resumen automático
Hábitos: {x} de {y}
Tareas: {x} de {y}
Finanzas: registradas ✓ / sin registrar

¿Qué logré hoy? (opcional, dictado disponible)
¿Qué costó más? (opcional)
¿Qué ajusto para mañana? (opcional)

Puntuación del día: {n} puntos

[Botón: Guardar y cerrar]
```

Tras guardar: `"Buen trabajo, {nombre}. Mañana seguimos." 🔥 Racha: {n+1} días` → vuelve a Hoy.

**Reabrir el mismo día ya cerrado**: carga las respuestas ya guardadas (`yaExisteRegistro`) en
vez de partir en blanco — antes esto perdía lo ya escrito al reabrir la pantalla (bug real
corregido, ver `08-decisiones-tecnicas.md`, 2026-08-05). El título/botón cambian a "Actualizar
cierre del día" cuando ya existía un registro.

**Revisar cierres de días anteriores**: no vive en esta pantalla — se ve dentro de Calendario,
modo Día, como una tarjeta de solo lectura (`TarjetaCierreDelDia`) si ese día ya se cerró.

---

## Hábitos

**Lista** — rediseño 2026-08-06 (antes: filas planas de texto, sin agrupar):
```
"Tus hábitos"
"Vas muy bien esta semana 💪"   (mensaje motivacional, nunca de reproche ni con 0% — ver abajo)

MAÑANA
┌───────────────────────────────┐
│ 🛏️ Tender la cama        🔥 3 │   ícono automático por palabra clave del nombre
│    L  M  M  J  V  S  [D]      │   letras de día reales, hoy resaltado (borde/negrita)
│    ●  ●  ○  ○  ○  ○  ○        │   racha PROPIA de este hábito, no la racha global
└───────────────────────────────┘
TARDE / NOCHE
   ...

[Botón flotante: + Nuevo hábito]
```

Agrupado por momento del día. La racha de cada tarjeta se calcula de su propio historial
(`calcularRacha`), a propósito distinta de la racha global de `LulaTopBar` (que cuenta días
con "Cerrar mi día" hecho) — así siempre cuadra con los círculos que se ven debajo, evitando la
confusión reportada ("la racha no cuadra con lo que se ve abajo"). El mensaje motivacional se
calcula de la fracción de días cumplidos de la semana visible: ≥70% "Vas muy bien esta semana
💪", >0% "Cada intento cuenta, sigue así 🌱", 0% "Hoy es un buen día para empezar 🙂" (nunca
negativo). "Constancia %" **no** se muestra acá a propósito — ya existe ese concepto en
Progreso (30 días) y hubiera sido un segundo cálculo distinto con el mismo nombre.

**Detalle:**
```
"{nombre del hábito}"
Momento: {mañana/tarde/noche}
Frecuencia: {...}
Racha actual: {n} días
Historial: [visualización de últimos 30 días]
[Editar] [Pausar] [Eliminar]
```

**Crear hábito** (formulario compacto, ver "Patrón de formulario compacto" más abajo):
```
Nombre: [campo]
Momento del día: [Mañana | Tarde | Noche]
⏱️ Duración › (fila colapsada → abre selector con duración inicial + progresión "¿Aumentar con el tiempo?")
🔔 Recordatorio › (fila colapsada → abre selector con hora + qué tan insistente)
[Crear]
```

**Progresión** (aparece como tarjeta dentro de Hoy o Progreso al cumplirse el ciclo de revisión):
```
"Completaste {hábito} {x} de {y} días esta semana."
"Actualmente haces {n} min. ¿Aumentamos?"
[Subir a {n+incremento} min] [Mantener {n} min] [Recordarme después]
```

---

## Progreso

```
"Tu semana"
Cumplimiento: {%}
🔥 Racha máxima: {n} días
📊 Constancia (30 días): {x}/30

Puntos esta semana: {n}

[Revisión semanal completa — ver abajo]
```

**Revisión semanal** (se activa el día configurado, por defecto domingo):
```
"Tu semana"
Cumplimiento general: {%}
{métricas por hábito relevante}

Lo que mejor funcionó
✓ {actividad} — {%}

Lo que costó más
✗ {actividad} — {%}

¿Qué logré esta semana? (opcional, dictado disponible)
¿Qué no funcionó?
¿Qué ajusto la próxima semana?

[Guardar revisión]
```

**Matriz de Eisenhower** (vista/filtro dentro de Progreso o Tareas):
```
HACER (urgente + importante)        PROGRAMAR (importante, no urgente)
DELEGAR (urgente, no importante)    POSPONER (ninguna)
```

---

## Tareas (pantalla dedicada, no estaba en el diseño original)

Accesible desde "Ver todas mis tareas" en Hoy o desde el menú `+`. Dos vistas intercambiables:

```
"Tus tareas"
[Lista]  [🗂️ Matriz]

— Vista Lista —
PENDIENTES                          (vencidas y con fecha próxima primero)
☐ {tarea}
   📅 {fecha}  ⚠️ vencida           (si corresponde — nunca se oculta, solo se resalta)

✅ HECHAS                            (sección aparte, nunca mezclada con pendientes)
☑ {tarea}

— Vista Matriz — igual a "Matriz de Eisenhower" arriba
```

Ver `08-decisiones-tecnicas.md`, 2026-08-07: antes pendientes y hechas estaban mezcladas sin
ningún orden (más nueva creada primero).

## Ajustes (pantalla dedicada, no estaba en el diseño original)

Accesible desde el menú "⋮" → "⚙️ Ajustes":

```
"Ajustes"

🔕 Permitir notificaciones ›          (solo si falta el permiso)
⏰ Permitir alarmas exactas ›         (solo si falta el permiso)
🔋 Permitir que Lula funcione siempre › (solo si falta la exención de batería — Motorola/
                                          Xiaomi/Huawei apagan apps en segundo plano)
🔊 Sonido de mis recordatorios ›       (lleva a los canales de notificación del sistema)

🔔 Sonido al marcar un check en Hoy   [Switch]
🗓️ Día en que se activa Revisión semanal   [L M M J V S D]
🔥 Recordarme cerrar mi día            [Switch] + selector de hora
   (apagado por defecto — si se activa, avisa a la hora elegida solo si el día sigue sin
    cerrarse; se salta solo si ya se cerró. Ver `08-decisiones-tecnicas.md`, 2026-08-07)

🧭 Personalizar mi navegación
   Posición 2 / 3 / 4 de la barra inferior: [chips con las opciones disponibles]
```

---

## Áreas de vida

Vista dentro de Progreso (o accesible desde ahí), agrega el cumplimiento de actividades y
metas por área para que la persona detecte qué está descuidando sin revisar todo en detalle:

```
"Mi progreso por área"

Salud          ████████░░ 80%
Finanzas       ██████░░░░ 60%
Organización   █████████░ 90%
Lectura        ████░░░░░░ 40%

[Configurar mis áreas]
```

El usuario puede activar/desactivar áreas predefinidas (Salud, Finanzas, Aprendizaje, Hogar,
Trabajo, Familia, Personal/espiritual) o crear una propia. El cálculo se basa en el
cumplimiento de las actividades y metas vinculadas a cada área — no requiere captura
adicional del usuario más allá de vincular el área al crear o editar una actividad/meta.

## Línea de vida

Vista de línea de tiempo (fase posterior a 0.5, no MVP) que combina automáticamente entradas
de diario, metas cumplidas y fechas importantes — no es una tabla nueva, es una consulta
sobre datos ya existentes en `ENTRADA_DIARIO`, `META` y `ACTIVIDAD (fecha_importante)`.

```
"Tu línea de vida"

2026
├── 🎯 Alcancé una meta — {meta}
├── ✈️ {entrada de diario con etiqueta "viaje"}
├── 📖 Cumplí 100 días de lectura
├── 🎂 Cumpleaños de Mamá
└── 📝 {entrada de diario}
```

Pensado para generar apego a la app con el tiempo: la persona no solo ve estadísticas, ve
su propia historia de crecimiento. A futuro, el asistente conversacional podría ayudar a
buscar dentro de la línea de vida ("muéstrame mis recuerdos de viajes"), siempre dentro del
espacio privado y con autorización explícita del usuario.

## Metas

```
"Tus metas"
{ícono} {nombre de la meta}
   ████░░░░░░ {progreso} de {objetivo}
   Hábito vinculado: {nombre}    (si aplica)
   ⏳ Faltan N días               (solo la última semana antes de fechaLimite, o si ya venció)

[+ Nueva meta]
```

Lista ordena pendientes primero, con las completadas (100%) separadas debajo de un divisor
"✅ Completadas". En Hoy, cruzar un hito (25/50/75/100%) muestra una tarjeta de celebración una
sola vez ("🎉 ¡Vas al 50%!" + botón "Genial") — `Meta.ultimoHitoCelebrado` evita repetirla.
Ver `08-decisiones-tecnicas.md`, 2026-08-05/06.

**Crear meta:**
```
Nombre: [campo]
Área de vida (opcional): [selector]
Fecha límite (opcional): [selector]
¿Cómo la vas a medir?: [por hábito | por monto | por número | manual]
Vincular hábito o actividad existente (opcional)
[Crear]
```

---

## Finanzas

```
"Finanzas"
Este mes: Ingresos S/{x} — Gastos S/{y} — Balance S/{z}

Gastos de hoy
{categoría} {monto}
Total: S/{n}

[+ Agregar gasto] [+ Agregar ingreso]
Ver historial completo →
```

**Estado vacío:**
```
"Aún no has registrado nada."
"Registrar tus gastos te ayuda a ver a dónde va tu dinero, poco a poco."
[Registrar mi primer gasto]
```

**Formulario:**
```
Tipo: [Gasto | Ingreso]
Monto: [número] S/
Categoría: [selector]
Descripción (opcional): [campo]
Fecha: hoy (editable)
[Guardar]
```

**Historial** (`FinancesHistoryScreen`, navegable mes a mes con ◀/▶/"Hoy", o modo
"📅 Rango de fechas" con dos selectores desde/hasta elegidos a mano):
```
Por mes  |  📅 Rango de fechas
◀  Agosto 2026  ▶     (o "{desde} → {hasta}" en modo rango)
   Hoy

📈 S/ {ingresos}   📉 S/ {gastos}   ⚖️ S/ {balance}     (resumen del período visible)

{categoría}  +/- S/{monto}          {fecha}
   {descripción}                   {día de semana}
```

Cada fila editable/eliminable al tocarla. Nace con `privacidad: solo_yo` — vive dentro de Zona
Privada por defecto.

---

## Zona Privada

**Primera configuración:**
```
"Protege tu espacio privado"
[Usar huella / Face ID] [Usar PIN]
```

**Al entrar (ya configurado):**
```
🔒 "Confirma que eres tú"
[sensor biométrico] o [Usar PIN en su lugar]
```

**Contenido:**
```
"Tu espacio privado"
📓 Diario → últimas entradas, [Ver todas]
💰 Finanzas → acceso directo
📝 Notas privadas → [Ver todas]
⚙️ Elementos marcados como privados manualmente
```

**Diario:**
```
"Diario"
[+ Nueva entrada]
{fecha} — "{título}"
   Área: {área}
   "{extracto}..." [continuar leyendo →]
```

**Nueva entrada:**
```
Título (opcional): [campo]
Área de vida (opcional): [selector]
Fecha: hoy (editable)
[campo de texto libre, grande — con ícono de dictado 🎤]
Foto (opcional): [+ Agregar foto]
[Guardar]
```

Bloqueo automático tras inactividad (configurable, ej. 2-5 min).

---

## Mi salud (fase 0.8, extendida en sesiones posteriores)

```
"Mi salud"

MEDICAMENTOS
💊 {nombre} — {dosis}
   {toma 1} [Tomado] [Omitido]     ⚠️ en rojo si vencida sin marcar

PRÓXIMAS CITAS
📅 {nombre} — {fecha, hora} · {lugar}
🔁 {nombre} (sesión N/Y) — {fecha, hora} · {lugar}      (cita de curso, ver abajo)
   🔁 Van N de Y sesiones

[+ Agregar medicamento] [+ Agregar cita]
```

Una Cita de **curso** (radioterapia, sesiones de masaje...) siempre muestra la fecha de su
**próxima sesión sin marcar**, no una fecha fija — antes de esta corrección desaparecía de la
lista apenas empezaba el curso (bug real, ver `08-decisiones-tecnicas.md`, 2026-08-07).

**Nuevo medicamento** (formulario compacto — ver "Patrón de formulario compacto"):
```
Nombre del medicamento: [campo]
Dosis: [campo]
⏰ Frecuencia ›       (cada cierto número de horas, o según las comidas — igual que antes)
🏁 Termina ›          (sin fecha de fin | fecha elegida | cantidad de dosis total)
🔔 Recordatorio ›     (Silencioso/Sonido/Alarma) + 🔁 Insistir cada cierto tiempo hasta
                       que se marque (recordatorio persistente, minutos configurables — hasta
                       que se marca o termina el día, lo que pase primero)
[Guardar]
```

En Hoy, cada horario aparece como ítem individual con botones `Tomado` / `Omitido` (ya no hay
"Recordar en 15 min" en esa fila — el "insistir" ahora es una opción del medicamento, no una
acción manual puntual). El recordatorio siempre muestra la instrucción original ("después del
almuerzo"), no solo la hora calculada.

**Detalle de medicamento:** sin cambios de fondo respecto al diseño original (nombre, dosis,
horarios, fechas, historial semanal, Editar/Marcar como finalizado).

**Nueva cita:**
```
Nombre / Lugar (opcional) / Motivo (opcional) / Fecha / Hora
🔁 Repetición ›   (Una sola vez | Curso — varias sesiones)
   — si Curso —
   ¿Qué días? [chips L-D, o atajo "Días laborables"]
   Fecha de inicio / Hora de cada sesión
   ¿Cuántas sesiones en total? (vacío = sin cantidad fija, ej. masajes)
🔔 Recordatorios ›   (uno o más, cada uno con su propia anticipación y hora)
[Guardar]
```

**Detalle de cita puntual:** nombre, fecha/hora, lugar, motivo, tareas vinculadas,
[✅ Marcar cumplida] / [⏭️ No se cumplió], [Editar] [Eliminar] [🤝 Compartir seguimiento].

**Detalle de cita de curso:**
```
"{nombre}"
🔁 Van {n} de {total} sesiones          (o "{n} sesiones cumplidas" si no hay cantidad fija)

✅ Sesión 1 — {fecha} · {hora}
   [↩️ Deshacer]  [📅 Reprogramar]
⬜ Sesión 2 — {fecha} · {hora}          ⚠️ si venció sin marcar
   [✅ Cumplida] [⏭️ No se cumplió]  [📅 Reprogramar]
...
[Editar] [Eliminar] [🤝 Compartir seguimiento]
```

Cada sesión tiene color propio por estado (no solo emoji), y reprogramar una sesión **solo
mueve esa sesión** — el resto del curso y el conteo total no se tocan. "Deshacer" vuelve una
sesión marcada por error a pendiente y reprograma su recordatorio si todavía no pasó la hora.

---

## Fechas importantes

```
"Fechas importantes"
🎂 {nombre} — {fecha} ({recurrencia})
[+ Agregar fecha importante]
```

**Crear:**
```
Nombre: [campo]
Fecha: [selector]
Se repite: [Una vez | Cada semana | Cada año]
Recordarme: [mismo día | 1 día antes | 1 semana antes]
Hora del recordatorio: [selector]
Cómo avisarme: [🔔 Alarma con sonido | 💬 Solo notificación]
[Guardar]
```

---

## Mi círculo de cuidado (fase 1.0)

```
"Mi círculo de cuidado"

Personas que acompaño
👤 {nombre}
   {ícono} {actividad} — {hora}
      ✅ Confirmado / ⏳ Pendiente de confirmación
   [si tiene permiso "ver y recordar"]:
      [Marcar como tomado] [Enviar recordatorio]

Quién me acompaña a mí
👤 {nombre} — puede ver y recordar: {elemento}
[Gestionar accesos →]
```

**Enviar solicitud (desde el detalle de cualquier elemento privado):**
```
[Botón: Compartir seguimiento]
   ↓
"Compartir '{elemento}' con..."
[Buscar por correo o contacto]
¿Qué puede hacer esta persona?
   ○ Solo ver el seguimiento
   ○ Ver y recordarme
[Enviar solicitud]
```

Si el destinatario no tiene la app: ver flujo de invitación en `01-arquitectura.md`.

**Recibir solicitud** (notificación + pantalla en Yo → Solicitudes):
```
"{nombre} quiere compartir contigo: {elemento}"
Permiso: {tipo}
[Aceptar] [Rechazar]
```

**Gestionar accesos:**
```
👤 {nombre}
   Acceso a: {elemento} ({permiso})
   [Revocar acceso]
```

---

## Espacio Familia (fase 1.5)

**Crear espacio:**
```
"Crear espacio familiar"
Nombre: [campo]
[Crear]
   ↓
"Invita a tu familia"
[Buscar por correo]
Rol: [Admin | Miembro]
[+ Agregar otro]
[Enviar invitaciones]
```

**Hoy dentro del espacio Familia:**
```
[Familia {nombre} ▾]
"Hoy en {espacio}"

TAREAS DEL HOGAR
☐ {tarea} — {responsable(s)}

RETO FAMILIAR
{ícono} {nombre del reto} — {objetivo}
   Participantes: {lista}
   Progreso: {x} de {y} ya cumplieron hoy

[+ Agregar tarea familiar]
```

**Tarea compartida:**
```
Nombre: [campo]
Responsables: [checkboxes de miembros]
Fecha límite (opcional)
Se completa cuando: [Cualquiera la marque | Todos deben confirmar]
[Crear]
```

**Crear reto familiar:**
```
Nombre: [campo]
Objetivo: [campo]
Frecuencia: [Diaria | Semanal]
Participantes: [selector de miembros]
Recompensa (opcional, texto libre): [campo]
[Crear reto]
```

**Calendario compartido:** vista semanal/mensual con actividades de todos los miembros,
diferenciadas por persona (color/ícono).

**Gastos compartidos:** reutiliza `FINANZAS` con `espacio_id = familia`. Sin división de
cuentas entre personas en el MVP de esta fase.

---

## Asistente (fase 2.0)

Ver especificación completa en `07-asistente-voz.md`.
