package service

import model.LogEvento
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.validation.SchemaFactory

object LogService {

    private val xmlFile = File("build/resources/main/data/eventos.xml")

    private val xsdFile = File("build/resources/main/data/eventos.xsd")


    fun registrarEvento(log: LogEvento) {
        val doc = cargarDocumento()

        val root = doc.documentElement

        // buscar usuario
        val usuarios = root.getElementsByTagName("usuario")
        var usuarioElem: Element? = null
        for (i in 0 until usuarios.length) {
            val u = usuarios.item(i) as Element
            if (u.getAttribute("nombre") == log.usuario) {
                usuarioElem = u
                break
            }
        }
        if (usuarioElem == null) {
            usuarioElem = doc.createElement("usuario")
            usuarioElem.setAttribute("nombre", log.usuario)
            root.appendChild(usuarioElem)
        }

        // buscar evento
        val eventos = usuarioElem!!.getElementsByTagName("evento")
        var eventoElem: Element? = null
        for (i in 0 until eventos.length) {
            val e = eventos.item(i) as Element
            if (e.getAttribute("tipo") == log.evento) {
                eventoElem = e
                break
            }
        }
        if (eventoElem == null) {
            eventoElem = doc.createElement("evento")
            eventoElem.setAttribute("tipo", log.evento)
            eventoElem.setAttribute("repeticiones", "1")
            usuarioElem.appendChild(eventoElem)
        } else {
            val rep = eventoElem.getAttribute("repeticiones").toIntOrNull() ?: 0
            eventoElem.setAttribute("repeticiones", (rep + 1).toString())
        }

        guardarYValidar(doc)
    }

    private fun cargarDocumento(): Document {
        if (!xmlFile.exists()) {
            xmlFile.parentFile.mkdirs()
            xmlFile.writeText("""<eventos></eventos>""")
        }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(xmlFile)
    }

    private fun guardarYValidar(doc: Document) {
        // guardar
        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        transformer.transform(DOMSource(doc), StreamResult(xmlFile))

        // validar
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val schema = schemaFactory.newSchema(xsdFile)
        val validator = schema.newValidator()
        validator.validate(javax.xml.transform.stream.StreamSource(xmlFile))
    }
}
