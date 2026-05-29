package com.interivalle.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicios")
public class Servicios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicios")
    private Integer idServicios;

    @Column(name = "nombre_servicio", nullable = false, unique = true)
    private String nombreServicio;

    @Column(name = "activo")
    private Boolean activo = true;

    public Servicios() {
        // Constructor vacio requerido por JPA.
    }

    public Servicios(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public Integer getIdServicio() {
        return idServicios;
    }

    public void setIdServicio(Integer idServicios) {
        this.idServicios = idServicios;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
