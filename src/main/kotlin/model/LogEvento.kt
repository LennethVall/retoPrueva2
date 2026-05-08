package model

import java.time.LocalDateTime

data class LogEvento(
    val usuario: String = "Anonimo",
    val estimulo: String = "ninguno",
    val tiempoReaccionMs: Long = 0,
    val fecha: String = LocalDateTime.now().toString()
)