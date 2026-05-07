package service

import model.LogEvento
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.validation.SchemaFactory

object LogService {

    // Rutas (Asegúrate de que las carpetas existan en resources)
    private val dbFile = File("data.db")
    private val xmlFile = File("src/main/resources/data/eventos.xml")
    private val xsdFile = File("src/main/resources/data/eventos.xsd")
    private val xsltFile = File("src/main/resources/data/xslt/resumen.xslt")

    init {
        // Inicializar SQLite
        Class.forName("org.xerial.sqlite-jdbc")
        conectar().use { conn ->
            val sql = """
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT,
                    evento TEXT,
                    estimulo TEXT,
                    tiempoReaccionMs INTEGER,
                    fecha TEXT
                )
            """.trimIndent()
            conn.createStatement().execute(sql)
        }
    }

    private fun conectar(): Connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

    fun registrarEvento(log: LogEvento) {
        // 1. PERSISTENCIA EN SQLITE (Historial)
        conectar().use { conn ->
            val sql = "INSERT INTO logs (usuario, evento) VALUES (?, ?)"
            val pstmt = conn.prepareStatement(sql)
            pstmt.setString(1, log.usuario)
            pstmt.setString(2, log.evento)
            pstmt.setString(3, log.estimulo)
            pstmt.setLong(4, log.tiempoReaccionMs)
            pstmt.setString(5, log.fecha)
            pstmt.executeUpdate()
        }

        // 2. PERSISTENCIA EN XML (Resumen acumulado)
        val doc = cargarDocumento()
        val root = doc.documentElement

        // Lógica de Usuario: ¿Existe ya?
        var usuarioElem = root.childNodes.let { list ->
            (0 until list.length).map { list.item(it) }
                .filterIsInstance<Element>()
                .find { it.getAttribute("nombre") == log.usuario }
        }

        if (usuarioElem == null) {
            val nuevoUsuario = doc.createElement("usuario")
            nuevoUsuario.setAttribute("nombre", log.usuario)
            root.appendChild(nuevoUsuario)
            usuarioElem = nuevoUsuario
        }

        // Lógica de Evento: ¿Existe ya para este usuario?
        var eventoElem = usuarioElem!!.childNodes.let { list -> // Usamos !! porque ya sabemos que existe
            (0 until list.length).map { list.item(it) }
                .filterIsInstance<Element>()
                .find { it.getAttribute("tipo") == log.evento }
        }

        if (eventoElem == null) {
            val nuevoEvento = doc.createElement("evento")
            nuevoEvento.setAttribute("tipo", log.evento)
            nuevoEvento.setAttribute("repeticiones", "1")
            usuarioElem.appendChild(nuevoEvento)
        } else {
            // Si ya existe, subimos las repeticiones
            val reps = eventoElem.getAttribute("repeticiones").toIntOrNull() ?: 0
            eventoElem.setAttribute("repeticiones", (reps + 1).toString())
        }

        guardarYValidar(doc)
    }

    private fun cargarDocumento(): Document {
        if (!xmlFile.exists()) {
            xmlFile.parentFile.mkdirs()
            xmlFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><eventos></eventos>")
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(xmlFile)
    }

    private fun guardarYValidar(doc: Document) {
        // Guardar físicamente
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        transformer.transform(DOMSource(doc), StreamResult(xmlFile))

        // Validar con XSD
        if (xsdFile.exists()) {
            val schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdFile)
            schema.newValidator().validate(javax.xml.transform.stream.StreamSource(xmlFile))
            println("✅ XML Validado correctamente")
        }
    }

    fun generarInformeHtml() {
        val outputFile = File("informe_resultados.html")
        if (xmlFile.exists() && xsltFile.exists()) {
            val transformer = TransformerFactory.newInstance().newTransformer(javax.xml.transform.stream.StreamSource(xsltFile))
            transformer.transform(javax.xml.transform.stream.StreamSource(xmlFile), StreamResult(outputFile))
        }
    }
}