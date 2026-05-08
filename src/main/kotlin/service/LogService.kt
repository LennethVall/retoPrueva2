package service

import model.LogEvento
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult

object LogService {

    // =========================
    // ARCHIVOS XML / XSLT
    // =========================
    private val xmlFile = File("src/main/resources/data/eventos.xml")
    private val xsltFile = File("src/main/resources/data/xslt/resumen.xslt")

    // =========================
    // BASE DE DATOS
    // =========================
    private val dbFile = File("data.db")

    init {
        Class.forName("org.sqlite.JDBC")

        conectar().use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT,
                    evento TEXT,
                    estimulo TEXT,
                    tiempoReaccionMs INTEGER
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
                INSERT INTO logs (usuario, evento, estimulo, tiempoReaccionMs)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            )

            stmt.setString(1, log.usuario)
            stmt.setString(2, log.evento)
            stmt.setString(3, log.estimulo)
            stmt.setLong(4, log.tiempoReaccionMs)

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
                    "media" to rs.getDouble("media"),
                    "mejor" to rs.getLong("mejor"),
                    "peor" to rs.getLong("peor"),
                    "clicks" to rs.getInt("clicks")
                )
            }
        }

        return mapOf("media" to 0, "mejor" to 0, "peor" to 0, "clicks" to 0)
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
                resultado[rs.getString("estimulo")] = rs.getLong("mejor")
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
                lista.add(
                    mapOf(
                        "usuario" to rs.getString("usuario"),
                        "media" to rs.getDouble("media")
                    )
                )
            }
        }

        return lista
    }

    // =========================
    // XML SIMPLE
    // =========================
    private fun guardarEnXML(log: LogEvento) {

        if (!xmlFile.exists()) {
            xmlFile.parentFile.mkdirs()
            xmlFile.writeText("<logs></logs>")
        }

        val nuevo = """
            <log>
                <usuario>${log.usuario}</usuario>
                <evento>${log.evento}</evento>
                <estimulo>${log.estimulo}</estimulo>
                <tiempo>${log.tiempoReaccionMs}</tiempo>
            </log>
        """.trimIndent()

        val actual = xmlFile.readText()
        xmlFile.writeText(actual.replace("</logs>", "$nuevo</logs>"))
    }

    // =========================
    // XSLT → HTML
    // =========================
    fun generarInformeHtml() {

        val outputFile = File("informe_resultados.html")

        if (xmlFile.exists() && xsltFile.exists()) {

            val transformer = TransformerFactory.newInstance()
                .newTransformer(javax.xml.transform.stream.StreamSource(xsltFile))

            transformer.transform(
                javax.xml.transform.stream.StreamSource(xmlFile),
                StreamResult(outputFile)
            )
        }
    }
}