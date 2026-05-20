package model

// Se importa LocalDateTime para generar automáticamente
// la fecha y hora actual cuando se crea un nuevo evento.
import java.time.LocalDateTime

/**
 * Clase de datos que representa un evento registrado en la aplicación.
 *
 * Se utiliza para guardar la información básica de cada interacción del usuario:
 * - usuario: nombre o identificador del jugador
 * - estimulo: tipo de estímulo mostrado
 * - tiempoReaccionMs: tiempo de reacción medido en milisegundos
 * - fecha: momento en el que se registró el evento
 *
 * Al ser una data class, Kotlin genera automáticamente métodos útiles
 * como toString(), equals(), hashCode() y copy().
 */
data class LogEvento(

    // Nombre del usuario que ha realizado la interacción.
    // Si no se especifica, se asigna "Anonimo" por defecto.
    val usuario: String = "Anonimo",

    // Tipo de estímulo que provocó la reacción del usuario.
    // Por defecto se inicializa como "ninguno".
    val estimulo: String = "ninguno",

    // Tiempo de reacción medido en milisegundos.
    // Se inicia en 0 si no se proporciona otro valor.
    val tiempoReaccionMs: Long = 0,

    // Fecha y hora de creación del evento.
    // Se genera automáticamente con la fecha actual en formato ISO.
    val fecha: String = LocalDateTime.now().toString()
)