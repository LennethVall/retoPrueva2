let clicks = 0;

const boton = document.getElementById("botonMisterioso");
const contador = document.getElementById("contador");
const resultado = document.getElementById("resultado");
const imagenRandom = document.getElementById("imagenRandom");

boton.addEventListener("click", misterio);

function misterio() {
    clicks++;
    contador.innerText = clicks;

    const acciones = [
        cambiarFondo,
        reproducirSonido,
        mostrarImagen,
        mostrarFrase
    ];

    const accion = acciones[Math.floor(Math.random() * acciones.length)];
    accion();

    cambiarColorBoton();
    moverBoton();
    animarBoton();
}

function cambiarColorBoton() {
    const colores = [
        "btn-primary",
        "btn-success",
        "btn-danger",
        "btn-warning",
        "btn-info",
        "btn-dark"
    ];

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
    boton.classList.add("moving");

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

    setTimeout(() => {
        boton.classList.remove("moving");
    }, 600);
}

function cambiarFondo() {
    const index = Math.floor(Math.random() * 28) + 1;
    const posiblesRutas = [
        `background/background${index}.jpg`,
        `background/background${index}.png`
    ];

    cargarPrimeraImagenValida(
        posiblesRutas,
        function (urlValida) {
            document.body.style.backgroundImage = `url('${urlValida}')`;
            document.body.style.backgroundSize = "cover";
            document.body.style.backgroundPosition = "center";
            document.body.style.backgroundRepeat = "no-repeat";

            aplicarContrasteAutomatico(urlValida);
            resultado.innerText = "¡Fondo cambiado!";
        },
        function () {
            resultado.innerText = "No se encontró ningún fondo válido.";
        }
    );
}

function mostrarImagen() {
    const index = Math.floor(Math.random() * 28) + 1;
    const posiblesRutas = [
        `img/img${index}.png`,
        `img/img${index}.jpg`,
        `img/img${index}.jpeg`
    ];

    cargarPrimeraImagenValida(
        posiblesRutas,
        function (urlValida) {
            imagenRandom.innerHTML = `<img src="${urlValida}" alt="Imagen aleatoria">`;
            resultado.innerText = "¡Ha aparecido una imagen!";
        },
        function () {
            resultado.innerText = "No se encontró ninguna imagen válida.";
        }
    );
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
    resultado.innerText = "¡Ha aparecido una frase!";
}

function reproducirSonido() {
    const index = Math.floor(Math.random() * 28) + 1;
    const audio = new Audio(`sounds/sound${index}.mp3`);

    audio.play()
        .then(() => {
            resultado.innerText = "¡Suena un audio!";
        })
        .catch(() => {
            resultado.innerText = "No se pudo reproducir el sonido.";
        });
}

function cargarPrimeraImagenValida(rutas, onSuccess, onError) {
    let i = 0;

    function probarSiguiente() {
        if (i >= rutas.length) {
            onError();
            return;
        }

        const img = new Image();

        img.onload = function () {
            onSuccess(rutas[i]);
        };

        img.onerror = function () {
            i++;
            probarSiguiente();
        };

        img.src = rutas[i];
    }

    probarSiguiente();
}

function aplicarContrasteAutomatico(urlImagen) {
    const img = new Image();
    img.src = urlImagen;

    img.onload = function () {
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");

        canvas.width = img.width;
        canvas.height = img.height;

        ctx.drawImage(img, 0, 0);

        const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data;

        let total = 0;

        for (let i = 0; i < data.length; i += 80) {
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];
            total += (r + g + b) / 3;
        }

        const luminosidad = total / (data.length / 80);
        const elementos = document.querySelectorAll("#contador, #resultado, #imagenRandom, .contador-box");

        if (luminosidad < 128) {
            elementos.forEach(el => {
                el.classList.remove("text-negro-borde-blanco");
                el.classList.add("text-blanco-borde-negro");
            });
        } else {
            elementos.forEach(el => {
                el.classList.remove("text-blanco-borde-negro");
                el.classList.add("text-negro-borde-blanco");
            });
        }
    };
}