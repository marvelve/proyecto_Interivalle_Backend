package com.interivalle.DTO;

import jakarta.validation.constraints.Size;

public class UsuarioUpdate {

    // Campos permitidos para actualizar datos basicos del usuario.
    @Size(min = 5)
    private String nombreUsuario;

    private String celularUsuario;
    private String ciudadUsuario;

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCelularUsuario() {
        return celularUsuario;
    }

    public void setCelularUsuario(String celularUsuario) {
        this.celularUsuario = celularUsuario;
    }

    public String getCiudadUsuario() {
        return ciudadUsuario;
    }

    public void setCiudadUsuario(String ciudadUsuario) {
        this.ciudadUsuario = ciudadUsuario;
    }
}
