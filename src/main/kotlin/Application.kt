// Importaciones de Ktor para gestión HTTP, servidor embebido, rutas y respuestas.
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

// Importaciones de clases propias del proyecto.
import model.LogEvento
import service.LogService

// Importaciones de Java para trabajar con archivos y rutas del sistema.
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Función principal de arranque de la aplicación.
 *
 * Este método inicia un servidor embebido con Netty en el puerto 8080.
 * Desde aquí se configura Ktor, se instala la serialización JSON
 * y se definen todas las rutas del backend.
 *
 * Además, este servidor da soporte a la parte web del proyecto,
 * ya que sirve archivos estáticos como HTML, CSS y JavaScript.
 */
fun main() {

    // Mensaje informativo que se muestra por consola al arrancar el servidor.
    println("🚀 Servidor iniciado en http://localhost:8080")

    /**
     * Creación del servidor embebido con motor Netty.
     *
     * - Netty actúa como motor interno del servidor HTTP.
     * - port = 8080 indica el puerto local en el que escuchará la aplicación.
     */
    embeddedServer(Netty, port = 8080) {

        /**
         * Instalación del plugin ContentNegotiation.
         *
         * Este plugin permite convertir automáticamente objetos Kotlin
         * en formatos intercambiables como JSON, y viceversa.
         * En este caso se usa Jackson como biblioteca de serialización.
         */
        install(ContentNegotiation) {
            jackson()
        }

        /**
         * Bloque principal de enrutamiento.
         *
         * Aquí se definen todos los endpoints de la aplicación:
         * - rutas para servir contenido estático,
         * - generación del informe HTML,
         * - registro de eventos,
         * - consulta de estadísticas,
         * - visualización del XML original.
         */
        routing {

            /**
             * Ruta para servir archivos estáticos.
             *
             * staticFiles("/", File(".")) permite publicar directamente
             * archivos del proyecto como HTML, CSS y JavaScript.
             *
             * Gracias a esta configuración, el frontend puede cargar recursos como:
             * - hojas de estilo
             * - scripts
             * - imágenes
             *
             * Esto conecta el backend Ktor con la interfaz web desarrollada aparte.
             */
            staticFiles("/", File("."))

            /**
             * Endpoint GET que genera dinámicamente un informe HTML.
             *
             * En lugar de redirigir al navegador a un archivo ya existente,
             * el servidor fuerza primero la generación actualizada del informe
             * mediante el servicio LogService.
             *
             * Esto evita problemas de caché y asegura que el contenido mostrado
             * esté recién generado en el momento de la petición.
             */
            get("/generar-informe") {
                try {
                    // Llama al servicio que genera el informe HTML a partir de los datos disponibles.
                    LogService.generarInformeHtml()

                    // Se intenta localizar el archivo generado en la ruta principal del proyecto.
                    val informeFile = File("informe_resultados.html")

                    if (informeFile.exists()) {
                        // Si el archivo existe, se lee su contenido y se devuelve como HTML al navegador.
                        val contenidoHtml = informeFile.readText()
                        call.respondText(contenidoHtml, ContentType.Text.Html)
                    } else {
                        /**
                         * Plan alternativo:
                         * si el archivo no está en la ruta principal,
                         * se busca en la carpeta build/resources/main.
                         *
                         * Esto puede ocurrir si Gradle ha movido los recursos durante la compilación.
                         */
                        val informeBuildFile = File("build/resources/main/informe_resultados.html")

                        if (informeBuildFile.exists()) {
                            // Si se encuentra en la carpeta build, también se devuelve al cliente.
                            call.respondText(informeBuildFile.readText(), ContentType.Text.Html)
                        } else {
                            // Si no se encuentra en ninguna ubicación, se responde con error 404.
                            call.respond(
                                HttpStatusCode.NotFound,
                                "El motor XSLT terminó pero el sistema operativo no encuentra el HTML."
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Si ocurre cualquier error durante la generación del informe, se muestra en consola.
                    e.printStackTrace()

                    // Se responde al cliente con error interno del servidor.
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error generando el informe físico mediante XSLT"
                    )
                }
            }

            /**
             * Endpoint POST para registrar eventos de usuario.
             *
             * Recibe un objeto LogEvento en el cuerpo de la petición,
             * lo procesa y lo guarda utilizando LogService.
             *
             * Esta ruta es importante para almacenar la actividad del usuario
             * y luego poder generar estadísticas e informes.
             */
            post("/log") {
                try {
                    // Convierte automáticamente el cuerpo JSON recibido en un objeto LogEvento.
                    val log = call.receive<LogEvento>()

                    // Registra el evento mediante la capa de servicio.
                    LogService.registrarEvento(log)

                    // Respuesta de éxito en formato JSON.
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("ok" to true, "mensaje" to "Log registrado correctamente")
                    )
                } catch (e: Exception) {
                    // Si el log no puede procesarse, se informa del error.
                    e.printStackTrace()

                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("ok" to false, "mensaje" to "Error registrando log")
                    )
                }
            }

            /**
             * Endpoint GET para obtener estadísticas de un usuario concreto.
             *
             * El nombre del usuario se recibe como parámetro en la URL.
             * Ejemplo: /stats/selene
             */
            get("/stats/{usuario}") {

                // Se obtiene el parámetro 'usuario' desde la ruta.
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Usuario no informado")
                    )

                // Se devuelve la estadística generada por el servicio.
                call.respond(LogService.obtenerEstadisticasUsuario(usuario))
            }

            /**
             * Endpoint GET para consultar cuál ha sido el mejor resultado
             * del usuario según el tipo de estímulo.
             *
             * Ejemplo: /stats/estimulos/selene
             */
            get("/stats/estimulos/{usuario}") {

                // Se valida que el parámetro usuario exista en la petición.
                val usuario = call.parameters["usuario"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Usuario no informado")
                    )

                // Responde con los mejores resultados agrupados por estímulo.
                call.respond(LogService.mejorPorEstimulo(usuario))
            }

            /**
             * Endpoint GET que devuelve un ranking con el Top 3.
             *
             * Esta ruta obtiene una clasificación global de resultados,
             * probablemente a partir de los eventos almacenados.
             */
            get("/ranking") {
                call.respond(LogService.rankingTop3())
            }

            /**
             * Endpoint GET para visualizar el contenido XML original.
             *
             * Se usa para comprobar directamente el archivo XML de datos,
             * lo cual resulta útil para depuración, validación
             * o para verificar la información antes de aplicar XSLT.
             */
            get("/ver-xml") {

                // Ruta del archivo XML dentro de resources.
                val path = Paths.get("src/main/resources/data/estimulos.xml")

                if (Files.exists(path)) {
                    // Si el XML existe, se devuelve como texto XML.
                    call.respondText(Files.readString(path), ContentType.Text.Xml)
                } else {
                    // Si no existe, se devuelve un error 404.
                    call.respond(HttpStatusCode.NotFound, "No existe el XML")
                }
            }
        }

        /**
         * start(wait = true) arranca el servidor y mantiene la aplicación activa
         * esperando peticiones HTTP hasta que se detenga manualmente.
         */
    }.start(wait = true)
}