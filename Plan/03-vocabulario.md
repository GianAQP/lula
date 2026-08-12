# Vocabulario del dominio — Lula

Glosario de términos usados de forma consistente en el diseño, la documentación y el código.
Mantener estos nombres al nombrar entidades, clases y campos en Kotlin.

| Término | Significado |
|---|---|
| **Actividad** | Entidad genérica que representa cualquier cosa que se hace o se sigue: hábito, tarea, rutina, medicamento, cita, evento o fecha importante. Es la pieza central del modelo de datos. |
| **Espacio** (workspace) | Contexto donde vive una actividad: `personal`, `familia` o `equipo`. Todo usuario tiene un espacio personal por defecto. |
| **Hábito** | Actividad que se repite con una frecuencia definida, puede tener progresión (duración que crece con el tiempo). |
| **Tarea** | Actividad puntual, generalmente con fecha límite. Puede clasificarse como importante/urgente (Matriz de Eisenhower). |
| **Rutina** | Conjunto de actividades agrupadas que se completan como bloque (ej. "Rutina de mañana"). |
| **Meta** | Objetivo de más largo plazo, agrupa una o más actividades como medio para alcanzarla. |
| **Cierre del día** | Ritual diario: resumen del día, preguntas opcionales de reflexión (que funcionan como diario integrado), y cálculo de la puntuación del día. |
| **Revisión semanal** | Ritual semanal equivalente al cierre del día, pero a nivel de semana: qué funcionó, qué no, qué se ajusta. |
| **Racha** | Días consecutivos con el día cerrado y al menos una actividad cumplida. Se rompe si un día no se cumple ninguna condición. |
| **Constancia** | Porcentaje de días activos en los últimos 30 días. Independiente de la racha — no se resetea si se rompe una racha. |
| **Puntuación del día** | Suma de puntos por actividades cumplidas (1 punto cada una, sin techo fijo). |
| **Zona Privada** | Filtro/sección de la app que agrupa todo lo marcado con `privacidad: solo_yo`, protegido con biometría o PIN. |
| **Diario / Bitácora** | Entrada de texto libre asociada a una fecha, integrada al cierre del día o creada de forma independiente. |
| **Línea de vida** | Vista de línea de tiempo que combina entradas de diario, metas cumplidas y fechas importantes — no es una tabla nueva, es una consulta sobre datos existentes. |
| **Círculo de cuidado** | Conjunto de relaciones de cuidado: personas a quienes el usuario acompaña (ve/recuerda sus actividades) y personas que acompañan al usuario. |
| **Solicitud de compartir** | Mecanismo de opt-in explícito: un usuario propone compartir un elemento específico con otra persona, quien debe aceptar para que se active el acceso. |
| **Dictado de campo** | Conversión simple de voz a texto dentro de un campo existente (diario, notas, descripciones). No interpreta intención, solo transcribe. Disponible desde el MVP 0.1. |
| **Asistente conversacional** | Interpretación de intención completa por voz o texto ("anota que hice ejercicio") que ejecuta una acción sin pasar por un formulario. Fase 2.0. |
| **Modo manos libres** | Modo del asistente: conversación completa por voz, Lula responde hablando. |
| **Modo dictado** | Modo del asistente: se transcribe lo dicho, el usuario revisa y edita antes de confirmar la acción. |
| **Modo silencioso** | Modo del asistente: responde solo en texto, sin sonido. |
| **Estado de una actividad** | `confirmado` (se hizo), `sin_confirmar` (no hay registro todavía — nunca se interpreta como "no se hizo"), `omitido` (el usuario indicó expresamente que no lo hizo). |
| **Modo de frecuencia (medicamento)** | `intervalo_horas` (ej. "cada 8 horas") o `relacion_comida` (ej. "después del almuerzo"), con cálculo automático de horarios pero mostrando siempre la instrucción original. |
| **Reto familiar** | Actividad colectiva dentro de un espacio familiar, con objetivo, participantes y recompensa definida libremente por la familia (la app no la sugiere ni la impone). |
| **Local-first** | Principio de arquitectura: todo dato se guarda primero en el dispositivo y funciona sin conexión; se sincroniza a la nube en segundo plano cuando hay internet. |
| **Premium feature** | Campo booleano en cualquier elemento que indica si pertenece al nivel gratuito o de pago, permitiendo activar/desactivar el freemium sin reestructurar datos. |
| **Curso (de Cita)** | Una Cita con varias sesiones repetidas en un patrón de días de la semana (ej. radioterapia, sesiones de masaje), en vez de una cita puntual. `ActividadDetalle.Cita.esCurso`. Agregado 2026-08-06. |
| **Sesión (de curso)** | Una ocurrencia puntual dentro de un curso de Cita — tiene su propio estado y fecha, reprogramable individualmente sin afectar al resto del curso. Entidad `SesionCita`. |
| **Recordatorio persistente** | Opción de un Medicamento: además de sonar una vez, sigue insistiendo cada N minutos hasta que la toma se marque o termine el día. No confundir con el nivel "Alarma" (que suena en loop una sola vez hasta apagarla) — el recordatorio persistente vuelve a sonar cada cierto tiempo. |
| **Vencido/a** | Un ítem con hora de recordatorio (tarea, hábito, cita, medicamento) que ya pasó su hora y sigue `SIN_CONFIRMAR` — se resalta en rojo con ⚠️ en toda la app, nunca se oculta ni se mueve de lugar. |
| **Racha por hábito** | Racha calculada del historial de un hábito puntual (`calcularRacha`), distinta de la racha global de la app (que cuenta días con "Cerrar mi día" hecho). Se ve en cada tarjeta de la lista de Hábitos. |
| **Descartar cambios al salir** | Confirmación que aparece al intentar salir de una pantalla "Crear X" con contenido sin guardar (creando o editando) — evita perder texto ya escrito por tocar atrás sin querer. `core/ui/DescartarCambiosAlSalir.kt`. |
| **Formulario compacto** | Patrón de UI: un bloque de configuración se muestra como una fila colapsada con resumen ("🔔 Recordatorio — Cada 8h desde 08:00 ›") que abre un `ModalBottomSheet` al tocarla, en vez de estar siempre desplegado. `core/ui/SelectorRow.kt`. |

## Nombres de los asistentes del ecosistema (contexto, no parte de Lula en sí)

| Nombre | Producto |
|---|---|
| Ernesto | Asistente por voz del prototipo de finanzas (ChatGPT + n8n → Google Sheets) |
| Mayia | App de ventas ya construida, con asistente de 4 modos (referencia de UX para Lula) |
| Lula | Nombre del proyecto de este documento — app de mejora continua personal |
| Billy, Kira | Otros asistentes del ecosistema personal de Giancarlo, no detallados en este documento |
