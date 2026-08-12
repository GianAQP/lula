# Asistente conversacional — Lula (Fase 2.0)

## Diferencia clave: dos niveles de voz, no confundir

```
Dictado de campo (disponible desde el MVP 0.1)
  Voz → texto plano dentro de un campo existente → usuario revisa/edita → guarda
  No interpreta nada, solo transcribe. Usa el reconocimiento de voz nativo del sistema.

Asistente conversacional (fase 2.0)
  Voz o texto → Lula interpreta la intención → extrae datos estructurados
  → ejecuta la acción (crea/modifica/consulta) → confirma en lenguaje natural
```

## Arquitectura (reutiliza el patrón ya validado en Ernesto)

```
Entrada de voz/texto
   ↓
Modelo de lenguaje extrae: { intención, tipo_actividad, campos }
   ↓
Webhook → n8n (o backend propio más adelante)
   ↓
Se crea/modifica el registro correspondiente (cualquier tipo de ACTIVIDAD, no solo finanzas)
   ↓
Lula confirma en lenguaje natural
```

La diferencia frente al prototipo de Ernesto (que hoy solo llena `Balance`) es que aquí el
mismo patrón se generaliza a cualquier `tipo` de `Actividad` del modelo de datos completo.
No es una reconstrucción, es una extensión del mismo esquema ya probado.

**Solución al problema ya identificado**: el modo voz de ChatGPT no dispara Actions de forma
confiable. Al tener una interfaz propia (no depender del modo voz de un tercero), el botón
de micrófono en la app llama directamente al reconocimiento de voz nativo del sistema — mismo
mecanismo que el dictado de campo — eliminando la dependencia de un disparador externo.

## Los 4 modos (validados ya en producción en Mayia)

### 1. Manos libres
Conversación completa por voz: Lula escucha, responde hablando, sigue escuchando.
Uso típico: cocinando, ejercitándose, sin poder mirar la pantalla.

### 2. Dictado + edición
Se transcribe lo dicho, el usuario revisa y edita antes de que se ejecute la acción.
Uso típico: cuando se quiere confirmar que se entendió bien antes de ejecutar algo
sensible (ej. un gasto de monto alto).

### 3. Silencioso
El usuario habla o escribe, Lula responde solo con texto, sin voz.
Uso típico: de noche, en una reunión, en la calle — necesario y fácil de pasar por alto.

### Filtro de ruido — no es un modo, es una capacidad base
El reconocimiento de voz debe funcionar igual en cualquier ambiente (ruido de calle,
mercado, casa con gente), sin que el usuario tenga que activarlo. Pensarlo como el filtrado
que ya hace una llamada telefónica en altavoz: siempre activo, en manos libres y en dictado
por igual. No es una opción de configuración, es comportamiento por defecto del motor de voz.

## Reglas de comportamiento

- **Ejecutar directo si la intención es clara.** Ejemplo: "Gasté 15 soles en el almuerzo" →
  se registra sin pedir confirmación.
- **Pedir confirmación solo ante ambigüedad real o acción sensible.** Ejemplo: dos
  medicamentos con horarios cercanos, compartir algo con alguien, eliminar un registro,
  montos altos de dinero.
- **Transcripción siempre visible**, incluso en modo manos libres con voz activada — el
  usuario puede verificar qué se entendió sin tener que escuchar de nuevo, y corregir por
  texto si algo salió mal.
- **Si no entiende, ofrecer una acción concreta**, no solo avisar el fallo. Ejemplo:
  en vez de "No entendí eso", usar "No entendí. ¿Querías registrar un gasto o marcar un hábito?".
- **Confirmar acciones sensibles explícitamente**, igual que en el resto de la app (compartir
  siempre pasa por `SOLICITUD_COMPARTIR`, nunca es automático desde el asistente tampoco).

## Ejemplos de interacción

**Crear (comando directo):**
```
Usuario: "Anota que hice ejercicio hoy"
Lula: "Listo, ejercicio marcado como completado. Llevas 5 días seguidos 🔥"
```

**Crear con datos parciales (Lula pregunta lo que falta):**
```
Usuario: "Recuérdame comprar el remedio de mamá"
Lula: "¿Para qué día te recuerdo?"
Usuario: "Mañana"
Lula: "Listo, mañana a las 9 a.m. te recuerdo comprar el remedio de mamá."
```

**Consultar (lectura, no escritura):**
```
Usuario: "¿Qué me falta hacer hoy?"
Lula: "Te faltan dos: leer 5 minutos y registrar tus gastos."
```

**Medicamentos:**
```
Usuario: "Ya tomé mi medicamento"
Lula: "Marqué como tomada la dosis de las 2:00 p.m. de {medicamento}."
```

**Acción sensible con confirmación:**
```
Usuario: "Comparte mi medicamento con mi hermana"
Lula: "¿Envío la solicitud a tu hermana para que vea el seguimiento de {medicamento}?"
[Sí, enviar] [No]
```

## Modo sin conexión

```
Usuario habla/escribe sin internet
   ↓
Lula intenta interpretar con un modelo local ligero (ej. Whisper para voz, modelo
ligero tipo Llama para texto)
   ↓
Si logra interpretar con certeza → guarda local (sync_status: pendiente),
   sincroniza cuando vuelva la conexión
Si no logra certeza → guarda como nota pendiente de revisión, avisa al usuario que se
   confirmará cuando haya internet
```

## Punto de entrada en la navegación

No es una pestaña fija obligatoria — vive como una de las posiciones configurables de la
barra inferior (ver `02-pantallas.md`), y también dentro del menú `+` como
"🎙️ Hablar con Lula". En el valor por defecto de navegación, el Asistente ocupa la
posición 2, dado que la evidencia de uso real en Mayia indica que es la función más usada.

## Consistencia con el resto del ecosistema

Mantener los mismos 4 modos, mismos íconos y mismo comportamiento entre Lula, Mayia y los
demás asistentes del ecosistema (Billy, Kira, Ernesto) — un usuario que usa más de uno no
debería tener que reaprender nada.

## Monetización asociada

- **Gratis**: registro manual (formularios), dictado de campo.
- **Premium**: asistente conversacional completo (interpretación de intención por voz/chat).
- **IA Premium** (más adelante): además de registrar, analiza patrones y sugiere ajustes
  proactivamente.
