package com.interivalle.DTO;

public class ActualizarCronogramaDetalleRequest {

    private String trabajador;
    private String novedades;

    public String getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(String trabajador) {
        this.trabajador = trabajador;
    }

    public String getNovedades() {
        return novedades;
    }

    public void setNovedades(String novedades) {
        this.novedades = novedades;
    }
}
