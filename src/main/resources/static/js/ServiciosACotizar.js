const STORAGE_TOKEN = "authToken";
const STORAGE_CORREO = "usuarioCorreo";
const STORAGE_SOLICITUD = "solicitudCotizacionBase";
const STORAGE_SERVICIOS = "serviciosCotizacionBase";

window.addEventListener("load", () => {
    const token = localStorage.getItem(STORAGE_TOKEN);
    const correo = localStorage.getItem(STORAGE_CORREO);

    if (!token || !correo) {
        alert("Debe iniciar sesion primero");
        window.location.replace("Login.html");
    }
});

document.addEventListener("DOMContentLoaded", () => {
    const opcionCotizacion = document.getElementById("opcionCotizacion");
    const opcionVisita = document.getElementById("opcionVisita");
    const bloqueCotizacion = document.getElementById("bloqueCotizacion");
    const bloqueVisita = document.getElementById("bloqueVisita");
    const btnCotizar = document.getElementById("btnCotizar");
    const btnAgendarVisita = document.getElementById("btnAgendarVisita");

    opcionCotizacion.addEventListener("change", () => {
        bloqueCotizacion.hidden = false;
        bloqueVisita.hidden = true;
    });

    opcionVisita.addEventListener("change", () => {
        bloqueVisita.hidden = false;
        bloqueCotizacion.hidden = true;
    });

    btnCotizar.addEventListener("click", crearSolicitudCotizacion);
    btnAgendarVisita.addEventListener("click", crearSolicitudVisita);
});

async function crearSolicitudCotizacion() {
    const serviciosSeleccionados = obtenerServiciosSeleccionados();

    if (serviciosSeleccionados.length === 0) {
        alert("Debe seleccionar al menos un servicio");
        return;
    }

    const nombreProyecto = document.getElementById("nombre_proyecto_usuario").value.trim();

    if (!nombreProyecto) {
        alert("Debe ingresar el nombre del proyecto");
        return;
    }

    const data = {
        correoUsuario: localStorage.getItem(STORAGE_CORREO),
        tipoSolicitud: "COTIZACION_BASE",
        nombreProyecto,
        servicios: serviciosSeleccionados
    };

    try {
        const solicitud = await enviarSolicitud(data);
        localStorage.setItem(STORAGE_SOLICITUD, JSON.stringify(solicitud));
        localStorage.setItem(STORAGE_SERVICIOS, JSON.stringify(serviciosSeleccionados));
        window.location.href = "CotizacionBase.html";
    } catch (error) {
        alert(error.message);
    }
}

async function crearSolicitudVisita() {
    const nombreProyecto = document.getElementById("nombre_proyecto_visita").value.trim();
    const fechaVisita = document.getElementById("fechaVisita").value;
    const horaVisita = document.getElementById("horaVisita").value;
    const direccionVisita = document.getElementById("direccionVisita").value.trim();
    const celularCliente = document.getElementById("celularCliente").value.trim();

    if (!nombreProyecto || !fechaVisita || !horaVisita || !direccionVisita || !celularCliente) {
        alert("Complete los datos de la visita tecnica");
        return;
    }

    const data = {
        correoUsuario: localStorage.getItem(STORAGE_CORREO),
        tipoSolicitud: "VISITA_TECNICA",
        nombreProyecto,
        fechaVisita,
        horaVisita,
        direccionVisita,
        celularCliente
    };

    try {
        await enviarSolicitud(data);
        alert("Visita tecnica agendada correctamente");
    } catch (error) {
        alert(error.message);
    }
}

function obtenerServiciosSeleccionados() {
    return Array.from(document.querySelectorAll(".servicioCheck:checked"))
        .map((check) => Number(check.value))
        .filter((id) => Number.isFinite(id));
}

async function enviarSolicitud(data) {
    const respuesta = await fetch("/api/solicitudes", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${localStorage.getItem(STORAGE_TOKEN)}`
        },
        body: JSON.stringify(data)
    });

    const body = await leerRespuesta(respuesta);

    if (!respuesta.ok) {
        throw new Error(body.message || body.error || body || "No se pudo guardar la solicitud");
    }

    return body;
}

async function leerRespuesta(respuesta) {
    const texto = await respuesta.text();
    if (!texto) {
        return "";
    }

    try {
        return JSON.parse(texto);
    } catch (error) {
        return texto;
    }
}

function cerrarSesion() {
    localStorage.removeItem(STORAGE_CORREO);
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_SOLICITUD);
    localStorage.removeItem(STORAGE_SERVICIOS);
    window.location.replace("Login.html");
}
