package com.aqpseller.lulaapp

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * `AuditLogger.registrar<T>()` serializa `T` con kotlinx.serialization antes de guardarlo en
 * el historial — si a una entidad usada ahí le falta `@Serializable`, no falla al compilar,
 * crashea recién cuando se ejecuta ese código (ya pasó una vez, ver
 * `Plan/08-decisiones-tecnicas.md`). Antes esto dependía solo de acordarse a mano.
 *
 * Escanea el código fuente en texto plano (no reflection ni classpath de Android) — corre
 * como test unitario normal de JVM, con el directorio de trabajo en la carpeta del módulo
 * `app/` (comportamiento estándar de Gradle).
 */
class AuditLoggerSerializableTest {

    @Test
    fun `toda entidad usada con auditLogger registrar tiene Serializable`() {
        val carpetaRepositorio = carpetaFuente("src/main/java/com/aqpseller/lulaapp/data/repository")
        val carpetaEntidades = carpetaFuente("src/main/java/com/aqpseller/lulaapp/data/local/entity")

        val patronUso = Regex("""auditLogger\.registrar<(\w+)>""")
        val nombresUsados = carpetaRepositorio.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { archivo -> patronUso.findAll(archivo.readText()).map { it.groupValues[1] } }
            .toSet()
        check(nombresUsados.isNotEmpty()) {
            "No se encontró ningún uso de auditLogger.registrar<T> en $carpetaRepositorio — " +
                "¿cambió el patrón de uso? Este test necesita ajustarse si es así."
        }

        // Cada entidad vive en un "bloque" separado por línea en blanco dentro de su archivo
        // (@Serializable opcional + @Entity(...) + data class Nombre(...)) — se busca el
        // bloque que declara cada nombre usado y se revisa que tenga la anotación ahí mismo.
        val bloquePorNombre = carpetaEntidades.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { it.readText().split("\n\n") }
            .mapNotNull { bloque ->
                val nombre = Regex("""data class (\w+)\(""").find(bloque)?.groupValues?.get(1)
                nombre?.let { it to bloque }
            }
            .toMap()

        val sinSerializable = nombresUsados.filter { nombre ->
            val bloque = bloquePorNombre[nombre]
            bloque == null || !bloque.contains("@Serializable")
        }

        if (sinSerializable.isNotEmpty()) {
            fail(
                "Estas entidades se usan con auditLogger.registrar<T> pero les falta " +
                    "@Serializable (o no se encontró su definición en data/local/entity) — " +
                    "van a crashear en tiempo de ejecución: ${sinSerializable.sorted()}",
            )
        }
    }

    private fun carpetaFuente(subruta: String): File {
        val dir = File(subruta)
        check(dir.isDirectory) {
            "No se encontró ${dir.absolutePath} — este test asume que Gradle corre los tests " +
                "con directorio de trabajo = la carpeta del módulo app/ (comportamiento estándar)."
        }
        return dir
    }
}
