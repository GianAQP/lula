# Firebase Auth + sincronización — Lula

Documento de diseño para el paso 5 de `11-cuentas-y-conexiones.md`: login real, recuperación de
cuenta, administración de usuarios, y que las conexiones de Familia/Círculo de Cuidado
funcionen entre dos personas de verdad. Decidido con el usuario (2026-08-01): se usa Firebase
(Auth + Firestore) — mismo ecosistema Google, sin armar un backend propio desde cero, y ya
estaba previsto desde `01-arquitectura.md` (`AuthRepository` como interfaz estable,
`Usuario.metodoLogin` con `GOOGLE`/`CORREO_MAGICO`/`LOCAL`, `SyncStatus` en `Actividad`).

Este documento se escribe **antes** de tocar código, mismo criterio que
`11-cuentas-y-conexiones.md` — para no tener que rehacer el armado dos veces.

## 1. Qué NO cambia

- `AuthRepository` (interfaz de dominio) — ya está diseñada para esto, ningún caso de uso ni
  pantalla le habla directo a Firebase.
- El id del usuario semilla actual. Cuando alguien "reclama" su cuenta (agrega correo/Google a
  un usuario que ya tenía datos locales), se actualiza esa misma fila — no se crea una fila
  nueva. Ningún FK en toda la base necesita reescribirse (mismo criterio ya aplicado con
  `Conexion`, ver `08-decisiones-tecnicas.md`).
- Todo lo que hoy vive 100% local (hábitos, tareas, finanzas, diario, notas, Mi propósito,
  Espacio Personal) **se queda 100% local**. Esto no es "sync completo de toda la app" — es
  específicamente lo que hace falta para que Familia/Círculo de Cuidado funcionen entre personas
  reales. Ver sección 3.

## 2. Métodos de login

Ya definidos en el modelo (`MetodoLogin`): **Google Sign-In** y **enlace mágico por correo**
(sin contraseña — Firebase Auth envía un link, la persona lo toca y queda dentro; evita que Lula
tenga que guardar/gestionar contraseñas). Ambos los resuelve Firebase Auth directamente:

- **Recuperación de cuenta**: con Google, no aplica (la cuenta de Google ya tiene su propia
  recuperación). Con correo mágico, "recuperar" es simplemente pedir un nuevo enlace — no hay
  contraseña que resetear, así que este problema (el más común en cualquier app) prácticamente
  desaparece con este método.
- **Administrar usuarios**: para esta etapa (una sola persona por cuenta, sin panel de admin
  para el equipo de Lula) alcanza con lo que Firebase Console ya da gratis: ver usuarios,
  deshabilitar una cuenta, ver métricas de login. No hace falta construir nada nuevo para esto
  todavía.

## 3. Qué sí se sincroniza — el límite es el tipo de Espacio

`Espacio.tipo` ya distingue `PERSONAL` de `FAMILIA` (`01-arquitectura.md`). Ese límite existente
es exactamente el criterio de qué sube a Firestore:

- **Espacios `PERSONAL`**: nunca tocan Firestore. Son de una sola persona, no hay nadie más con
  quien sincronizar, y es la mayoría del contenido sensible (diario, Mi propósito, finanzas
  personales) — sacarlo de la nube por defecto es mejor privacidad, no solo menos trabajo.
- **Espacios `FAMILIA` (y, cuando exista, Círculo de Cuidado)**: se replican en Firestore porque
  por definición tienen más de un miembro — no hay forma de que dos personas en dispositivos
  distintos vean lo mismo sin que viva en algún servidor.
- **`Conexion` y `SolicitudCompartir`**: van a Firestore siempre — son, por definición, el
  registro de una relación entre dos cuentas reales, no tienen sentido guardadas solo en un
  dispositivo.
- **`Usuario`**: un perfil mínimo (nombre, correo, foto si Google la da) se replica a Firestore
  para que otra persona pueda ver "quién te invitó" o "quién está en tu Espacio Familia". El
  resto de los campos de `Usuario` (horarios de comida, consentimientos) se quedan solo locales
  — no le sirven a nadie más.

## 4. Modelo en Firestore (propuesto)

```
usuarios/{usuarioId}                    — perfil mínimo: nombreCompleto, nombrePreferido, correo
conexiones/{conexionId}                 — espejo de la tabla Conexion local
solicitudes_compartir/{solicitudId}     — espejo de SolicitudCompartir
espacios/{espacioId}                    — solo espacios tipo FAMILIA/CUIDADO
espacios/{espacioId}/miembros/{usuarioId}
espacios/{espacioId}/actividades/{actividadId}   — solo lo que se comparte en ese espacio
```

Reglas de seguridad (borrador, a afinar cuando se implemente):
- `usuarios/{id}`: cualquier usuario autenticado puede leer (para mostrar nombre/foto de un
  contacto), solo el dueño puede escribir su propio documento.
- `espacios/{id}/**`: solo lectura/escritura para quien figure en `miembros`.
- `conexiones`/`solicitudes_compartir`: solo lectura/escritura para los dos usuarios
  involucrados (`usuarioA`/`usuarioB` o `origen`/`destino`).

## 5. Migración del usuario semilla

`AsegurarDatosSemillaUseCase` no desaparece — sigue siendo lo que corre en el primer arranque
sin cuenta. Lo nuevo es un flujo de "reclamar cuenta": la persona elige Google o correo mágico,
Firebase Auth devuelve un `uid`, y en vez de crear un `Usuario` nuevo, se actualiza el
`UsuarioEntity` semilla existente (mismo `id` local — no el `uid` de Firebase, para no romper
ningún FK) agregando `correo`, `metodoLogin`, y guardando el `uid` de Firebase en un campo nuevo
(`firebaseUid: String?`) para poder re-autenticar en el futuro. Recién ahí ese usuario y sus
espacios `FAMILIA` (si los tiene) empiezan a subir a Firestore.

## 6. Costos

Plan gratuito de Firebase (Spark): Auth es gratis sin límite de usuarios. Firestore tiene una
cuota diaria gratuita generosa (50k lecturas / 20k escrituras / 20k borrados por día) — de sobra
para el volumen que va a tener Lula mientras no tenga miles de usuarios activos con Espacios
Familia grandes. Si eso cambia, el plan Blaze (pago por uso) solo cobra lo que se pasa de la
cuota gratuita, no un costo fijo. No hace falta tarjeta para arrancar en Spark.

## 7. Qué necesita hacer el usuario (fuera de la app, no lo puede hacer Claude)

Crear el proyecto de Firebase requiere tu cuenta de Google — esto no lo puedo hacer yo. Pasos
exactos:

1. Ir a **console.firebase.google.com**, iniciar sesión con tu cuenta de Google.
2. "Agregar proyecto" → nombre (ej. "Lula") → seguir el asistente (no hace falta Google
   Analytics para esto, se puede omitir).
3. Dentro del proyecto: **Compilación → Authentication → Comenzar** → pestaña "Sign-in method" →
   activar **Google** y **Correo electrónico/contraseña → Vínculo de correo electrónico (sin
   contraseña)**.
4. **Compilación → Firestore Database → Crear base de datos** → modo producción (las reglas de
   seguridad se configuran después, con código) → elegir una región cercana (ej.
   `southamerica-east1`).
5. **Configuración del proyecto (ícono de engranaje) → Tus apps → Agregar app → Android**.
   - Nombre del paquete (`applicationId`): `com.aqpseller.lulaapp`
   - Descargar el archivo **`google-services.json`** que te ofrece al final.
6. Pasarme ese archivo `google-services.json` (puedes copiar su contenido o el archivo en sí) —
   con eso puedo conectar el proyecto al código.

Cuando tengas eso listo, seguimos con la parte de código: agregar las dependencias de Firebase,
`AuthRepositoryFirebaseImpl`, las pantallas de login/registro, y el repositorio de sincronización
de Firestore para `Conexion`/`SolicitudCompartir`/Espacios Familia.

## 8. Orden sugerido de implementación (una vez con el archivo de configuración)

1. Gradle: plugin `google-services` + dependencias de Firebase Auth/Firestore (BoM).
2. `AuthRepositoryFirebaseImpl` (Google + correo mágico) + pantallas de login/registro.
3. Flujo "reclamar cuenta" sobre el usuario semilla (sección 5).
4. Repositorio de sync para `Conexion`/`SolicitudCompartir` (Firestore ↔ Room).
5. Sync de Espacios `FAMILIA` (contenido compartido) — recién acá "aceptar una
   `SolicitudCompartir`" empieza a tener sentido con una segunda persona real.
6. Reglas de seguridad de Firestore, probadas con dos cuentas reales.
7. Verificación en dispositivo real con dos cuentas distintas (dos celulares o un celular +
   emulador) — no alcanza con probar en un solo dispositivo, todo lo nuevo de acá depende de que
   haya una segunda persona de verdad.
