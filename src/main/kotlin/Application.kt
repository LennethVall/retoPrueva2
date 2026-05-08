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

    println("🚀 Servidor iniciado en http://localhost:8080")

    embeddedServer(Netty, port = 8080) {

        install(ContentNegotiation) {
            jackson()
        }

        routing {

            staticFiles("/", File(".")) {
                default("index.html")
            }

            post("/log") {
                try {
                    val log = call.receive<LogEvento>()
                    LogService.registrarEvento(log)

                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "ok" to true,
                            "mensaje" to "Log registrado correctamente"
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "ok" to false,
                            "mensaje" to "Error registrando log"
                        )
                    )
                }
            }

            get("/stats/{usuario}") {
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Usuario no informado")
                    )

                call.respond(LogService.obtenerEstadisticasUsuario(usuario))
            }

            get("/stats/estimulos/{usuario}") {
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Usuario no informado")
                    )

                call.respond(LogService.mejorPorEstimulo(usuario))
            }

            get("/ranking") {
                call.respond(LogService.rankingTop3())
            }

            get("/generar-informe") {
                try {
                    LogService.generarInformeHtml()
                    val file = File("informe_resultados.html")

                    if (file.exists()) {
                        call.respondFile(file)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "No se pudo generar el informe"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error generando informe"
                    )
                }
            }

            get("/ver-xml") {
                val path = Paths.get("src/main/resources/data/estimulos.xml")

                if (Files.exists(path)) {
                    call.respondText(
                        Files.readString(path),
                        ContentType.Text.Xml
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "No existe el XML"
                    )
                }
            }
        }

    }.start(wait = true)
}