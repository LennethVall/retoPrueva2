package service

// Modelo que representa cada evento registrado por la aplicación.
import model.LogEvento

// Importaciones para trabajar con archivos y base de datos SQLite.
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

// Importaciones para transformar XML mediante XSLT y generar HTML.
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Servicio principal encargado de:
 * - registrar eventos,
 * - consultar estadísticas,
 * - guardar persistencia en XML,
 * - generar el informe HTML mediante XSLT.
 *
 * Se declara como object para que exista una sola instancia compartida
 * durante toda la ejecución de la aplicación.
 */
object LogService {

    /*
        Rutas principales de trabajo.

        Se usan rutas relativas para evitar problemas al mover el proyecto
        entre distintos entornos o sistemas operativos.
    */
    private val xmlFile = File("src/main/resources/data/estimulos.xml")
    private val xsltFile = File("src/main/resources/data/xslt/resumen.xslt")
    private val dbFile = File("data.db")

    /*
        Archivos de salida del informe HTML.

        - outputFileDev: salida directa en la raíz del proyecto.
        - outputFileBuild: copia dentro del entorno de compilación de Gradle.
    */
    private val outputFileDev = File("informe_resultados.html")
    private val outputFileBuild = File("build/resources/main/informe_resultados.html")

    /**
     * Bloque de inicialización automática del servicio.
     *
     * Se ejecuta cuando el objeto se carga por primera vez y prepara:
     * - directorios necesarios,
     * - estructura inicial del XML,
     * - driver SQLite,
     * - tabla logs de la base de datos.
     */
    init {
        // Se asegura que exista la carpeta donde se guardará el XML.
        xmlFile.parentFile?.mkdirs()

        /*
            Si el archivo XML no existe o está vacío,
            se crea con una estructura base válida.
        */
        if (!xmlFile.exists() || xmlFile.readText().trim().isEmpty()) {
            xmlFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n</logs>")
        }

        /*
            Carga explícita del driver JDBC de SQLite.
            Esto permite que DriverManager reconozca la conexión a la base de datos.
        */
        Class.forName("org.sqlite.JDBC")

        /*
            Creación de la tabla logs si todavía no existe.
            La tabla almacena cada evento registrado por el usuario.
        */
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

    /**
     * Abre una conexión con la base de datos SQLite.
     *
     * Se usa JDBC con DriverManager y una cadena de conexión
     * apuntando al archivo físico data.db.
     */
    private fun conectar(): Connection =
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")


    // INSERTAR LOG


    /**
     * Registra un evento en la base de datos y también en el XML.
     *
     * De esta forma se mantiene una doble persistencia:
     * - SQLite para consultas rápidas y estadísticas,
     * - XML para transformación posterior con XSLT.
     */
    fun registrarEvento(log: LogEvento) {
        conectar().use { conn ->

            /*
                Inserción segura mediante PreparedStatement.
                Esto permite enviar valores dinámicos a la consulta SQL.
            */
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

        // Además de guardar en base de datos, se añade también al XML.
        guardarEnXML(log)
    }


    // ESTADÍSTICAS USUARIO


    /**
     * Obtiene estadísticas agregadas de un usuario concreto.
     *
     * Devuelve:
     * - media de tiempo de reacción,
     * - mejor tiempo,
     * - peor tiempo,
     * - número total de clics registrados.
     */
    fun obtenerEstadisticasUsuario(usuario: String): Map<String, Any> {
        conectar().use { conn ->

            /*
                Consulta SQL con funciones agregadas:
                - AVG: media
                - MIN: mejor tiempo
                - MAX: peor tiempo
                - COUNT: número de registros
            */
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

        // Si no hay datos, se devuelven valores por defecto.
        return mapOf("media" to 0.0, "mejor" to 0L, "peor" to 0L, "clicks" to 0)
    }


    // MEJOR POR ESTÍMULO


    /**
     * Obtiene el mejor tiempo del usuario para cada tipo de estímulo.
     *
     * El resultado se devuelve como un mapa:
     * clave = nombre del estímulo
     * valor = mejor tiempo registrado
     */
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


    // RANKING TOP 3


    /**
     * Calcula el ranking global de los tres mejores usuarios
     * según su tiempo medio de reacción.
     *
     * Se ordena de menor a mayor media,
     * ya que un tiempo más bajo representa una mejor reacción.
     */
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


    // XML PERSISTENCIA ROBUSTA


    /**
     * Guarda un evento también en el archivo XML.
     *
     * Esto permite mantener una copia estructurada de los datos
     * que luego puede transformarse con XSLT para generar informes.
     */
    private fun guardarEnXML(log: LogEvento) {

        /*
            Si el XML no existe o está vacío,
            se recrea con la estructura mínima válida.
        */
        if (!xmlFile.exists() || xmlFile.readText().trim().isEmpty()) {
            xmlFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n</logs>")
        }

        /*
            Se construye manualmente un nuevo nodo XML con los datos del evento.
        */
        val nuevo = """
            <log>
                <usuario>${log.usuario}</usuario>
                <estimulo>${log.estimulo}</estimulo>
                <tiempo>${log.tiempoReaccionMs}</tiempo>
                <fecha>${log.fecha}</fecha>
            </log>
        """.trimIndent()

        // Se lee el contenido actual del XML.
        var actual = xmlFile.readText().trim()

        /*
            Si existe la etiqueta de cierre </logs>,
            el nuevo nodo se inserta justo antes.
            Si no existe, se reconstruye el documento completo.
        */
        if (actual.contains("</logs>")) {
            actual = actual.replace("</logs>", "$nuevo\n</logs>")
        } else {
            actual = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<logs>\n$nuevo\n</logs>"
        }

        // Se guarda el XML actualizado en disco.
        xmlFile.writeText(actual)
    }


    // TRANSFORMACIÓN XSLT ROBUSTA EN DISCO


    /**
     * Genera un informe HTML a partir del XML usando una plantilla XSLT.
     *
     * Proceso:
     * - comprueba que existan XML y XSLT,
     * - crea el transformador,
     * - genera una salida HTML en desarrollo,
     * - genera otra salida en el entorno build.
     *
     * Esto permite mostrar el informe tanto directamente en el proyecto
     * como desde recursos preparados por Gradle.
     */
    fun generarInformeHtml() {

        // Se aseguran las carpetas de destino antes de escribir archivos.
        outputFileDev.parentFile?.mkdirs()
        outputFileBuild.parentFile?.mkdirs()

        // Mensajes de depuración para verificar rutas reales de trabajo.
        println("📂 Leyendo XML desde: ${xmlFile.absolutePath}")
        println("📂 Leyendo XSLT desde: ${xsltFile.absolutePath}")

        if (xmlFile.exists() && xsltFile.exists()) {
            try {
                /*
                    Se crea una TransformerFactory
                    y a partir de ella un transformador XSLT.
                */
                val factory = TransformerFactory.newInstance()
                val transformer = factory.newTransformer(StreamSource(xsltFile))

                /*
                    Primera transformación:
                    se guarda el informe HTML en la raíz del proyecto.
                */
                transformer.transform(
                    StreamSource(xmlFile),
                    StreamResult(outputFileDev)
                )

                /*
                    Segunda transformación:
                    se guarda una copia en la carpeta build de Gradle.
                */
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

            if (!xmlFile.exists()) {
                println("👉 Falta el archivo XML en: ${xmlFile.absolutePath}")
            }

            if (!xsltFile.exists()) {
                println("👉 Falta el archivo XSLT en: ${xsltFile.absolutePath}")
            }

            throw java.io.FileNotFoundException("No se encontraron los componentes XML/XSLT requeridos.")
        }
    }
}