import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.http.content.default
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.*
import model.LogEvento
import service.LogService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            jackson()
        }

        routing {

            // Servir tu web tal cual está en la raíz del proyecto
            staticFiles("/", File(".")) {
                default("index.html")
            }

            // Recibir logs desde tu JS
            post("/log") {
                val log = call.receive<LogEvento>()
                LogService.registrarEvento(log)
                call.respond(HttpStatusCode.OK, "OK")
            }

            // Devolver XML principal
            get("/eventos.xml") {
                val path = Paths.get("src/main/resources/data/eventos.xml")
                val xml = Files.readString(path)
                call.respondText(xml, ContentType.Text.Xml)
            }

            // Devolver XSLT
            get("/xslt/resumen.xslt") {
                val path = Paths.get("src/main/resources/data/xslt/resumen.xslt")
                val xsl = Files.readString(path)
                call.respondText(xsl, ContentType.Text.Xml)
            }
        }
    }.start(wait = true)
}
