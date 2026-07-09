package com.interivalle.DTO;

import java.math.BigDecimal;

public class ActividadMaterialV2Request {

    private Integer idActividad;
    private Integer idMaterial;
    private BigDecimal cantidad;
    private BigDecimal factor;
    private String modoCantidad;
    private String unidadMaterial;
    private Boolean activo;

    public Integer getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(Integer idActividad) {
        this.idActividad = idActividad;
    }

    public Integer getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(Integer idMaterial) {
        this.idMaterial = idMaterial;
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
