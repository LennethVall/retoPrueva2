package service

import model.LogEvento
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object LogService {

    // 🛠️ RUTAS RELATIVAS DIRECTAS: Evitamos concatenar "rootPath" a mano para que el SO no mueva los archivos
    private val xmlFile = File("src/main/resources/data/estimulos.xml")
    private val xsltFile = File("src/main/resources/data/xslt/resumen.xslt")
    private val dbFile = File("data.db")

    // Destinos exactos que tu Application.kt leerá en vivo
    private val outputFileDev = File("informe_resultados.html")
    private val outputFileBuild = File("build/resources/main/informe_resultados.html")

    init {
        // Aseguramos que el directorio de datos exista en el disco
        xmlFile.parentFile?.mkdirs()

        // 🧼 Inicialización limpia y formateada del XML con su cabecera obligatoria
        if (!xmlFile.exists() || xmlFile.readText().trim().isEmpty()) {
            xmlFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n</logs>")
        }

        Class.forName("org.sqlite.JDBC")

        conectar().use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT,
                    estimulo TEXT,
                    tiempoReaccionMs INTEGER,
                    fecha TEXT
                )
                """.trimIndent()
            )
        }
    }

    private fun conectar(): Connection =
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

    // =========================
    // INSERTAR LOG
    // =========================
    fun registrarEvento(log: LogEvento) {
        conectar().use { conn ->
            val stmt = conn.prepareStatement(
                """
                INSERT INTO logs (usuario, estimulo, tiempoReaccionMs, fecha)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            )
            stmt.setString(1, log.usuario)
            stmt.setString(2, log.estimulo)
            stmt.setLong(3, log.tiempoReaccionMs)
            stmt.setString(4, log.fecha)
            stmt.executeUpdate()
        }
        guardarEnXML(log)
    }

    // =========================
    // ESTADÍSTICAS USUARIO
    // =========================
    fun obtenerEstadisticasUsuario(usuario: String): Map<String, Any> {
        conectar().use { conn ->
            val stmt = conn.prepareStatement(
                """
                SELECT 
                    AVG(tiempoReaccionMs) as media,
                    MIN(tiempoReaccionMs) as mejor,
                    MAX(tiempoReaccionMs) as peor,
                    COUNT(*) as clicks
                FROM logs
                WHERE usuario = ?
                """.trimIndent()
            )
            stmt.setString(1, usuario)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "media" to (rs.getDouble("media") ?: 0.0),
                    "mejor" to (rs.getLong("mejor") ?: 0L),
                    "peor" to (rs.getLong("peor") ?: 0L),
                    "clicks" to (rs.getInt("clicks") ?: 0)
                )
            }
        }
        return mapOf("media" to 0.0, "mejor" to 0L, "peor" to 0L, "clicks" to 0)
    }

    // =========================
    // MEJOR POR ESTÍMULO
    // =========================
    fun mejorPorEstimulo(usuario: String): Map<String, Long> {
        val resultado = mutableMapOf<String, Long>()
        conectar().use { conn ->
            val stmt = conn.prepareStatement(
                """
                SELECT estimulo, MIN(tiempoReaccionMs) as mejor
                FROM logs
                WHERE usuario = ?
                GROUP BY estimulo
                """.trimIndent()
            )
            stmt.setString(1, usuario)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val est = rs.getString("estimulo") ?: "DESCONOCIDO"
                resultado[est] = rs.getLong("mejor")
            }
        }
        return resultado
    }

    // =========================
    // RANKING TOP 3
    // =========================
    fun rankingTop3(): List<Map<String, Any>> {
        val lista = mutableListOf<Map<String, Any>>()
        conectar().use { conn ->
            val rs = conn.createStatement().executeQuery(
                """
                SELECT usuario, AVG(tiempoReaccionMs) as media
                FROM logs
                GROUP BY usuario
                ORDER BY media ASC
                LIMIT 3
                """.trimIndent()
            )
            while (rs.next()) {
                lista.add(mapOf("usuario" to rs.getString("usuario"), "media" to rs.getDouble("media")))
            }
        }
        return lista
    }

    // =========================
    // XML PERSISTENCIA ROBUSTA
    // =========================
    private fun guardarEnXML(log: LogEvento) {
        if (!xmlFile.exists() || xmlFile.readText().trim().isEmpty()) {
            xmlFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n</logs>")
        }

        val nuevo = """
            <log>
                <usuario>${log.usuario}</usuario>
                <estimulo>${log.estimulo}</estimulo>
                <tiempo>${log.tiempoReaccionMs}</tiempo>
                <fecha>${log.fecha}</fecha>
            </log>
        """.trimIndent()

        var actual = xmlFile.readText().trim()

        if (actual.contains("</logs>")) {
            actual = actual.replace("</logs>", "$nuevo\n</logs>")
        } else {
            actual = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n$nuevo\n</logs>"
        }
        xmlFile.writeText(actual)
    }

    // =====================================
    // TRANSFORMACIÓN XSLT ROBUSTA EN DISCO
    // =====================================
    fun generarInformeHtml() {
        // Aseguramos la existencia física de las carpetas de salida antes de volcar
        outputFileDev.parentFile?.mkdirs()
        outputFileBuild.parentFile?.mkdirs()

        println("📂 Leyendo XML desde: ${xmlFile.absolutePath}")
        println("📂 Leyendo XSLT desde: ${xsltFile.absolutePath}")

        if (xmlFile.exists() && xsltFile.exists()) {
            try {
                val factory = TransformerFactory.newInstance()
                val transformer = factory.newTransformer(StreamSource(xsltFile))

                // Ejecución 1: Guardamos en la raíz del proyecto para Ktor directo
                transformer.transform(
                    StreamSource(xmlFile),
                    StreamResult(outputFileDev)
                )

                // Ejecución 2: Guardamos en el build activo de Gradle
                transformer.transform(
                    StreamSource(xmlFile),
                    StreamResult(outputFileBuild)
                )

                println("✅ Transformación completada con éxito. Archivos actualizados.")
            } catch (e: Exception) {
                println("❌ Error crítico en el motor XSLT:")
                e.printStackTrace()
                throw e
            }
        } else {
            println("⚠️ Error: Archivos fuente no localizados.")
            if (!xmlFile.exists()) println("👉 Falta el archivo XML en: ${xmlFile.absolutePath}")
            if (!xsltFile.exists()) println("👉 Falta el archivo XSLT en: ${xsltFile.absolutePath}")
            throw java.io.FileNotFoundException("No se encontraron los componentes XML/XSLT requeridos.")
        }
    }
}