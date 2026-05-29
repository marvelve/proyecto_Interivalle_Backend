package com.interivalle.DTO;

import java.math.BigDecimal;

public class CatalogoItemResponse {

    // En el catalogo V2 este id es codificado para que sea unico entre actividad, material y producto.
    private Integer idCatalogoItem;
    private Integer idItemOrigen;
    private String nombreItem;
    private String categoria;
    private String tipoItem;
    private Integer idServicio;
    private String nombreServicio;
    private BigDecimal precioUnitarioVenta;
    private BigDecimal precioUnitarioProveedor;
    private Boolean activo;
    private String tablaOrigen;
    private Integer relacionesV2;

    public Integer getIdCatalogoItem() {
        return idCatalogoItem;
    }

    public void setIdCatalogoItem(Integer idCatalogoItem) {
        this.idCatalogoItem = idCatalogoItem;
    }

    public Integer getIdItemOrigen() {
        return idItemOrigen;
    }

    public void setIdItemOrigen(Integer idItemOrigen) {
        this.idItemOrigen = idItemOrigen;
    }

    public String getNombreItem() {
        return nombreItem;
    }

    public void setNombreItem(String nombreItem) {
        this.nombreItem = nombreItem;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTipoItem() {
        return tipoItem;
    }

    public void setTipoItem(String tipoItem) {
        this.tipoItem = tipoItem;
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

    public BigDecimal getPrecioUnitarioVenta() {
        return precioUnitarioVenta;
    }

    public void setPrecioUnitarioVenta(BigDecimal precioUnitarioVenta) {
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    public BigDecimal getPrecioUnitarioProveedor() {
        return precioUnitarioProveedor;
    }

    public void setPrecioUnitarioProveedor(BigDecimal precioUnitarioProveedor) {
        this.precioUnitarioProveedor = precioUnitarioProveedor;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getTablaOrigen() {
        return tablaOrigen;
    }

    public void setTablaOrigen(String tablaOrigen) {
        this.tablaOrigen = tablaOrigen;
    }

    public Integer getRelacionesV2() {
        return relacionesV2;
    }

    public void setRelacionesV2(Integer relacionesV2) {
        this.relacionesV2 = relacionesV2;
    }
}
