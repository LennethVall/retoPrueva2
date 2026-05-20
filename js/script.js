/*
    Variables globales de estado de la aplicación.
    Se utilizan para guardar información del usuario, contar clics,
    registrar el estímulo activo y medir el tiempo de reacción.
*/
let usuario = "";
let clicks = 0;
let estimuloActual = "";
let tiempoInicioEstimulo = 0;

/*
    Referencias a elementos del DOM.
    Se recuperan los nodos principales de la interfaz para poder modificarlos dinámicamente.
*/
const boton = document.getElementById("botonMisterioso");
const contador = document.getElementById("contador");
const resultado = document.getElementById("resultado");
const imagenRandom = document.getElementById("imagenRandom");

/*
    Inicialización de la aplicación cuando la ventana termina de cargar.
    Se espera a que todo el DOM esté listo antes de empezar la interacción.
*/
window.onload = function () {
    /*
        Se limpian localStorage y sessionStorage para evitar que queden
        datos persistentes de sesiones anteriores.
    */
    localStorage.clear();
    sessionStorage.clear();

    /*
        Se introduce un pequeño retardo para que la interfaz se renderice primero
        antes de mostrar el prompt al usuario.
    */
    setTimeout(() => {
        let nombreMetido = prompt("Introduce tu nombre o nick para jugar:");

        /*
            Si el usuario no introduce nada válido,
            se asigna un nombre por defecto.
        */
        usuario = (!nombreMetido || nombreMetido.trim() === "") ? "Jugador_Nuevo" : nombreMetido.trim();

        /*
            Si existe el contenedor de resultado,
            se muestra un mensaje de bienvenida personalizado.
        */
        if (resultado) {
            resultado.innerText = "Bienvenido, " + usuario;
        }

        /*
            Una vez validado el nombre, se solicitan los datos del panel.
            Así se cargan estadísticas y ranking desde el servidor.
        */
        refrescarPanel();
    }, 200);
};

/*
    Si el botón principal existe en el documento,
    se le asigna el evento click para generar un nuevo estímulo.
*/
if (boton) {
    boton.addEventListener("click", generarEstimulo);
}

/*
    Función principal del juego.
    Cada vez que el usuario pulsa el botón:
    - aumenta el contador de clics,
    - calcula el tiempo de reacción del estímulo anterior,
    - limpia la zona visual,
    - elige un nuevo estímulo aleatorio,
    - cambia el aspecto y la posición del botón.
*/
function generarEstimulo() {
    clicks++;

    const ahora = Date.now();

    /*
        Solo se registra un log si ya existía un estímulo anterior real.
        Esto evita guardar mediciones inválidas al primer clic.
    */
    if (tiempoInicioEstimulo > 0 && estimuloActual !== "") {
        const tiempoReaccionMs = ahora - tiempoInicioEstimulo;

        /*
            Se descartan tiempos excesivamente bajos,
            ya que probablemente no serían una reacción válida.
        */
        if (tiempoReaccionMs > 10) {
            registrarLog(estimuloActual, tiempoReaccionMs);
        }
    }

    /*
        Se reinicia el tiempo de inicio para medir el siguiente estímulo.
    */
    tiempoInicioEstimulo = ahora;

    /*
        Se limpia la zona donde se muestran imágenes o frases anteriores.
    */
    limpiarZonaVisual();

    /*
        Lista de estímulos posibles.
        Cada estímulo tiene:
        - nombre: identificador textual
        - fn: función que ejecuta el efecto asociado

        Esto permite una selección aleatoria flexible y limpia.
    */
    const acciones = [
        { nombre: "FONDO", fn: cambiarFondo },
        { nombre: "SONIDO", fn: reproducirSonido },
        { nombre: "IMAGEN", fn: mostrarImagen },
        { nombre: "FRASE", fn: mostrarFrase }
    ];

    /*
        Se elige una acción aleatoria del array.
    */
    const elegido = acciones[Math.floor(Math.random() * acciones.length)];
    estimuloActual = elegido.nombre;

    /*
        Se ejecuta la función del estímulo elegido
        y se actualiza la apariencia del botón.
    */
    elegido.fn();
    cambiarColorBoton();
    moverBoton();
    animarBoton();
}

/*
    Envía un registro de evento al backend.
    Utiliza fetch para hacer una petición POST al endpoint /log.
    El cuerpo de la petición se envía en formato JSON.
*/
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
        /*
            fetch devuelve una Promise.
            Aquí se comprueba si la respuesta HTTP es correcta antes de procesarla.
        */
        .then(response => {
            if (!response.ok) throw new Error("Error al guardar log");
            return response.json();
        })

        /*
            Si el registro se guarda correctamente,
            se actualiza el panel de estadísticas.
        */
        .then(() => refrescarPanel())

        /*
            Si falla la petición, se muestra el error en consola.
        */
        .catch(error => console.error("Error registrando evento en Ktor:", error));
}

/*
    Limpia el contenedor visual donde aparecen imágenes o frases.
*/
function limpiarZonaVisual() {
    if (imagenRandom) imagenRandom.innerHTML = "";
}

/*
    Cambia el color del botón principal usando clases de Bootstrap.
    Esto permite reutilizar estilos ya definidos por el framework.

    Clases utilizadas:
    - btn-primary
    - btn-success
    - btn-danger
    - btn-warning
    - btn-info
    - btn-dark

    Con este sistema, Bootstrap aporta consistencia visual sin necesidad
    de crear una clase CSS distinta para cada color.
*/
function cambiarColorBoton() {
    const colores = ["btn-primary", "btn-success", "btn-danger", "btn-warning", "btn-info", "btn-dark"];
    boton.classList.remove(...colores);
    boton.classList.add(colores[clicks % colores.length]);
}

/*
    Aplica una pequeña animación de escala al botón.
    Se amplía brevemente y luego vuelve a su tamaño normal.
*/
function animarBoton() {
    boton.style.transform = "scale(1.1)";
    setTimeout(() => { boton.style.transform = "scale(1)"; }, 150);
}

/*
    Mueve el botón a una posición aleatoria entre las esquinas definidas.
    Esto incrementa la dificultad y hace la interacción más dinámica.
*/
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

/*
    Cambia el fondo de la página por una imagen aleatoria.
    Se prueban varias extensiones posibles hasta encontrar una válida.
*/
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

/*
    Muestra una imagen aleatoria dentro del contenedor visual.
    Igual que en el fondo, se prueban varias extensiones posibles.
*/
function mostrarImagen() {
    const index = Math.floor(Math.random() * 28) + 1;
    const rutas = [`img/img${index}.png`, `img/img${index}.jpg`, `img/img${index}.jpeg`];

    cargarPrimeraValida(rutas, (url) => {
        imagenRandom.innerHTML = `<img src="${url}" alt="Imagen aleatoria" style="max-height: 300px; border-radius: 10px;">`;
    });
}

/*
    Muestra una frase aleatoria en pantalla.
    Se inserta dinámicamente en el contenedor imagenRandom.
*/
function mostrarFrase() {
    const frases = [
        "Nunca sabes lo que pasará al hacer click.",
        "Prueba tu suerte, ¡va a ser algo grande!",
        "Un regalo en cada click.",
        "Click, click, click...",
        "El orden es aburrido, ¡viva el caos!",
        "Solo un click más..."
    ];

    const frase = frases[Math.floor(Math.random() * frases.length)];

    imagenRandom.innerHTML = `<div class="frase h3 text-white p-3 rounded" style="background: rgba(0,0,0,0.6);">${frase}</div>`;
}

/*
    Reproduce un sonido aleatorio en formato mp3.
*/
function reproducirSonido() {
    const index = Math.floor(Math.random() * 28) + 1;
    new Audio(`sounds/sound${index}.mp3`).play().catch(() => {});
}

/*
    Función auxiliar que prueba varias rutas posibles de imagen.
    Cuando encuentra una válida, ejecuta la función onSuccess con esa URL.
    Esto evita errores si un recurso no existe en una extensión concreta.
*/
function cargarPrimeraValida(rutas, onSuccess) {
    let i = 0;

    function probar() {
        if (i >= rutas.length) return;

        const img = new Image();

        img.onload = () => onSuccess(rutas[i]);
        img.onerror = () => {
            i++;
            probar();
        };

        img.src = rutas[i];
    }

    probar();
}

/*
    Actualiza el contenido del panel lateral de estadísticas.
    Realiza varias peticiones al backend al mismo tiempo para obtener:
    - resumen general del usuario,
    - mejores tiempos por estímulo,
    - ranking global.

    Promise.all permite esperar todas las respuestas a la vez,
    lo que hace más eficiente la carga conjunta de datos.
*/
function refrescarPanel() {
    const contenidoPanel = document.getElementById("contenidoPanel");

    if (!contenidoPanel || !usuario) return;

    Promise.all([
        fetch(`/stats/${encodeURIComponent(usuario)}`).then(r => r.ok ? r.json() : null),
        fetch(`/stats/estimulos/${encodeURIComponent(usuario)}`).then(r => r.ok ? r.json() : null),
        fetch("/ranking").then(r => r.ok ? r.json() : [])
    ])
        .then(([stats, estimulos, ranking]) => {

            /*
                Bloque HTML del resumen principal del usuario.
                Aquí se usan varias clases de Bootstrap para maquetar y resaltar datos:
                - list-group
                - shadow-sm
                - rounded
                - border
                - text-warning
                - badge
                - bg-success
                - rounded-pill
                - text-info
            */
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

            /*
                Bloque HTML con los mejores resultados por tipo de estímulo.
                Se presenta en una cuadrícula responsive usando clases de Bootstrap.
            */
            let estimulosHtml = `<div class="mb-4"><h6 class="text-info text-uppercase small fw-bold mb-2">⏱️ Mejor por Estímulo</h6><div class="row g-2">`;

            if (estimulos && Object.keys(estimulos).length > 0) {
                for (const est in estimulos) {
                    estimulosHtml += `<div class="col-6"><div class="p-2 rounded border border-secondary text-center bg-dark bg-opacity-70"><small class="text-info text-uppercase fw-bold d-block">${est}</small><span class="fw-bold text-white">${estimulos[est]} ms</span></div></div>`;
                }
            } else {
                estimulosHtml += `<p class="text-muted small ps-2">Esperando eventos...</p>`;
            }

            estimulosHtml += `</div></div>`;

            /*
                Bloque HTML del ranking general.
                Se usa list-group-numbered de Bootstrap para mostrar
                una clasificación ordenada de jugadores.
            */
            let rankingHtml = `<div class="mb-3"><h6 class="text-info text-uppercase small fw-bold mb-3">🏆 Líderes (Media)</h6><ol class="list-group list-group-numbered border border-secondary rounded">`;

            if (ranking && ranking.length > 0) {
                ranking.forEach(fila => {
                    rankingHtml += `<li class="list-group-item bg-transparent text-white d-flex justify-content-between align-items-center border-secondary"><span class="${fila.usuario === usuario ? 'text-warning' : ''}">${fila.usuario}</span><span class="badge bg-info text-dark rounded-pill">${Math.round(fila.media ?? 0)} ms</span></li>`;
                });
            } else {
                rankingHtml += `<li class="list-group-item bg-transparent text-muted small border-0">Podio vacío.</li>`;
            }

            rankingHtml += `</ol></div>`;

            /*
                Se inserta todo el HTML generado dentro del panel lateral.
                Así el contenido se actualiza dinámicamente sin recargar la página.
            */
            contenidoPanel.innerHTML = statsHtml + estimulosHtml + rankingHtml;
        })

        /*
            Si alguna petición falla, se informa en consola.
        */
        .catch(err => console.error("Error en panel:", err));
}