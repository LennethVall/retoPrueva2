import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.LogEvento
import service.LogService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    embeddedServer(Netty, port = 8080) {
        // Instalamos Jackson para que el POST funcione
        install(ContentNegotiation) {
            jackson()
        }

        routing {
            // 1. Servir archivos de la raíz (index, js, css, img...)
            staticFiles("/", File(".")) {
                default("index.html")
            }

            // 2. Endpoint para el LOG (Tu JS hace POST aquí)
            post("/log") {
                try {
                    val log = call.receive<LogEvento>()
                    LogService.registrarEvento(log)
                    call.respond(HttpStatusCode.OK, "Evento registrado")
                } catch (e: Exception) {
                    // Si algo falla al recibir el JSON, nos avisa aquí
                    call.respond(HttpStatusCode.BadRequest, "Error en el formato del log")
                }
            }

            // 3. Endpoint para generar el informe y verlo directamente
            get("/generar-informe") {
                LogService.generarInformeHtml()
                val file = File("informe_resultados.html")
                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respondText("Error al generar el archivo", status = HttpStatusCode.InternalServerError)
                }
            }

            // 4. Ver el XML en crudo si quieres
            get("/ver-xml") {
                val path = Paths.get("src/main/resources/data/eventos.xml")
                if (Files.exists(path)) {
                    val xml = Files.readString(path)
                    call.respondText(xml, ContentType.Text.Xml)
                } else {
                    call.respond(HttpStatusCode.NotFound, "XML no encontrado")
                }
            }
        }
    }.start(wait = true)
}