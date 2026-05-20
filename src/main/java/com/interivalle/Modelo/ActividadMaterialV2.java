/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 *
 * @author mary_
 */
@Entity
@Table(name = "actividad_material_v2")
public class ActividadMaterialV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad_material_v2")
    private Integer idActividadMaterialV2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material", nullable = false)
    private Material material;

    @Column(name = "semana", nullable = false)
    private Integer semana;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "factor")
    private BigDecimal factor;

    @Column(name = "modo_cantidad")
    private String modoCantidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Integer getIdActividadMaterialV2() {
        return idActividadMaterialV2;
    }

    public void setIdActividadMaterialV2(Integer idActividadMaterialV2) {
        this.idActividadMaterialV2 = idActividadMaterialV2;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Integer getSemana() {
        return semana;
    }

    public void setSemana(Integer semana) {
        this.semana = semana;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    
}
