package com.interivalle.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VidrioBaseRequest {

    @NotNull(message = "La cantidad de baños es obligatoria")
    @Min(value = 1, message = "La cantidad de baños debe ser mayor a 0")
    private Integer cantidadBanos;

    @NotBlank(message = "Debe indicar si es corrediza o batiente")
    private String tipoApertura; // CORREDIZA o BATIENTE

    @NotBlank(message = "Debe indicar el color de accesorios")
    private String colorAccesorios; // NEGROS o PLATEADOS

    private Boolean tieneNicho;

    public Integer getCantidadBanos() {
        return cantidadBanos;
    }

    public void setCantidadBanos(Integer cantidadBanos) {
        this.cantidadBanos = cantidadBanos;
    }

    public String getTipoApertura() {
        return tipoApertura;
    }

    public void setTipoApertura(String tipoApertura) {
        this.tipoApertura = tipoApertura;
    }

    public String getColorAccesorios() {
        return colorAccesorios;
    }

    public void setColorAccesorios(String colorAccesorios) {
        this.colorAccesorios = colorAccesorios;
    }

    public Boolean getTieneNicho() {
        return tieneNicho;
    }

    public void setTieneNicho(Boolean tieneNicho) {
        this.tieneNicho = tieneNicho;
    }
}
