package com.interivalle.DTO;

import jakarta.validation.constraints.NotNull;

public class RolUpdate {

    // Roles validos: 1=ADMIN, 2=SUPERVISOR, 3=CLIENTE.
    @NotNull
    private Integer idRol;

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }
}
