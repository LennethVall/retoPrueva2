// =========================
// 1. VARIABLES GLOBALES
// =========================
let tipoEstimuloActual = "";
let tiempoEstimulo = 0;
let esperandoRespuesta = false;
let usuario = "Anonimo";
let clicks = 0;

// =========================
// 2. REFERENCIAS DOM
// =========================
const boton = document.getElementById("botonMisterioso");
const contador = document.getElementById("contador");
const resultado = document.getElementById("resultado");
const imagenRandom = document.getElementById("imagenRandom");

// =========================
// 3. INICIALIZACIÓN
// =========================
window.onload = function () {
    usuario = prompt("Introduce tu nombre:") || "Anonimo";
    console.log("Usuario:", usuario);
};

// =========================
// 4. CLICK PRINCIPAL → GENERA ESTÍMULO
// =========================
if (boton) {
    boton.addEventListener("click", generarEstimulo);
}

function generarEstimulo() {

    clicks++;
    if (contador) contador.innerText = clicks;

    const acciones = [
        { fn: cambiarFondo, tipo: "fondo" },
        { fn: reproducirSonido, tipo: "sonido" },
        { fn: mostrarImagen, tipo: "imagen" },
        { fn: mostrarFrase, tipo: "frase" }
    ];

    const elegido = acciones[Math.floor(Math.random() * acciones.length)];

    tipoEstimuloActual = elegido.tipo;

    // 🔥 empieza medición de reacción
    tiempoEstimulo = Date.now();
    esperandoRespuesta = true;

    elegido.fn();

    cambiarColorBoton();
    moverBoton();
    animarBoton();
}

// =========================
// 5. CLICK DE RESPUESTA (MEDICIÓN REAL)
// =========================
document.addEventListener("click", function (e) {

    // evitamos contar clicks del botón generador
    if (e.target === boton) return;

    if (!esperandoRespuesta) return;

    const ahora = Date.now();
    const tiempoReaccion = ahora - tiempoEstimulo;

    esperandoRespuesta = false;

    registrarEvento(tipoEstimuloActual, tiempoReaccion);
});

// =========================
// 6. ENVÍO AL BACKEND
// =========================
function registrarEvento(tipoEstimulo, tiempoReaccionMs) {

    fetch("/log", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            usuario: usuario,
            evento: tipoEstimulo,
            estimulo: tipoEstimulo,
            tiempoReaccionMs: tiempoReaccionMs
        })
    })
        .then(() => console.log("Log enviado"))
        .catch(err => console.error("Error:", err));
}

// =========================
// 7. UI (SIN CAMBIOS GRANDES)
// =========================
function cambiarColorBoton() {
    const colores = ["btn-primary","btn-success","btn-danger","btn-warning","btn-info","btn-dark"];
    boton.classList.remove(...colores);
    boton.classList.add(colores[clicks % colores.length]);
}

function animarBoton() {
    boton.style.transform = "scale(1.1)";
    setTimeout(() => boton.style.transform = "scale(1)", 150);
}

function moverBoton() {
    const posiciones = [
        { t: "20px", l: "20px" },
        { t: "20px", r: "20px" },
        { b: "20px", l: "20px" },
        { b: "20px", r: "20px" }
    ];

    const pos = posiciones[Math.floor(Math.random() * posiciones.length)];

    boton.style.top = pos.t || "auto";
    boton.style.bottom = pos.b || "auto";
    boton.style.left = pos.l || "auto";
    boton.style.right = pos.r || "auto";
}

// =========================
// 8. ESTÍMULOS
// =========================
function cambiarFondo() {
    document.body.style.backgroundColor =
        "#" + Math.floor(Math.random()*16777215).toString(16);
    resultado.innerText = "Fondo cambiado";
}

function mostrarImagen() {
    resultado.innerText = "Imagen mostrada";
}

function mostrarFrase() {
    resultado.innerText = "Frase aleatoria";
}

function reproducirSonido() {
    resultado.innerText = "Sonido";
}