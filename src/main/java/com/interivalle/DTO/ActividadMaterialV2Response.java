package com.interivalle.DTO;

import java.math.BigDecimal;

public class ActividadMaterialV2Response {

    private Integer idActividadMaterialV2;
    private Integer idActividad;
    private String nombreActividad;
    private Integer idMaterial;
    private String nombreMaterial;
    private Integer idServicio;
    private String nombreServicio;
    private BigDecimal cantidad;
    private BigDecimal factor;
    private String modoCantidad;
    private String unidadMaterial;
    private Boolean activo;

    public Integer getIdActividadMaterialV2() {
        return idActividadMaterialV2;
    }

    public void setIdActividadMaterialV2(Integer idActividadMaterialV2) {
        this.idActividadMaterialV2 = idActividadMaterialV2;
    }

    public Integer getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(Integer idActividad) {
        this.idActividad = idActividad;
    }

    public String getNombreActividad() {
        return nombreActividad;
    }

    public void setNombreActividad(String nombreActividad) {
        this.nombreActividad = nombreActividad;
    }

    public Integer getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(Integer idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getNombreMaterial() {
        return nombreMaterial;
    }

    public void setNombreMaterial(String nombreMaterial) {
        this.nombreMaterial = nombreMaterial;
    }

    public Integer getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(Integer idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public String getModoCantidad() {
        return modoCantidad;
    }

    public void setModoCantidad(String modoCantidad) {
        this.modoCantidad = modoCantidad;
    }

    public String getUnidadMaterial() {
        return unidadMaterial;
    }

    public void setUnidadMaterial(String unidadMaterial) {
        this.unidadMaterial = unidadMaterial;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
