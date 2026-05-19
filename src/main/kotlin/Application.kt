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

            // Servimos carpetas y archivos directamente desde tu raíz real (css, js, etc.)
            staticFiles("/", File("."))

            // 🛠️ REVISIÓN CRÍTICA: En vez de redirigir a un archivo que puede sufrir caché,
            // forzamos al backend a leer el HTML fresco generado y responderlo en vivo.
            get("/generar-informe") {
                try {
                    LogService.generarInformeHtml()

                    // Buscamos el archivo generado en el espacio activo
                    val informeFile = File("informe_resultados.html")

                    if (informeFile.exists()) {
                        val contenidoHtml = informeFile.readText()
                        call.respondText(contenidoHtml, ContentType.Text.Html)
                    } else {
                        // Plan B: Intentamos leerlo de la carpeta build si Gradle desvió la ruta
                        val informeBuildFile = File("build/resources/main/informe_resultados.html")
                        if (informeBuildFile.exists()) {
                            call.respondText(informeBuildFile.readText(), ContentType.Text.Html)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "El motor XSLT terminó pero el sistema operativo no encuentra el HTML.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error generando el informe físico mediante XSLT"
                    )
                }
            }

            // Registro de eventos
            post("/log") {
                try {
                    val log = call.receive<LogEvento>()
                    LogService.registrarEvento(log)

                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("ok" to true, "mensaje" to "Log registrado correctamente")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("ok" to false, "mensaje" to "Error registrando log")
                    )
                }
            }

            // Estadísticas de usuario
            get("/stats/{usuario}") {
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Usuario no informado"))
                call.respond(LogService.obtenerEstadisticasUsuario(usuario))
            }

            // Estadísticas por estímulo
            get("/stats/estimulos/{usuario}") {
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Usuario no informado"))
                call.respond(LogService.mejorPorEstimulo(usuario))
            }

            // Ranking Top 3
            get("/ranking") {
                call.respond(LogService.rankingTop3())
            }

            // Visualizar XML crudo
            get("/ver-xml") {
                val path = Paths.get("src/main/resources/data/estimulos.xml")
                if (Files.exists(path)) {
                    call.respondText(Files.readString(path), ContentType.Text.Xml)
                } else {
                    call.respond(HttpStatusCode.NotFound, "No existe el XML")
                }
            }
        }

    }.start(wait = true)
}