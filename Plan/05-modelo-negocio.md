# Modelo de negocio — Lula

## Estrategia de lanzamiento

```
1. Construir para uso propio (Giancarlo como primer usuario real)
2. Validar con 10-30 personas cercanas, sin cobrar, durante 1-2 meses
3. Publicar versión gratuita pública
4. Introducir Premium cuando se sepa qué funciones generan retención real
```

No monetizar antes de validar qué se usa de verdad.

## Modelo Freemium

Hipótesis de precio a validar: Premium Individual **S/ 6.90–12.90/mes** (plan anual a
descuento); Premium Familia, precio mayor, a definir según cuántas personas incluye.

| Función | Gratis | Premium Individual | Premium Familia |
|---|---|---|---|
| Hábitos | Limitados (N a definir con datos de uso real) | Ilimitados | Ilimitados |
| Progresión automática de hábitos | — | ✅ | ✅ |
| Tareas y calendario | Básico | Completo | Completo |
| Finanzas | Registro manual simple | + Presupuestos y metas de ahorro | + Gastos compartidos |
| Checklist diario / Cierre del día | ✅ | ✅ | ✅ |
| Rachas y constancia | ✅ | ✅ | ✅ |
| Revisión semanal y mensual | — | ✅ | ✅ |
| Metas y proyectos | Limitadas (N a definir) | Ilimitadas | Ilimitadas |
| Diario | Básico local | ✅ | ✅ |
| Mi propósito (preguntas guiadas) | ✅ Ilimitado (es solo texto, no genera costo) | ✅ | ✅ |
| Mi propósito — armar y presentar con IA | Hasta 2 veces gratis, después pide Premium | Ilimitado | Ilimitado |
| Asistente conversacional (voz/chat, Fase 2.0) | — | ✅ | ✅ |
| PIN / Zona privada | ✅ (nunca de pago, ver regla abajo) | ✅ | ✅ |
| Dictado de campo | ✅ | ✅ | ✅ |
| Círculo de cuidado | Básico (1 persona a la que acompañar) | — | ✅ Ilimitado |
| Espacio Familia | — | — | ✅ Hasta 5 personas |
| Sincronización cifrada entre dispositivos | — | ✅ | ✅ |
| Copias de seguridad | — | ✅ | ✅ |

**Por qué "Mi propósito" tiene su propio límite, distinto del resto de lo gratis**: es la
primera función que va a usar IA de verdad (llamada a un modelo de lenguaje vía n8n) —
a diferencia de todo lo demás en la fila "Gratis" (que es guardar texto/números, sin costo de
por sí), cada uso de "armar y presentar" cuesta dinero real por llamada. Dar 1-2 usos gratis
alcanza para que la persona vea el valor antes de decidir pagar, sin que el costo de la IA
escale con cada usuario gratuito nuevo sin límite. El resto de las respuestas de "Mi
propósito" (las 8 preguntas en sí) siguen siendo gratis ilimitadas — no cuestan nada,
son solo texto guardado localmente.

### IA Premium (futuro, posterior al asistente conversacional básico)

- Análisis de patrones de comportamiento ("completas mejor la lectura en la mañana")
- Recomendaciones proactivas de ajuste
- Seguimiento de metas financieras con proyecciones
- Más adelante, evaluar IA local (on-device) para funciones de bajo costo de cómputo,
  reduciendo cuánto de esto depende de pagar por cada llamada a un modelo en la nube.

## Regla de diseño técnico ligada al negocio

Todo elemento del modelo de datos incluye el campo `es_premium_feature: boolean`
(ver `01-arquitectura.md`). Esto permite activar/desactivar el freemium con una bandera,
sin reestructurar datos ni código cuando llegue el momento de monetizar.

Las funciones **básicas de privacidad y seguridad nunca deben ser de pago** — proteger
información personal (PIN, cifrado, Zona Privada básica) se mantiene gratuito siempre.

## Diferenciación frente a la competencia

La combinación que le da identidad propia al producto, frente a apps de un solo propósito
(hábitos, tareas, finanzas, o calendario por separado):

```
Organizador personal que crece contigo
   + Mejora continua (ciclo planificar → ejecutar → revisar → ajustar)
   + Círculo de cuidado y familia
```

No se vende como "otra app para marcar hábitos", sino como una aplicación que ayuda a
organizar y mejorar la vida diaria de forma progresiva.

## Ejemplo ilustrativo de proyección (no es una meta, solo referencia de orden de magnitud)

```
10,000 usuarios activos × 3% conversión a Premium = 300 usuarios Premium
300 × S/ 9.90/mes ≈ S/ 2,970 mensuales brutos

100,000 usuarios activos × 3% conversión = 3,000 usuarios Premium
3,000 × S/ 9.90/mes ≈ S/ 29,700 mensuales brutos
```

De estas cifras brutas hay que descontar impuestos, infraestructura, IA, marketing, soporte
y comisiones de las tiendas de aplicaciones. El propósito de este ejemplo es mostrar que no
se necesitan millones de usuarios para empezar a generar ingresos, no proyectar resultados
reales — la conversión real solo se conoce validando con usuarios de verdad.

## Referencia de mercado (contexto, no dato definitivo)

Existen aplicaciones de hábitos y productividad con suscripciones anuales de decenas de
dólares, lo que indica que hay un mercado dispuesto a pagar, aunque con bastante competencia.
Las comisiones de las tiendas de aplicaciones varían según programa y región; conviene
verificar las condiciones vigentes de Google Play y App Store al momento de definir precios
finales, en vez de asumir cifras fijas.

## Oportunidad B2B (mucho más adelante, no planificar aún)

Un eventual "Plan Equipos" orientado a mejora continua + hábitos de equipo + objetivos,
sin competir directamente con herramientas de gestión de proyectos como Trello o Asana.
