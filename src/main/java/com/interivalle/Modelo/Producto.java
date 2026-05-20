package com.interivalle.Modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private Servicios servicio;

    @Column(name = "id_catalogo_item_origen")
    private Integer idCatalogoItemOrigen;

    @Column(name = "codigo", length = 100)
    private String codigo;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "unidad", length = 50)
    private String unidad;

    @Column(name = "modo_precio", length = 30)
    private String modoPrecio;
    
    @Column(name = "formula_code", length = 80)
    private String formula_code;

    @Column(name = "variable_base", length = 80)
    private String variable_base;

    @Column(name = "factor", precision = 12, scale = 4)
    private BigDecimal factor;

    @Column(name = "precio_unitario_venta", precision = 12, scale = 2)
    private BigDecimal precioUnitarioVenta;

    @Column(name = "precio_unitario_proveedor", precision = 12, scale = 2)
    private BigDecimal precioUnitarioProveedor;

    @Column(name = "semana")
    private Integer semana;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_por")
    private Integer creadoPor;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Servicios getServicio() {
        return servicio;
    }

    public void setServicio(Servicios servicio) {
        this.servicio = servicio;
    }

    public Integer getIdCatalogoItemOrigen() {
        return idCatalogoItemOrigen;
    }

    public void setIdCatalogoItemOrigen(Integer idCatalogoItemOrigen) {
        this.idCatalogoItemOrigen = idCatalogoItemOrigen;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getModoPrecio() {
        return modoPrecio;
    }

    public void setModoPrecio(String modoPrecio) {
        this.modoPrecio = modoPrecio;
    }

    public String getFormula_code() {
        return formula_code;
    }

    public void setFormula_code(String formula_code) {
        this.formula_code = formula_code;
    }

    public String getFormulaCode() {
        return formula_code;
    }

    public void setFormulaCode(String formulaCode) {
        this.formula_code = formulaCode;
    }

    public String getVariable_base() {
        return variable_base;
    }

    public void setVariable_base(String variable_base) {
        this.variable_base = variable_base;
    }

    public String getVariableBase() {
        return variable_base;
    }

    public void setVariableBase(String variableBase) {
        this.variable_base = variableBase;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
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

    public Integer getSemana() {
        return semana;
    }

    public void setSemana(Integer semana) {
        this.semana = semana;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Integer creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
