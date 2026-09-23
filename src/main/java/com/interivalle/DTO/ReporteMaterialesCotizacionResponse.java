package com.interivalle.DTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReporteMaterialesCotizacionResponse {

    private Integer idCotizacion;
    private String nombreProyecto;
    private String cliente;
    private String estado;
    private List<MaterialCotizacion> materiales = new ArrayList<>();
    private List<ActividadAdicional> actividadesAdicionales = new ArrayList<>();
    private BigDecimal totalMateriales = BigDecimal.ZERO;
    private BigDecimal totalActividadesAdicionales = BigDecimal.ZERO;
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    public Integer getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(Integer idCotizacion) { this.idCotizacion = idCotizacion; }
    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<MaterialCotizacion> getMateriales() { return materiales; }
    public void setMateriales(List<MaterialCotizacion> materiales) { this.materiales = materiales; }
    public List<ActividadAdicional> getActividadesAdicionales() { return actividadesAdicionales; }
    public void setActividadesAdicionales(List<ActividadAdicional> actividadesAdicionales) {
        this.actividadesAdicionales = actividadesAdicionales;
    }
    public BigDecimal getTotalMateriales() { return totalMateriales; }
    public void setTotalMateriales(BigDecimal totalMateriales) { this.totalMateriales = totalMateriales; }
    public BigDecimal getTotalActividadesAdicionales() { return totalActividadesAdicionales; }
    public void setTotalActividadesAdicionales(BigDecimal totalActividadesAdicionales) {
        this.totalActividadesAdicionales = totalActividadesAdicionales;
    }
    public BigDecimal getTotalGeneral() { return totalGeneral; }
    public void setTotalGeneral(BigDecimal totalGeneral) { this.totalGeneral = totalGeneral; }

    public static class MaterialCotizacion {
        private String nombreProyecto;
        private Integer numeroCotizacion;
        private String cliente;
        private String servicio;
        private String actividad;
        private String material;
        private String unidad;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public String getNombreProyecto() { return nombreProyecto; }
        public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }
        public Integer getNumeroCotizacion() { return numeroCotizacion; }
        public void setNumeroCotizacion(Integer numeroCotizacion) { this.numeroCotizacion = numeroCotizacion; }
        public String getCliente() { return cliente; }
        public void setCliente(String cliente) { this.cliente = cliente; }
        public String getServicio() { return servicio; }
        public void setServicio(String servicio) { this.servicio = servicio; }
        public String getActividad() { return actividad; }
        public void setActividad(String actividad) { this.actividad = actividad; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public BigDecimal getCantidad() { return cantidad; }
        public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }

    public static class ActividadAdicional {
        private String servicio;
        private String actividad;
        private String lugar;
        private String unidad;
        private Integer cantidad;
        private BigDecimal medida;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public String getServicio() { return servicio; }
        public void setServicio(String servicio) { this.servicio = servicio; }
        public String getActividad() { return actividad; }
        public void setActividad(String actividad) { this.actividad = actividad; }
        public String getLugar() { return lugar; }
        public void setLugar(String lugar) { this.lugar = lugar; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public BigDecimal getMedida() { return medida; }
        public void setMedida(BigDecimal medida) { this.medida = medida; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}
