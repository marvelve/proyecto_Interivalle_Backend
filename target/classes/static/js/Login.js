document.getElementById("formLogin").addEventListener("submit", async (event) => {
    event.preventDefault();

    const mensaje = document.getElementById("mensaje");
    mensaje.textContent = "";

    const usuario = {
        correoUsuario: document.getElementById("correo_usuario").value,
        contrasenaUsuario: document.getElementById("contrasena_usuario").value
    };

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(usuario)
        });

        const data = await leerRespuesta(response);

        if (!response.ok) {
            throw new Error(data.message || data.error || data || "No se pudo iniciar sesion");
        }

        localStorage.setItem("authToken", data.token);
        localStorage.setItem("usuarioCorreo", data.correoUsuario);
        localStorage.setItem("usuarioRol", data.idRol);
        window.location.href = "ServiciosACotizar.html";
    } catch (error) {
        mensaje.textContent = error.message;
    }
});

async function leerRespuesta(response) {
    const texto = await response.text();
    if (!texto) {
        return "";
    }

    try {
        return JSON.parse(texto);
    } catch (error) {
        return texto;
    }
}
