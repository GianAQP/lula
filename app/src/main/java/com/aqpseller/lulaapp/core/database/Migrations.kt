package com.aqpseller.lulaapp.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones reales de Room, una por cada versión desde que se decidió dejar de depender de
 * `fallbackToDestructiveMigration` (ver `Plan/08-decisiones-tecnicas.md`) — antes de esto, cada
 * versión nueva del esquema borraba toda la base local, aceptable mientras no había nada que
 * perder, pero no aceptable apenas exista un usuario real con datos que le importen. No se
 * escribieron migraciones retroactivas para las versiones 1 a 15 (esa historia es solo de
 * pruebas en dispositivos de desarrollo, sin datos reales de nadie que proteger) — de acá en
 * adelante, todo cambio de esquema necesita su propia `Migration`, agregada a
 * `DatabaseModule.provideLulaDatabase`.
 */

/** Notas gana `titulo` (opcional) y `orden` (manual, flechas ▲▼) — ver `08-decisiones-tecnicas.md`. */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE nota ADD COLUMN titulo TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE nota ADD COLUMN orden INTEGER NOT NULL DEFAULT 0")
    }
}

/** Tabla nueva para Propósito personal — ver `08-decisiones-tecnicas.md`. */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `proposito_personal` (" +
                "`espacioId` TEXT NOT NULL, `propietario` TEXT NOT NULL, " +
                "`respuestasJson` TEXT NOT NULL, `fechaEdicion` INTEGER NOT NULL, " +
                "PRIMARY KEY(`espacioId`), " +
                "FOREIGN KEY(`espacioId`) REFERENCES `espacio`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
    }
}

/** Usuario gana consentimientos (mayoría de edad, términos, datos de salud) y se crea la tabla
 * `conexion` — ver `Plan/11-cuentas-y-conexiones.md`. */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE usuario ADD COLUMN confirmoMayorDe13 INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE usuario ADD COLUMN terminosAceptadosEn INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE usuario ADD COLUMN consentimientoDatosSaludEn INTEGER DEFAULT NULL")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `conexion` (" +
                "`id` TEXT NOT NULL, `usuarioA` TEXT NOT NULL, `usuarioB` TEXT NOT NULL, " +
                "`tipo` TEXT NOT NULL, `origenSolicitudId` TEXT, `fechaConexion` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conexion_usuarioA` ON `conexion` (`usuarioA`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conexion_usuarioB` ON `conexion` (`usuarioB`)")
    }
}

/** Medicamento gana `cantidadDosisTotal` — permite calcular `fechaFin` sola a partir de cuántas
 * dosis recetó el médico, en vez de tener que elegir una fecha a mano. Ver
 * `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medicamento_detalle ADD COLUMN cantidadDosisTotal INTEGER DEFAULT NULL")
    }
}

/** Meta gana `ultimoHitoCelebrado` — evita repetir la misma tarjeta de "llegaste al 50%" cada
 * vez que se recompone Hoy. Ver `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meta ADD COLUMN ultimoHitoCelebrado INTEGER NOT NULL DEFAULT 0")
    }
}

/** Medicamento gana recordatorio persistente (insiste cada N minutos hasta marcar la toma o que
 * termine el día) — ver `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medicamento_detalle ADD COLUMN recordatorioPersistente INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE medicamento_detalle ADD COLUMN intervaloPersistenciaMin INTEGER DEFAULT NULL")
    }
}

/** Cita gana modo "curso" (varias sesiones, ej. radioterapia/masajes) — nueva tabla
 * `sesion_cita` (una fila por sesión, mismo principio que `toma_medicamento`), ver
 * `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cita_detalle ADD COLUMN esCurso INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cita_detalle ADD COLUMN diasSemanaJson TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE cita_detalle ADD COLUMN horaSesion TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE cita_detalle ADD COLUMN fechaInicioCurso INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE cita_detalle ADD COLUMN cantidadSesionesTotal INTEGER DEFAULT NULL")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sesion_cita` (" +
                "`id` TEXT NOT NULL, `actividadId` TEXT NOT NULL, `numeroSesion` INTEGER NOT NULL, " +
                "`fecha` INTEGER NOT NULL, `fechaOriginal` INTEGER NOT NULL, `horario` TEXT NOT NULL, " +
                "`estado` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`actividadId`) REFERENCES `actividad`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sesion_cita_actividadId_numeroSesion` ON `sesion_cita` (`actividadId`, `numeroSesion`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_cita_fecha` ON `sesion_cita` (`fecha`)")
    }
}

/** Lista (el título, no los ítems de adentro) gana `orden` para las flechas ▲▼ — el usuario
 * aclaró que quería reordenar los títulos de lista, no los ítems de cada lista (esos se
 * autoordenan por marcado, ver `08-decisiones-tecnicas.md`). */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lista ADD COLUMN orden INTEGER NOT NULL DEFAULT 0")
    }
}

/** Corrige `MIGRATION_22_23`: todas las listas que ya existían quedaron empatadas en
 * `orden = 0`, así que intercambiar el orden entre dos de ellas era un no-op — el usuario
 * probó las flechas nuevas y encontró que la primera de la lista "no retrocedía" mientras las
 * de más abajo sí (esas ya estaban junto a una lista creada después de la migración, con un
 * `orden` distinto). Reparte un `orden` único por espacio, respetando el orden que ya se veía
 * en pantalla (`fechaCreacion` DESC, como ordenaba la consulta antes de esta ronda) para que no
 * salte nada de lugar al aplicar esta migración. Ver `08-decisiones-tecnicas.md`. */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE lista SET orden = (
                SELECT COUNT(*) FROM lista AS l2
                WHERE l2.espacioId = lista.espacioId
                  AND (l2.fechaCreacion > lista.fechaCreacion
                       OR (l2.fechaCreacion = lista.fechaCreacion AND l2.id > lista.id))
            )
            """,
        )
    }
}

/** Meta gana `categoria` (a cuál de las 6 preguntas de ayuda responde, para agrupar las
 * completadas) y `avisarAlVencer` (aviso sonoro el día que llega la fecha límite) — pedido
 * explícito del usuario. Ver `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meta ADD COLUMN categoria TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE meta ADD COLUMN avisarAlVencer INTEGER NOT NULL DEFAULT 0")
    }
}

/** Reemplaza el aviso booleano (`avisarAlVencer`) por un nivel completo de recordatorio
 * (Silencioso/Sonido/Alarma), igual que Hábito/Tarea/Medicamento/Cita — pedido explícito del
 * usuario para que Metas se sienta "ordenado tipo medicamentos". La columna vieja se deja sin
 * usar (Room no la borra en una migración simple de `ALTER TABLE`, y no vale la pena reconstruir
 * la tabla por una columna huérfana e inofensiva). Ver `Plan/08-decisiones-tecnicas.md`. */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meta ADD COLUMN nivelRecordatorio TEXT NOT NULL DEFAULT 'SONIDO'")
    }
}
