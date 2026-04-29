const STORAGE_SOLICITUD = "solicitudCotizacionBase";
const STORAGE_SERVICIOS = "serviciosCotizacionBase";
const STORAGE_TOKEN = "authToken";
const STORAGE_CORREO = "usuarioCorreo";

const formatoMoneda = new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0
});

document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem(STORAGE_TOKEN);
    const solicitud = obtenerSolicitudGuardada();

    if (!token || !solicitud || !solicitud.idSolicitud) {
        window.location.replace("ServiciosACotizar.html");
        return;
    }

    const serviciosSeleccionados = obtenerServiciosSeleccionados(solicitud);
    configurarSecciones(serviciosSeleccionados);

    document.getElementById("nombreProyecto").textContent =
        solicitud.nombreProyecto ? `Proyecto: ${solicitud.nombreProyecto}` : "";

    document.getElementById("btnVolver").addEventListener("click", () => {
        window.location.href = "ServiciosACotizar.html";
    });

    document.getElementById("formCotizar").addEventListener("submit", async (event) => {
        event.preventDefault();
        await generarCotizacion(solicitud.idSolicitud, serviciosSeleccionados, token);
    });
});

function obtenerSolicitudGuardada() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_SOLICITUD));
    } catch (error) {
        return null;
    }
}

function obtenerServiciosSeleccionados(solicitud) {
    if (Array.isArray(solicitud.solicitudServicios)) {
        return solicitud.solicitudServicios
            .map((item) => Number(item.idServicio))
            .filter((id) => Number.isFinite(id));
    }

    try {
        return JSON.parse(localStorage.getItem(STORAGE_SERVICIOS)) || [];
    } catch (error) {
        return [];
    }
}

function configurarSecciones(serviciosSeleccionados) {
    document.querySelectorAll("[data-servicio]").forEach((seccion) => {
        const idServicio = Number(seccion.dataset.servicio);
        const visible = serviciosSeleccionados.includes(idServicio);
        seccion.hidden = !visible;

        seccion.querySelectorAll("input, select").forEach((campo) => {
            campo.disabled = !visible;
            campo.required = visible && campo.type !== "checkbox";
        });
    });

    document.querySelectorAll("#seccionManoObra input[min='0'], #seccionCarpinteria input[min='0']").forEach((campo) => {
        if (campo.value === "") {
            campo.value = "0";
        }
    });
}

async function generarCotizacion(idSolicitud, serviciosSeleccionados, token) {
    const mensaje = document.getElementById("mensaje");
    mensaje.textContent = "Generando cotizacion...";
    mensaje.classList.remove("error");

    try {
        const payload = construirPayload(idSolicitud, serviciosSeleccionados);

        const respuesta = await fetch("/api/cliente/cotizaciones/generar-base", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        const data = await leerRespuesta(respuesta);

        if (!respuesta.ok) {
            throw new Error(data.message || data.error || data || "No se pudo generar la cotizacion");
        }

        mensaje.textContent = data.mensaje || "Cotizacion generada correctamente";

        const detalle = await obtenerDetalleCotizacion(data.idCotizacion, token);
        renderizarDetalle(detalle);
    } catch (error) {
        mensaje.textContent = error.message;
        mensaje.classList.add("error");
    }
}

function construirPayload(idSolicitud, serviciosSeleccionados) {
    const payload = { solicitudId: idSolicitud };

    if (serviciosSeleccionados.includes(1)) {
        payload.manoObra = {
            medidaAreaPrivada: numero("manoAreaPrivada"),
            cantidadBanos: entero("manoCantidadBanos"),
            tipoCielo: valor("manoTipoCielo"),
            divisionPared: valor("manoDivisionPared") === "true",
            metrosCuadradosPanelYeso: decimalOpcional("manoM2PanelYeso"),
            cantidadPoyos: enteroOpcional("manoCantidadPoyos"),
            cantidadPuntosElectricos: enteroOpcional("manoPuntosElectricos"),
            metrosCuadradosMuro: decimalOpcional("manoM2Muro"),
            metrosCuadradosCielo: decimalOpcional("manoM2Cielo"),
            metrosCuadradosTaparTuberias: decimalOpcional("manoM2Tuberias")
        };
    }

    if (serviciosSeleccionados.includes(2)) {
        payload.carpinteria = {
            cantidadCloset: entero("carpCloset"),
            cantidadPuertas: entero("carpPuertas"),
            muebleAltoCocina: numero("carpMuebleAltoCocina"),
            muebleBajoCocina: numero("carpMuebleBajoCocina"),
            cantidadBanos: entero("carpCantidadBanos"),
            cantidadMuebleBajoBano: entero("carpMuebleBajoBano"),
            cantidadMuebleAltoBano: entero("carpMuebleAltoBano")
        };
    }

    if (serviciosSeleccionados.includes(3)) {
        payload.vidrio = {
            cantidadBanos: entero("vidrioCantidadBanos"),
            tipoApertura: valor("vidrioTipoApertura"),
            colorAccesorios: valor("vidrioColorAccesorios")
        };
    }

    if (serviciosSeleccionados.includes(4)) {
        payload.mezon = {
            mezonCocina: checked("mezonCocina"),
            mezonBarra: checked("mezonBarra"),
            mezonLavamanos: checked("mezonLavamanos")
        };
    }

    return payload;
}

async function obtenerDetalleCotizacion(idCotizacion, token) {
    const respuesta = await fetch(`/api/cliente/cotizaciones/${idCotizacion}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    const data = await leerRespuesta(respuesta);

    if (!respuesta.ok) {
        throw new Error(data.message || data.error || data || "La cotizacion se genero, pero no se pudo consultar el detalle");
    }

    return data;
}

function renderizarDetalle(detalle) {
    document.getElementById("resultado").hidden = false;
    document.getElementById("totalManoObra").textContent = moneda(detalle.totalManoObra);
    document.getElementById("totalMateriales").textContent = moneda(detalle.totalMateriales);
    document.getElementById("totalProductos").textContent = moneda(detalle.totalProductos);
    document.getElementById("totalEstimado").textContent = moneda(detalle.totalEstimado);

    const tbody = document.getElementById("tablaCotizacion");
    tbody.innerHTML = "";

    const detalles = Array.isArray(detalle.detalles) ? detalle.detalles : [];

    if (detalles.length === 0) {
        const fila = document.createElement("tr");
        const celda = document.createElement("td");
        celda.colSpan = 6;
        celda.textContent = "No hay items de catalogo para los servicios seleccionados.";
        fila.appendChild(celda);
        tbody.appendChild(fila);
        return;
    }

    detalles.forEach((item) => {
        const fila = document.createElement("tr");
        fila.appendChild(celda(item.nombreServicio || ""));
        fila.appendChild(celda(item.tipoItem || ""));
        fila.appendChild(celda(item.descripcion || item.actividadMaterial || ""));
        fila.appendChild(celda(item.cantidad ?? ""));
        fila.appendChild(celda(moneda(item.precioUnitarioVenta)));
        fila.appendChild(celda(moneda(item.subtotalVenta)));
        tbody.appendChild(fila);
    });
}

function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto;
    return td;
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

function valor(id) {
    return document.getElementById(id).value;
}

function numero(id) {
    const value = valor(id);
    return value === "" ? 0 : Number(value);
}

function entero(id) {
    return Math.trunc(numero(id));
}

function decimalOpcional(id) {
    const value = valor(id);
    return value === "" ? null : Number(value);
}

function enteroOpcional(id) {
    const value = valor(id);
    return value === "" ? null : Math.trunc(Number(value));
}

function checked(id) {
    return document.getElementById(id).checked;
}

function moneda(valor) {
    return formatoMoneda.format(Number(valor || 0));
}

function cerrarSesion() {
    localStorage.removeItem(STORAGE_CORREO);
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_SOLICITUD);
    localStorage.removeItem(STORAGE_SERVICIOS);
    window.location.replace("Login.html");
}
