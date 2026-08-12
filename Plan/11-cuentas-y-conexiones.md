# Cuentas y conexiones — Lula

Documento de diseño para lo que el usuario llamó "usuarios pendientes" (2026-08-01): qué datos
personales pide Lula al crear una cuenta, qué preguntas legales hacen falta, cómo se conectan
dos personas (familia, amigos, círculo de cuidado), qué preguntas de onboarding ayudan a
conocer/enganchar a alguien nuevo sin mezclarse con "Mi propósito", y qué hace falta para
publicar en Play Store. Se acordó armar este documento **antes** de tocar código — ver
`10-pendientes.md` para el estado real de implementación de cada pieza.

## Por qué este orden

El usuario prefiere terminar de estabilizar el modelo de datos (esto, sumado a lo que ya se
hizo el 2026-07-30/08-01 con Notas, deuda técnica y Mi propósito) **antes** de armar el
workflow de n8n — para no tener que modificarlo dos veces. Este documento es el último tramo
de esa estabilización: una vez construido lo de acá (al menos la parte local-first), recién
tiene sentido diseñar el workflow de n8n con el esquema ya firme.

## 1. Datos personales — tabla `Usuario` ampliada

Base actual, de `01-arquitectura.md`:

```
USUARIO
  id
  nombre_completo
  nombre_preferido
  correo
  metodo_login: google | correo_magico
  privacidad_aceptada_en: fecha
  horarios_comida: { desayuno, almuerzo, cena }        (opcional)
  preferencia_asistente: { modo_defecto }
  configuración_navegación: [posición_2, posición_3, posición_4]
```

Campos nuevos propuestos:

```
USUARIO (agregados)
  confirmo_mayor_de_13: boolean          — checkbox en el registro, no fecha de nacimiento exacta
  terminos_aceptados_en: fecha
  consentimiento_datos_salud_en: fecha?  — null salvo que declare que va a usar Medicamentos/Citas
```

**Por qué no pedir la fecha de nacimiento exacta**: no hace falta para ninguna función de la
app hoy (si alguien quiere que Lula le recuerde su cumpleaños, ya existe Fechas Importantes
para eso, y ahí sí tiene sentido guardar una fecha completa porque es explícitamente lo que
pidió). Pedir menos dato del que se necesita es mejor práctica de privacidad y deja un campo
menos que declarar en el formulario de Data Safety de Play Store. Un checkbox "confirmo que
soy mayor de 13 años" alcanza para lo que Play exige declarar (apta para menores o no).

**Por qué separar `consentimiento_datos_salud_en` del consentimiento general**: Play Store
trata los datos de salud como categoría sensible aparte — medicamentos y citas médicas caen
ahí. Conviene un consentimiento explícito y propio, no mezclado en el checkbox genérico de
privacidad, primero porque es más transparente para la persona, y segundo porque simplifica
declarar esto en Play Console (queda claro qué usuarios lo aceptaron y cuándo).

## 2. Preguntas legales / consentimientos, en el registro

| # | Consentimiento | Obligatorio | Notas |
|---|---|---|---|
| 1 | Política de Privacidad | Sí | Play Store no publica sin esto. Necesita existir como documento real, en una URL pública, antes de subir la app. |
| 2 | Términos de Servicio | Recomendado | Distinto de la política de privacidad: privacidad = qué hacemos con tus datos; términos = reglas de uso (qué pasa si cancelás, qué no está permitido, etc.). Play no siempre lo exige, pero cubre legalmente al negocio. |
| 3 | Datos de salud | Condicional | Solo se pide si la persona declara que va a usar Medicamentos/Citas (o la primera vez que crea uno de esos). |

**Importante**: puedo dejar un borrador razonable de estos textos, pero la redacción final —
sobre todo pensando en la Ley N° 29733 (Ley de Protección de Datos Personales del Perú)— la
debería revisar alguien con conocimiento legal real antes de publicar. Esto no reemplaza esa
revisión.

## 3. Cómo se conectan las personas (familia, amigos, círculo de cuidado)

La pieza central **ya existe**: `SOLICITUD_COMPARTIR` (`01-arquitectura.md`) ya modela pedir y
aceptar una conexión, con `contexto: elemento_compartido | invitación_a_espacio`. Lo que falta
es recordar, **después** de aceptada, que esas dos personas ya están conectadas — hoy cada
solicitud es sobre un elemento puntual (un hábito, una cita, un espacio), no queda un
"contacto" guardado que se pueda listar.

Tabla nueva propuesta:

```
CONEXION
  id
  usuario_a
  usuario_b
  tipo: familia | amigo | cuidador
  origen_solicitud_id      (la SOLICITUD_COMPARTIR que la generó)
  fecha_conexion
```

Reglas:
- Se crea sola la primera vez que se acepta una `SOLICITUD_COMPARTIR` entre dos personas que
  todavía no tenían una `Conexion` (buscar antes de crear, nunca duplicar el par).
- `usuario_a`/`usuario_b` sin orden implícito — un query de "¿estas dos personas están
  conectadas?" tiene que mirar ambos sentidos.
- Sirve para que "Mi círculo de cuidado" y "Espacio Familia" puedan mostrar **personas**
  conectadas (una lista de contactos), no solo elementos compartidos sueltos como hoy.

Igual que el resto de compartir, la parte que involucra a una **segunda persona real** sigue
bloqueada por Firebase/sync — pero el modelo de datos (la tabla, la lógica de creación
automática) se puede construir ya, local-first, mismo criterio que se usó con Círculo de
cuidado y Espacio Familia: queda lista la base, sin simular una segunda persona que no existe.

## 4. Preguntas de onboarding — separadas de Mi propósito

Principio ya acordado (`06-onboarding.md`, `08-decisiones-tecnicas.md` 2026-08-01): el
onboarding de cuenta se queda **corto a propósito**. Mi Propósito es una reflexión más honda
que necesita calma, no algo que se responde en 30 segundos al registrarse — mezclarlos
arriesga que la gente abandone el registro antes de terminar.

Preguntas que ya están bien pensadas en `06-onboarding.md` (mantener tal cual):
- ¿Qué quieres mejorar primero? (organización, salud, finanzas, lectura)
- ¿Cómo prefieres empezar? (pocas cosas simples / plan más completo)
- ¿Qué momento del día usas más para organizarte?
- ¿Cómo prefieres que te hable Lula?
- ¿Por qué quieres empezar hoy? (opcional, para mensajes motivacionales después)

**Mi recomendación (con sombrero de psicólogo): no agregar preguntas nuevas al onboarding.**
Lo que engancha a alguien el primer día no es que le pregunten más — es sentir una victoria
rápida (Hook Model: gatillo → acción chica → recompensa → inversión). Eso ya está bien resuelto
en el flujo actual: hábitos sugeridos al final, resultado visible apenas entra a Hoy. Agregar
preguntas ahí compite contra eso, no lo refuerza.

**El puente hacia Mi Propósito** puede ser más sutil que una pregunta nueva: la respuesta a
"¿por qué quieres empezar hoy?" (ya existente) se puede ofrecer, más adelante, como sugerencia
editable para alguna pregunta de Mi Propósito (ej. "¿qué quiero lograr?") — sin obligar a nada,
solo como punto de partida para no encarar una pregunta en blanco. Esto es una idea a evaluar
cuando se construya la conexión entre onboarding y Mi Propósito, no algo urgente.

## 5. Checklist de Play Store

No todo esto es código — parte es configuración de Play Console y documentos:

- [ ] Política de Privacidad publicada en una URL pública.
- [ ] Términos de Servicio publicados (recomendado).
- [ ] Formulario de **Data Safety** completado en Play Console — declarar exactamente qué se
  recoge (nombre, correo, datos de salud si aplica) y si se comparte con terceros (n8n cuenta
  como "compartido" apenas exista esa conexión).
- [ ] **Eliminar cuenta desde dentro de la app** — política de Google desde 2023: si se permite
  crear cuenta, tiene que poder borrarse desde la app, no solo pidiéndolo por correo. Se puede
  construir ya (hoy borra el usuario semilla local; con cuenta real, debe borrar también del
  lado de Firebase/backend cuando exista).
- [ ] Cuestionario de clasificación de contenido (edad recomendada).
- [ ] Categoría de la app en la ficha de Play Store.

## 6. Qué se puede construir ya (local-first) vs qué sigue bloqueado

**Local-first, buildable ahora:**
- Campos nuevos en `Usuario` (`confirmo_mayor_de_13`, `terminos_aceptados_en`,
  `consentimiento_datos_salud_en`) + pantalla/paso donde se piden.
- Tabla `Conexion` + lógica de creación automática al aceptar una `SolicitudCompartir` (vacía
  en la práctica hasta que haya cuentas reales, pero lista).
- Pantalla "Eliminar mi cuenta" — hoy borraría el usuario semilla y todos sus datos locales;
  cumple igual con lo que pide Play Store aunque no haya backend todavía.

**Bloqueado por Firebase/backend (sin cambios respecto a `10-pendientes.md`):**
- Login real (Google / enlace mágico por correo).
- Que `Conexion` se pueble con una segunda persona real.
- Cualquier cosa que dependa de n8n.

## 7. Orden sugerido de implementación

1. ✅ Ampliar `Usuario` (nuevos campos + migración de Room). — 2026-08-01
2. ✅ Tabla `Conexion` + lógica de creación automática. — 2026-08-01
3. ✅ Pantalla "Eliminar mi cuenta". — 2026-08-01, ver detalle y verificación en
   `08-decisiones-tecnicas.md`.
4. ✅ Borrador de Política de Privacidad / Términos de Servicio + pantalla en la app para
   leerlos antes de aceptar. — 2026-08-01, ver `domain/legal/TextosLegales.kt` y detalle en
   `08-decisiones-tecnicas.md`. Sigue pendiente la revisión legal real del texto (marcado con
   placeholders) antes de publicar.
5. ⬜ Recién ahí, evaluar conectar Firebase Auth real — y con el esquema ya firme, empezar a
   diseñar el workflow de n8n para Mi propósito.
