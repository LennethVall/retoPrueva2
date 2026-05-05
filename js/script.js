let clicks = 0;

function misterio() {

    // 1. Contador
    clicks++;
    document.getElementById("contador").innerText = clicks;

    // 2. Lista de acciones aleatorias (solo una por click)
    const acciones = [
        cambiarFondo,
        reproducirSonido,
        mostrarImagen,
        mostrarFrase
    ];

    const accion = acciones[Math.floor(Math.random() * acciones.length)];
    accion(); // Ejecuta una acción aleatoria

    // 3. Cambiar color del botón
    const boton = document.getElementById("botonMisterioso");
    const colores = ["btn-primary", "btn-success", "btn-danger", "btn-warning", "btn-info", "btn-dark"];
    boton.classList.remove(...colores);
    boton.classList.add(colores[clicks % colores.length]);

    // 4. Llamada al backend
    fetch("/misterio")
        .then(r => r.text())
        .then(t => {
            document.getElementById("resultado").innerText = t;
        });

    // 5. Efecto de zoom rápido
    boton.style.transform = "scale(1.1)";
    setTimeout(() => boton.style.transform = "scale(1)", 150);
}


// ----------------------
// ACCIONES ALEATORIAS
// ----------------------

// 28 fondos numerados (.jpg)
function cambiarFondo() {
    const index = Math.floor(Math.random() * 28) + 1;
    const url = `/background/background${index}.jpg`;

    document.body.style.backgroundImage = `url('${url}')`;
    document.body.style.backgroundSize = "cover";
    document.body.style.backgroundPosition = "center";

    aplicarContrasteAutomatico(url);

    moverBoton();
}

// 28 sonidos numerados (.mp3)
function reproducirSonido() {
    const index = Math.floor(Math.random() * 28) + 1;
    const audio = new Audio(`sounds/sound${index}.mp3`);
    audio.play();
}

// 28 imágenes numeradas (.png)
function mostrarImagen() {
    const index = Math.floor(Math.random() * 28) + 1;
    document.getElementById("imagenRandom").innerHTML =
        `<img src="img/img${index}.png" alt="Random">`;
}

// Tus 28 frases
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
    document.getElementById("imagenRandom").innerHTML =
        `<div class="fs-4 fw-semibold">${frase}</div>`;
}
function moverBoton() {
    const boton = document.getElementById("botonMisterioso");

    // Activar estela
    boton.classList.add("moving");

    // Lista de posiciones posibles
    const posiciones = [
        { top: "20px", left: "20px" },                // esquina superior izquierda
        { top: "20px", right: "20px" },               // esquina superior derecha
        { bottom: "20px", left: "20px" },             // esquina inferior izquierda
        { bottom: "20px", right: "20px" }             // esquina inferior derecha
    ];

    // Elegir una posición aleatoria
    const pos = posiciones[Math.floor(Math.random() * posiciones.length)];

    // Resetear propiedades previas
    boton.style.top = "";
    boton.style.bottom = "";
    boton.style.left = "";
    boton.style.right = "";

    // Aplicar la nueva posición
    Object.assign(boton.style, pos);

    // Quitar la estela después del movimiento
    setTimeout(() => {
        boton.classList.remove("moving");
    }, 600);
}
function aplicarContrasteAutomatico(urlImagen) {
    const img = new Image();
    img.src = urlImagen;
    img.crossOrigin = "anonymous";

    img.onload = function () {
        // Crear un canvas temporal para analizar la imagen
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");

        canvas.width = img.width;
        canvas.height = img.height;

        ctx.drawImage(img, 0, 0);

        // Obtener datos de píxeles
        const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data;

        let r, g, b, avg;
        let total = 0;

        // Muestreo cada 20 píxeles para no saturar
        for (let i = 0; i < data.length; i += 80) {
            r = data[i];
            g = data[i + 1];
            b = data[i + 2];
            avg = (r + g + b) / 3;
            total += avg;
        }

        const luminosidad = total / (data.length / 80);

        // Seleccionar estilo según luminosidad
        const elementos = document.querySelectorAll("#contador, #resultado, #imagenRandom");

        if (luminosidad < 128) {
            // Fondo oscuro → texto claro
            elementos.forEach(el => {
                el.classList.remove("text-negro-borde-blanco");
                el.classList.add("text-blanco-borde-negro");
            });
        } else {
            // Fondo claro → texto oscuro
            elementos.forEach(el => {
                el.classList.remove("text-blanco-borde-negro");
                el.classList.add("text-negro-borde-blanco");
            });
        }
    };
}
