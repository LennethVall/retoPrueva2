let usuario = "Anonimo";
let clicks = 0;
let estimuloActual = "";
let tiempoInicioEstimulo = 0;
let esperandoRespuesta = false;

const boton = document.getElementById("botonMisterioso");
const contador = document.getElementById("contador");
const resultado = document.getElementById("resultado");
const imagenRandom = document.getElementById("imagenRandom");

window.onload = function () {
    usuario = prompt("Introduce tu nombre o nick:") || "Anonimo";

    if (resultado) {
        resultado.innerText = "Bienvenido, " + usuario;
    }

    refrescarPanel();
};

if (boton) {
    boton.addEventListener("click", generarEstimulo);
}

document.addEventListener("click", function (e) {
    if (!esperandoRespuesta) return;
    if (e.target === boton) return;

    const tiempoReaccionMs = Date.now() - tiempoInicioEstimulo;
    esperandoRespuesta = false;

    registrarLog(estimuloActual, tiempoReaccionMs);
});

function generarEstimulo() {
    clicks++;
    if (contador) contador.innerText = clicks;

    limpiarZonaVisual();

    const acciones = [
        { nombre: "fondo", fn: cambiarFondo },
        { nombre: "sonido", fn: reproducirSonido },
        { nombre: "imagen", fn: mostrarImagen },
        { nombre: "frase", fn: mostrarFrase }
    ];

    const elegido = acciones[Math.floor(Math.random() * acciones.length)];
    estimuloActual = elegido.nombre;

    elegido.fn();

    tiempoInicioEstimulo = Date.now();
    esperandoRespuesta = true;

    cambiarColorBoton();
    moverBoton();
    animarBoton();
}

function registrarLog(estimulo, tiempoReaccionMs) {
    fetch("/log", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            usuario: usuario,
            estimulo: estimulo,
            tiempoReaccionMs: tiempoReaccionMs,
            fecha: new Date().toISOString()
        })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Error al guardar log");
            }
            return response.text().catch(() => "");
        })
        .then(() => {
            if (resultado) {
                resultado.innerText = `Respuesta registrada: ${tiempoReaccionMs} ms`;
            }
            refrescarPanel();
        })
        .catch(error => {
            console.error("Error:", error);
            if (resultado) {
                resultado.innerText = "No se pudo guardar el registro";
            }
        });
}

function limpiarZonaVisual() {
    if (imagenRandom) {
        imagenRandom.innerHTML = "";
    }
}

function cambiarColorBoton() {
    const colores = ["btn-primary", "btn-success", "btn-danger", "btn-warning", "btn-info", "btn-dark"];
    boton.classList.remove(...colores);
    boton.classList.add(colores[clicks % colores.length]);
}

function animarBoton() {
    boton.style.transform = "scale(1.1)";
    setTimeout(() => {
        boton.style.transform = "scale(1)";
    }, 150);
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

    const rutas = [
        `background/background${index}.jpg`,
        `background/background${index}.png`
    ];

    cargarPrimeraValida(rutas, function (url) {
        document.body.style.backgroundImage = `url('${url}')`;
        document.body.style.backgroundSize = "cover";
        document.body.style.backgroundPosition = "center";
        document.body.style.backgroundRepeat = "no-repeat";

        if (resultado) resultado.innerText = "Fondo cambiado";
    }, function () {
        if (resultado) resultado.innerText = "No se encontró el fondo";
    });
}

function mostrarImagen() {
    const index = Math.floor(Math.random() * 28) + 1;

    const rutas = [
        `img/img${index}.png`,
        `img/img${index}.jpg`,
        `img/img${index}.jpeg`
    ];

    cargarPrimeraValida(rutas, function (url) {
        imagenRandom.innerHTML = `<img src="${url}" alt="Imagen aleatoria">`;
        if (resultado) resultado.innerText = "Imagen mostrada";
    }, function () {
        if (resultado) resultado.innerText = "No se encontró la imagen";
    });
}

function mostrarFrase() {
    const frases = [
        "Nunca sabes lo que pasará al hacer click.",
        "Prueba tu suerte, ¡va a ser algo grande!",
        "Un regalo en cada click.",
        "Click, click, click... ¿qué será lo siguiente?",
        "Yo tampoco puedo parar de hacer click.",
        "Solo un click más... solo un click más...",
        "¿Eso ha sido un sonido o te ha pitado el oído?",
        "¡Ups! He movido algo, espero que te guste el nuevo look.",
        "El algoritmo dice que ahora te toca algo... especial.",
        "¿Has visto eso? Yo tampoco, pero ha sonado genial.",
        "Espera, que estoy cargando la siguiente sorpresa...",
        "No parpadees, que el fondo puede cambiar.",
        "Un píxel acaba de nacer gracias a ti.",
        "Si pulsas 100 veces, no pasa nada... ¿o sí?",
        "Tu pantalla se está poniendo nerviosa.",
        "Ese click ha sonado a gloria bendita.",
        "Advertencia: este botón causa niveles altos de curiosidad.",
        "El orden es aburrido, ¡viva el caos!",
        "¿Has sido tú o ha sido la aplicación?",
        "Si ves un unicornio, es que has pulsado demasiado rápido.",
        "¡Zas! En toda la interfaz!",
        "Instalando dosis de aleatoriedad... 99%.",
        "He cambiado las cortinas, espero que no te importe.",
        "El siguiente click es el bueno, lo presiento.",
        "Ni yo sé qué va a pasar ahora mismo.",
        "Pulsa como si no hubiera un mañana.",
        "¡Otra vez! ¡Otra vez!",
        "¿Crees que puedes verlo todo? Sigue intentándolo."
    ];

    const frase = frases[Math.floor(Math.random() * frases.length)];
    imagenRandom.innerHTML = `<div class="frase">${frase}</div>`;
    if (resultado) resultado.innerText = "Frase mostrada";
}

function reproducirSonido() {
    const index = Math.floor(Math.random() * 28) + 1;
    const audio = new Audio(`sounds/sound${index}.mp3`);

    audio.play()
        .then(() => {
            if (resultado) resultado.innerText = "Sonido reproducido";
        })
        .catch(() => {
            if (resultado) resultado.innerText = "No se pudo reproducir el sonido";
        });
}

function cargarPrimeraValida(rutas, onSuccess, onError) {
    let i = 0;

    function probar() {
        if (i >= rutas.length) {
            if (onError) onError();
            return;
        }

        const img = new Image();
        img.onload = function () {
            onSuccess(rutas[i]);
        };
        img.onerror = function () {
            i++;
            probar();
        };
        img.src = rutas[i];
    }

    probar();
}

function refrescarPanel() {
    cargarResumenUsuario();
    cargarRanking();
}

function cargarResumenUsuario() {
    fetch(`/stats/${encodeURIComponent(usuario)}`)
        .then(r => r.json())
        .then(data => {
            const box = document.getElementById("statsUsuario");
            if (!box) return;

            box.innerHTML = `
                <div><strong>Usuario:</strong> ${usuario}</div>
                <div><strong>Clicks:</strong> ${data.clicks ?? 0}</div>
                <div><strong>Mejor tiempo:</strong> ${data.mejor ?? 0} ms</div>
                <div><strong>Peor tiempo:</strong> ${data.peor ?? 0} ms</div>
                <div><strong>Media:</strong> ${Math.round(data.media ?? 0)} ms</div>
            `;
        })
        .catch(err => console.error(err));

    fetch(`/stats/estimulos/${encodeURIComponent(usuario)}`)
        .then(r => r.json())
        .then(data => {
            const box = document.getElementById("statsEstimulos");
            if (!box) return;

            let html = "<strong>Mejor por estímulo:</strong><br>";
            for (const estimulo in data) {
                html += `${estimulo}: ${data[estimulo]} ms<br>`;
            }
            box.innerHTML = html;
        })
        .catch(err => console.error(err));
}

function cargarRanking() {
    fetch("/ranking")
        .then(r => r.json())
        .then(data => {
            const box = document.getElementById("rankingTop3");
            if (!box) return;

            let html = "<strong>Top 3</strong><br>";
            data.forEach((fila, index) => {
                html += `${index + 1}. ${fila.usuario} - ${Math.round(fila.mejorTiempo ?? fila.media ?? 0)} ms (${fila.estimulo ?? "?"})<br>`;
            });
            box.innerHTML = html;
        })
        .catch(err => console.error(err));
}