let usuario = "";
let clicks = 0;
let estimuloActual = "";
let tiempoInicioEstimulo = 0;

const boton = document.getElementById("botonMisterioso");
const contador = document.getElementById("contador");
const resultado = document.getElementById("resultado");
const imagenRandom = document.getElementById("imagenRandom");

// 🛠️ INICIALIZACIÓN SEGURA: Esperamos a que el DOM esté listo y damos un respiro al navegador
window.onload = function () {
    localStorage.clear();
    sessionStorage.clear();

    // Pequeño retardo (200ms) para que el navegador renderice la UI antes de bloquear el hilo con el prompt
    setTimeout(() => {
        let nombreMetido = prompt("Introduce tu nombre o nick para jugar:");

        usuario = (!nombreMetido || nombreMetido.trim() === "") ? "Jugador_Nuevo" : nombreMetido.trim();

        if (resultado) {
            resultado.innerText = "Bienvenido, " + usuario;
        }

        // Solo lanzamos las peticiones al servidor cuando el nombre es seguro
        refrescarPanel();
    }, 200);
};

if (boton) {
    boton.addEventListener("click", generarEstimulo);
}

function generarEstimulo() {
    clicks++;


    const ahora = Date.now();

    // 🛠️ FILTRO CRÍTICO: Solo se envía el log si ya existía un estímulo REAL previo
    if (tiempoInicioEstimulo > 0 && estimuloActual !== "") {
        const tiempoReaccionMs = ahora - tiempoInicioEstimulo;
        if (tiempoReaccionMs > 10) {
            registrarLog(estimuloActual, tiempoReaccionMs);
        }
    }

    tiempoInicioEstimulo = ahora;
    limpiarZonaVisual();

    const acciones = [
        { nombre: "FONDO", fn: cambiarFondo },
        { nombre: "SONIDO", fn: reproducirSonido },
        { nombre: "IMAGEN", fn: mostrarImagen },
        { nombre: "FRASE", fn: mostrarFrase }
    ];

    const elegido = acciones[Math.floor(Math.random() * acciones.length)];
    estimuloActual = elegido.nombre;

    elegido.fn();
    cambiarColorBoton();
    moverBoton();
    animarBoton();
}

function registrarLog(estimulo, tiempoReaccionMs) {
    fetch("/log", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            usuario: usuario,
            estimulo: estimulo,
            tiempoReaccionMs: tiempoReaccionMs,
            fecha: new Date().toLocaleString("es-ES")
        })
    })
        .then(response => { if (!response.ok) throw new Error("Error al guardar log"); return response.json(); })
        .then(() => refrescarPanel())
        .catch(error => console.error("Error registrando evento en Ktor:", error));
}

function limpiarZonaVisual() {
    if (imagenRandom) imagenRandom.innerHTML = "";
}

function cambiarColorBoton() {
    const colores = ["btn-primary", "btn-success", "btn-danger", "btn-warning", "btn-info", "btn-dark"];
    boton.classList.remove(...colores);
    boton.classList.add(colores[clicks % colores.length]);
}

function animarBoton() {
    boton.style.transform = "scale(1.1)";
    setTimeout(() => { boton.style.transform = "scale(1)"; }, 150);
}

function moverBoton() {
    const posiciones = [
        { top: "20px", left: "20px", right: "auto", bottom: "auto" },
        { top: "20px", right: "20px", left: "auto", bottom: "auto" },
        { bottom: "20px", left: "20px", top: "auto", right: "auto" },
        { bottom: "20px", right: "20px", top: "auto", left: "auto" }
    ];
    const pos = posiciones[Math.floor(Math.random() * posiciones.length)];
    boton.style.top = pos.top;
    boton.style.bottom = pos.bottom;
    boton.style.left = pos.left;
    boton.style.right = pos.right;
}

function cambiarFondo() {
    const index = Math.floor(Math.random() * 28) + 1;
    const rutas = [`background/background${index}.jpg`, `background/background${index}.png`];
    cargarPrimeraValida(rutas, (url) => {
        document.body.style.backgroundImage = `url('${url}')`;
        document.body.style.backgroundSize = "cover";
        document.body.style.backgroundPosition = "center";
        document.body.style.backgroundRepeat = "no-repeat";
    });
}

function mostrarImagen() {
    const index = Math.floor(Math.random() * 28) + 1;
    const rutas = [`img/img${index}.png`, `img/img${index}.jpg`, `img/img${index}.jpeg`];
    cargarPrimeraValida(rutas, (url) => {
        imagenRandom.innerHTML = `<img src="${url}" alt="Imagen aleatoria" style="max-height: 300px; border-radius: 10px;">`;
    });
}

function mostrarFrase() {
    const frases = ["Nunca sabes lo que pasará al hacer click.", "Prueba tu suerte, ¡va a ser algo grande!", "Un regalo en cada click.", "Click, click, click...", "El orden es aburrido, ¡viva el caos!", "Solo un click más..."];
    const frase = frases[Math.floor(Math.random() * frases.length)];
    imagenRandom.innerHTML = `<div class="frase h3 text-white p-3 rounded" style="background: rgba(0,0,0,0.6);">${frase}</div>`;
}

function reproducirSonido() {
    const index = Math.floor(Math.random() * 28) + 1;
    new Audio(`sounds/sound${index}.mp3`).play().catch(() => {});
}

function cargarPrimeraValida(rutas, onSuccess) {
    let i = 0;
    function probar() {
        if (i >= rutas.length) return;
        const img = new Image();
        img.onload = () => onSuccess(rutas[i]);
        img.onerror = () => { i++; probar(); };
        img.src = rutas[i];
    }
    probar();
}

function refrescarPanel() {
    const contenidoPanel = document.getElementById("contenidoPanel");
    if (!contenidoPanel || !usuario) return;

    Promise.all([
        fetch(`/stats/${encodeURIComponent(usuario)}`).then(r => r.ok ? r.json() : null),
        fetch(`/stats/estimulos/${encodeURIComponent(usuario)}`).then(r => r.ok ? r.json() : null),
        fetch("/ranking").then(r => r.ok ? r.json() : [])
    ])
        .then(([stats, estimulos, ranking]) => {
            let statsHtml = `
            <div class="mb-4">
                <h6 class="text-info text-uppercase small fw-bold mb-3">📋 Resumen de ${usuario}</h6>
                <div class="list-group list-group-flush shadow-sm rounded border border-secondary">
                    <div class="list-group-item bg-transparent text-white d-flex justify-content-between">
                        <span>🎯 Clicks:</span> <span class="fw-bold text-warning">${stats?.clicks ?? 0}</span>
                    </div>
                    <div class="list-group-item bg-transparent text-white d-flex justify-content-between">
                        <span>⚡ Mejor:</span> <span class="badge bg-success rounded-pill">${stats?.mejor ?? 0} ms</span>
                    </div>
                    <div class="list-group-item bg-transparent text-white d-flex justify-content-between">
                        <span>📊 Media:</span> <span class="fw-bold text-info">${Math.round(stats?.media ?? 0)} ms</span>
                    </div>
                </div>
            </div>`;

            let estimulosHtml = `<div class="mb-4"><h6 class="text-info text-uppercase small fw-bold mb-2">⏱️ Mejor por Estímulo</h6><div class="row g-2">`;
            if (estimulos && Object.keys(estimulos).length > 0) {
                for (const est in estimulos) {
                    estimulosHtml += `<div class="col-6"><div class="p-2 rounded border border-secondary text-center bg-dark bg-opacity-70"><small class="text-info text-uppercase fw-bold d-block">${est}</small><span class="fw-bold text-white">${estimulos[est]} ms</span></div></div>`;
                }
            } else { estimulosHtml += `<p class="text-muted small ps-2">Esperando eventos...</p>`; }
            estimulosHtml += `</div></div>`;

            let rankingHtml = `<div class="mb-3"><h6 class="text-info text-uppercase small fw-bold mb-3">🏆 Líderes (Media)</h6><ol class="list-group list-group-numbered border border-secondary rounded">`;
            if (ranking && ranking.length > 0) {
                ranking.forEach(fila => {
                    rankingHtml += `<li class="list-group-item bg-transparent text-white d-flex justify-content-between align-items-center border-secondary"><span class="${fila.usuario === usuario ? 'text-warning' : ''}">${fila.usuario}</span><span class="badge bg-info text-dark rounded-pill">${Math.round(fila.media ?? 0)} ms</span></li>`;
                });
            } else { rankingHtml += `<li class="list-group-item bg-transparent text-muted small border-0">Podio vacío.</li>`; }
            rankingHtml += `</ol></div>`;

            contenidoPanel.innerHTML = statsHtml + estimulosHtml + rankingHtml;
        })
        .catch(err => console.error("Error en panel:", err));
}