package com.interivalle.DTO;

import java.time.LocalDate;

public class FechaInicioDisponibleResponse {

    private LocalDate fechaInicio;
    private Long cantidadProyectos;
    private Boolean disponible;

    public FechaInicioDisponibleResponse() {
    }

    public FechaInicioDisponibleResponse(LocalDate fechaInicio, Long cantidadProyectos, Boolean disponible) {
        this.fechaInicio = fechaInicio;
        this.cantidadProyectos = cantidadProyectos;
        this.disponible = disponible;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Long getCantidadProyectos() {
        return cantidadProyectos;
    }

    public void setCantidadProyectos(Long cantidadProyectos) {
        this.cantidadProyectos = cantidadProyectos;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }
}
