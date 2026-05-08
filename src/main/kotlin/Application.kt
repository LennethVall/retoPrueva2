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

    // 🔹 Datos de prueba (opcional)
    LogService.registrarEvento(LogEvento("Sistema", "Inicio", "Prueba", 0))
    LogService.registrarEvento(LogEvento("Pepe", "Clic", "Imagen1", 150))
    LogService.registrarEvento(LogEvento("Maria", "Clic", "Sonido2", 300))

    println("✅ Servidor iniciando...")

    embeddedServer(Netty, port = 8080) {

        install(ContentNegotiation) {
            jackson()
        }

        routing {

            // =========================
            // 📦 ARCHIVOS ESTÁTICOS
            // =========================
            staticFiles("/", File(".")) {
                default("index.html")
            }

            // =========================
            // 📊 REGISTRAR EVENTO
            // =========================
            post("/log") {
                try {
                    val log = call.receive<LogEvento>()
                    LogService.registrarEvento(log)

                    call.respond(HttpStatusCode.OK, "Evento registrado")

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error en el formato del log")
                }
            }

            // =========================
            // 📈 ESTADÍSTICAS USUARIO
            // =========================
            get("/stats/{usuario}") {
                val usuario = call.parameters["usuario"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Usuario no válido"
                )

                call.respond(LogService.obtenerEstadisticasUsuario(usuario))
            }

            // =========================
            // 🎯 MEJOR POR ESTÍMULO
            // =========================
            get("/stats/estimulos/{usuario}") {
                val usuario = call.parameters["usuario"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Usuario no válido"
                )

                call.respond(LogService.mejorPorEstimulo(usuario))
            }

            // =========================
            // 🏆 RANKING TOP 3
            // =========================
            get("/ranking") {
                call.respond(LogService.rankingTop3())
            }

            // =========================
            // 📄 GENERAR INFORME HTML
            // =========================
            get("/generar-informe") {
                LogService.generarInformeHtml()

                val file = File("informe_resultados.html")

                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respondText(
                        "Error generando informe",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            // =========================
            // 📄 VER XML
            // =========================
            get("/ver-xml") {
                val path = Paths.get("src/main/resources/data/eventos.xml")

                if (Files.exists(path)) {
                    call.respondText(
                        Files.readString(path),
                        ContentType.Text.Xml
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound, "XML no encontrado")
                }
            }
        }

    }.start(wait = true)
}