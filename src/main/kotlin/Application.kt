import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import model.LogEvento
import service.LogService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main() {

    println("🚀 Servidor iniciado")

    embeddedServer(Netty, port = 8080) {

        install(ContentNegotiation) {
            jackson()
        }

        routing {

            staticFiles("/", File(".")) {
                default("index.html")
            }

            // LOG
            post("/log") {
                val log = call.receive<LogEvento>()
                LogService.registrarEvento(log)
                call.respond(HttpStatusCode.OK)
            }

            // STATS
            get("/stats/{usuario}") {
                val u = call.parameters["usuario"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(LogService.obtenerEstadisticasUsuario(u))
            }

            // ESTÍMULOS
            get("/stats/estimulos/{usuario}") {
                val u = call.parameters["usuario"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(LogService.mejorPorEstimulo(u))
            }

            // RANKING
            get("/ranking") {
                call.respond(LogService.rankingTop3())
            }

            // INFORME HTML
            get("/generar-informe") {
                try {
                    LogService.generarInformeHtml()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val file = File("informe_resultados.html")

                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respondText("Error generando informe")
                }
            }

            // XML
            get("/ver-xml") {
                val path = Paths.get("src/main/resources/data/eventos.xml")

                if (Files.exists(path)) {
                    call.respondText(Files.readString(path), ContentType.Text.Xml)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

    }.start(wait = true)
}