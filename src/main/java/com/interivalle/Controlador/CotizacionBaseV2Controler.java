package com.interivalle.Controlador;

import com.interivalle.DTO.GenerarCotizacionBaseRequest;
import com.interivalle.Modelo.Actividad;
import com.interivalle.Modelo.ActividadMaterialV2;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionDetalle;
import com.interivalle.Modelo.Material;
import com.interivalle.Modelo.Producto;
import com.interivalle.Repositorio.ActividadMaterialV2Repositorio;
import com.interivalle.Repositorio.ActividadRepositorio;
import com.interivalle.Repositorio.CotizacionDetalleRepositorio;
import com.interivalle.Repositorio.ProductoRepositorio;
import com.interivalle.Servicio.CotizacionBaseV2Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pruebas")
@CrossOrigin(origins = "*")
public class CotizacionBaseV2Controler {

    @Autowired
    private CotizacionBaseV2Service cotizacionBaseV2Service;

    @Autowired
    private CotizacionDetalleRepositorio cotizacionDetalleRepositorio;

    @Autowired
    private ActividadRepositorio actividadRepositorio;

    @Autowired
    private ActividadMaterialV2Repositorio actividadMaterialV2Repositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @PostMapping("/cotizacion-base-v2")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    public ResponseEntity<?> generarCotizacionBaseV2(@RequestBody GenerarCotizacionBaseRequest request) {
        Cotizacion cotizacion = cotizacionBaseV2Service.generarCotizacionBaseV2(request);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Cotizacion base V2 generada correctamente con nuevas tablas");
        response.put("idCotizacion", cotizacion.getIdCotizacion());
        response.put("estado", cotizacion.getEstado());
        response.put("tipo", cotizacion.getTipo());
        response.put("totalManoObra", valorSeguro(cotizacion.getTotalManoObra()));
        response.put("totalMateriales", valorSeguro(cotizacion.getTotalMateriales()));
        response.put("totalProductos", valorSeguro(cotizacion.getTotalProductos()));
        response.put("totalEstimado", valorSeguro(cotizacion.getTotalEstimado()));

        if (cotizacion.getSolicitud() != null) {
            response.put("idSolicitud", cotizacion.getSolicitud().getIdSolicitud());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cotizacion-base-v2/{idCotizacion}/detalles")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> verDetallesCotizacionV2(@PathVariable Integer idCotizacion) {
        List<Map<String, Object>> detalles = cotizacionDetalleRepositorio
                .findByCotizacion_IdCotizacionOrderBySemanaAsc(idCotizacion)
                .stream()
                .map(this::mapearDetalle)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("idCotizacion", idCotizacion);
        response.put("cantidadDetalles", detalles.size());
        response.put("detalles", detalles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalogo-v2/actividades/{idServicio}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarActividadesPorServicio(@PathVariable Integer idServicio) {
        List<Map<String, Object>> actividades = actividadRepositorio
                .findByServicio_IdServiciosAndActivoTrueOrderBySemanaAscOrdenAsc(idServicio)
                .stream()
                .map(this::mapearActividad)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("idServicio", idServicio);
        response.put("tabla", "actividad");
        response.put("cantidadActividades", actividades.size());
        response.put("actividades", actividades);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalogo-v2/productos/{idServicio}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarProductosPorServicio(@PathVariable Integer idServicio) {
        List<Map<String, Object>> productos = productoRepositorio
                .findByServicio_IdServiciosAndActivoTrueOrderBySemanaAscIdProductoAsc(idServicio)
                .stream()
                .map(this::mapearProducto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("idServicio", idServicio);
        response.put("tabla", "producto");
        response.put("cantidadProductos", productos.size());
        response.put("productos", productos);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalogo-v2/materiales-actividad/{idActividad}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarMaterialesDeActividad(@PathVariable Integer idActividad) {
        List<Map<String, Object>> relaciones = actividadMaterialV2Repositorio
                .findByActividad_IdActividadAndActivoTrueOrderBySemanaAscIdActividadMaterialV2Asc(idActividad)
                .stream()
                .map(this::mapearRelacionMaterial)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("idActividad", idActividad);
        response.put("cantidadMaterialesRelacionados", relaciones.size());
        response.put("relaciones", relaciones);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapearDetalle(CotizacionDetalle detalle) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idDetalle", detalle.getIdDetalle());
        item.put("tipoItem", detalle.getTipoItem());
        item.put("categoria", detalle.getCategoria());
        item.put("semana", detalle.getSemana());
        item.put("descripcion", detalle.getDescripcion());
        item.put("actividadMaterial", detalle.getActividadMaterial());
        item.put("cantidad", detalle.getCantidad());
        item.put("unidad", detalle.getUnidad());
        item.put("precioUnitarioVenta", detalle.getPrecioUnitarioVenta());
        item.put("subtotalVenta", detalle.getSubtotalVenta());
        item.put("precioUnitarioProveedor", detalle.getPrecioUnitarioProveedor());
        item.put("subtotalProveedor", detalle.getSubtotalProveedor());

        if (detalle.getServicio() != null) {
            item.put("idServicio", detalle.getServicio().getIdServicio());
            item.put("nombreServicio", detalle.getServicio().getNombreServicio());
        }

        return item;
    }

    private Map<String, Object> mapearActividad(Actividad actividad) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idActividad", actividad.getIdActividad());
        item.put("idCatalogoItemOrigen", actividad.getIdCatalogoItemOrigen());
        item.put("codigo", actividad.getCodigo());
        item.put("nombreActividad", actividad.getNombreActividad());
        item.put("descripcion", actividad.getDescripcion());
        item.put("categoria", actividad.getCategoria());
        item.put("unidad", actividad.getUnidad());
        item.put("modoPrecio", actividad.getModoPrecio());
        item.put("precioUnitarioVenta", actividad.getPrecioUnitarioVenta());
        item.put("formulaCode", actividad.getFormulaCode());
        item.put("variableBase", actividad.getVariableBase());
        item.put("factor", actividad.getFactor());
        item.put("semana", actividad.getSemana());
        item.put("orden", actividad.getOrden());
        item.put("activo", actividad.getActivo());

        if (actividad.getServicio() != null) {
            item.put("idServicio", actividad.getServicio().getIdServicio());
            item.put("nombreServicio", actividad.getServicio().getNombreServicio());
        }

        return item;
    }

    private Map<String, Object> mapearRelacionMaterial(ActividadMaterialV2 relacion) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idActividadMaterialV2", relacion.getIdActividadMaterialV2());
        item.put("semana", relacion.getSemana());
        item.put("cantidad", relacion.getCantidad());
        item.put("factor", relacion.getFactor());
        item.put("modoCantidad", relacion.getModoCantidad());
        item.put("activo", relacion.getActivo());

        if (relacion.getActividad() != null) {
            item.put("idActividad", relacion.getActividad().getIdActividad());
            item.put("nombreActividad", relacion.getActividad().getNombreActividad());
        }

        Material material = relacion.getMaterial();
        if (material != null) {
            item.put("idMaterial", material.getIdMaterial());
            item.put("nombreMaterial", material.getNombreMaterial());
            item.put("descripcionMaterial", material.getDescripcion());
            item.put("categoriaMaterial", material.getCategoria());
            item.put("unidadMaterial", material.getUnidad());
            item.put("precioUnitarioVentaMaterial", material.getPrecioUnitarioVenta());
            item.put("precioUnitarioProveedorMaterial", material.getPrecioUnitarioProveedor());
            item.put("activoMaterial", material.getActivo());
        }

        return item;
    }

    private Map<String, Object> mapearProducto(Producto producto) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idProducto", producto.getIdProducto());
        item.put("idCatalogoItemOrigen", producto.getIdCatalogoItemOrigen());
        item.put("codigo", producto.getCodigo());
        item.put("nombreProducto", producto.getNombreProducto());
        item.put("descripcion", producto.getDescripcion());
        item.put("categoria", producto.getCategoria());
        item.put("unidad", producto.getUnidad());
        item.put("modoPrecio", producto.getModoPrecio());
        item.put("precioUnitarioVenta", producto.getPrecioUnitarioVenta());
        item.put("precioUnitarioProveedor", producto.getPrecioUnitarioProveedor());
        item.put("formulaCode", producto.getFormulaCode());
        item.put("variableBase", producto.getVariableBase());
        item.put("factor", producto.getFactor());
        item.put("semana", producto.getSemana());
        item.put("activo", producto.getActivo());

        if (producto.getServicio() != null) {
            item.put("idServicio", producto.getServicio().getIdServicio());
            item.put("nombreServicio", producto.getServicio().getNombreServicio());
        }

        return item;
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
