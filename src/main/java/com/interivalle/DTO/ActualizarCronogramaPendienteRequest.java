package com.interivalle.DTO;

import java.time.LocalDate;
import java.util.List;

public class ActualizarCronogramaPendienteRequest {

    private LocalDate fechaInicio;
    private Integer totalSemanas;
    private List<DetallePendiente> detalles;

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Integer getTotalSemanas() {
        return totalSemanas;
    }

    public void setTotalSemanas(Integer totalSemanas) {
        this.totalSemanas = totalSemanas;
    }

    public List<DetallePendiente> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePendiente> detalles) {
        this.detalles = detalles;
    }

    public static class DetallePendiente {
        private Integer idDetalle;
        private String servicio;
        private String actividad;
        private String descripcion;
        private Integer semana;

        public Integer getIdDetalle() {
            return idDetalle;
        }

        public void setIdDetalle(Integer idDetalle) {
            this.idDetalle = idDetalle;
        }

        public String getServicio() {
            return servicio;
        }

        public void setServicio(String servicio) {
            this.servicio = servicio;
        }

        public String getActividad() {
            return actividad;
        }

        public void setActividad(String actividad) {
            this.actividad = actividad;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public Integer getSemana() {
            return semana;
        }

        public void setSemana(Integer semana) {
            this.semana = semana;
        }
    }
}
