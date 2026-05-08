package service

import model.LogEvento
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object LogService {

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
    // GUARDAR LOG
    // =========================
    fun registrarEvento(log: LogEvento) {
        conectar().use { conn ->
            val sql = """
                INSERT INTO logs (usuario, evento, estimulo, tiempoReaccionMs)
                VALUES (?, ?, ?, ?)
            """.trimIndent()

            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, log.usuario)
            stmt.setString(2, log.evento)
            stmt.setString(3, log.estimulo)
            stmt.setLong(4, log.tiempoReaccionMs)
            stmt.executeUpdate()
        }
    }

    // =========================
    // 📊 ESTADÍSTICAS USUARIO
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

        return mapOf(
            "media" to 0,
            "mejor" to 0,
            "peor" to 0,
            "clicks" to 0
        )
    }

    // =========================
    // 🎯 MEJOR POR ESTÍMULO
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
    // 🏆 RANKING GLOBAL TOP 3
    // =========================
    fun rankingTop3(): List<Map<String, Any>> {

        val lista = mutableListOf<Map<String, Any>>()

        conectar().use { conn ->

            val stmt = conn.createStatement()

            val rs = stmt.executeQuery(
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
}