# Onboarding — Lula

Principio: corto, sin fricción, sin pedir datos que no se van a usar de inmediato.
Máximo 5 preguntas de conocimiento, todas de un toque, sin escribir.

## Flujo estándar (usuario nuevo, sin invitación previa)

### 1. Bienvenida

```
[Logo/ícono de Lula]
"Hola, soy Lula."
"Vamos a organizar tu día a día, poco a poco."
[Botón: Empezar]
```

### 2. Cuenta

```
"¿Cómo quieres entrar?"
[Continuar con Google]   ← prioritario, cero fricción
[Continuar con correo]
(texto pequeño: "Aceptas nuestros Términos y Política de Privacidad")
```

Si Google: correo, nombre y foto llegan automáticos.
Si correo: se pide correo → se envía enlace mágico → pantalla de espera
("Revisa tu correo, te enviamos un enlace para entrar") → confirma con un toque.

No se usan contraseñas clásicas. Este mismo mecanismo sirve como recuperación de cuenta:
Google siempre recupera con Google; correo siempre recupera pidiendo un nuevo enlace mágico.

### 3. Permisos y privacidad

```
"Tus datos están seguros"
"Guardamos tu información de forma segura y cifrada. Tú decides qué compartir y con quién."
[Checkbox: He leído y acepto la Política de Privacidad] [Ver política]
[Botón: Continuar]  (deshabilitado hasta marcar el checkbox)
```

Permisos técnicos del sistema (notificaciones, micrófono, cámara) **no se piden aquí** —
se piden en el momento real en que se necesitan (ej. notificaciones al activar el primer
hábito con horario), con mejor tasa de aceptación por tener contexto claro.

### 4. Preguntas para conocerte (una por pantalla, con indicador de progreso)

**4a — Qué mejorar:**
```
"¿Qué quieres mejorar primero?"
(elige hasta 2)
[Organización] [Salud y hábitos] [Finanzas] [Lectura y aprendizaje]
[Siguiente]
```

**4b — Cómo empezar:**
```
"¿Cómo prefieres empezar?"
[Con pocas cosas simples] [Con un plan más completo]
[Siguiente]
```

**4c — Momento del día:**
```
"¿Qué momento del día usas más para organizarte?"
[Mañana] [Durante el día] [Noche]
[Siguiente]
```

**4d — Cómo le habla Lula:**
```
"¿Cómo prefieres que te hable Lula?"
[campo editable, pre-llenado con el nombre real, ej. "Giancarlo"]
(texto pequeño: "Puedes escribir cómo prefieras que te llame")
[Siguiente]
```
Este valor se guarda como `nombre_preferido`, separado de `nombre_completo`.

**4e — Opcional, para incentivar retención:**
```
"¿Por qué quieres empezar hoy?"
[Quiero ser más constante] [Quiero organizar mi día a día] [Quiero controlar mis finanzas]
```
Se guarda para usarlo después en mensajes motivacionales personalizados.

### 5. Hábitos sugeridos

```
"Te sugerimos empezar con esto:"
[✓] {hábito 1}
[✓] {hábito 2}
[✓] {hábito 3}
(cada uno con ✕ para quitarlo, "+ Agregar otro hábito" abajo)
[Confirmar]
```

Cantidad y elección de hábitos sugeridos depende de las respuestas 4a/4b
(2 hábitos si eligió "pocas cosas simples", 4-5 si eligió "plan más completo").

### 6. Resumen

```
"Listo, {nombre_preferido}."
"Armamos tu primer día. Vamos paso a paso."
[Botón: Empezar mi día]
```

### 7. Entra a Hoy

Ya con las actividades del onboarding cargadas — el usuario ve resultado inmediato antes
de tener que hacer nada más.

---

## Flujo especial — usuario llega por invitación

Ocurre cuando alguien recibe una `SOLICITUD_COMPARTIR` (círculo de cuidado o espacio familia)
y no tiene la app instalada. Ver mecanismo completo en `01-arquitectura.md`.

```
1. Bienvenida — personalizada:
   "{Nombre de quien invita} te invitó a Lula para {contexto de la solicitud}"
2. Cuenta (igual que el flujo estándar)
3. Permisos y privacidad (igual)
4. → Salta directo a mostrar la solicitud pendiente:
   "{Nombre} compartió contigo: {elemento}"
   [Aceptar] [Rechazar]
5. Después de responder → recién ahí pasa por las preguntas normales (paso 4 del flujo
   estándar), para armar su propia experiencia en la app
6. → Hoy
```

Razón del orden: reducir fricción justo en el momento donde más importa (ver rápido por
qué fue invitada la persona), dejando la personalización para después, cuando ya está
dentro y con motivación de haber completado la acción principal.

## Datos pedidos — resumen

| Dato | Cuándo se pide | Obligatorio |
|---|---|---|
| Correo | Paso 2 (Cuenta) | Sí |
| Nombre completo | Automático (Google) o Bienvenida (correo) | Sí |
| Nombre preferido | Paso 4d | No (default = nombre completo) |
| Qué quiere mejorar | Paso 4a | No, pero recomendado |
| Preferencia de horario | Paso 4c | No |
| Fecha de nacimiento | Nunca en onboarding | Solo si hay una razón de producto concreta más adelante |
| Género | Nunca se pide directamente | Se resuelve indirectamente vía "cómo te habla Lula" |
| Teléfono | Nunca en onboarding | Solo si se implementa notificación por SMS/WhatsApp más adelante |
