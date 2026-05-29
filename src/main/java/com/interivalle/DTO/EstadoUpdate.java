package com.interivalle.DTO;

import jakarta.validation.constraints.NotNull;

public class EstadoUpdate {

    // true = activo, false = inactivo.
    @NotNull
    private Boolean estadoUsuario;

    public Boolean getEstadoUsuario() {
        return estadoUsuario;
    }

    public void setEstadoUsuario(Boolean estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }
}
