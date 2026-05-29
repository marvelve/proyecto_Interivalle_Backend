package com.interivalle.Servicio;

import com.interivalle.DTO.ActualizarPrecioCatalogoRequest;
import com.interivalle.DTO.CatalogoItemResponse;
import com.interivalle.DTO.CrearCatalogoItemRequest;
import com.interivalle.Modelo.Actividad;
import com.interivalle.Modelo.ActividadMaterialV2;
import com.interivalle.Modelo.Material;
import com.interivalle.Modelo.Producto;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.ActividadMaterialV2Repositorio;
import com.interivalle.Repositorio.ActividadRepositorio;
import com.interivalle.Repositorio.MaterialRepositorio;
import com.interivalle.Repositorio.ProductoRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogoItemService {

    private static final int OFFSET_ACTIVIDAD = 1_000_000;
    private static final int OFFSET_MATERIAL = 2_000_000;
    private static final int OFFSET_PRODUCTO = 3_000_000;

    @Autowired
    private ActividadRepositorio actividadRepo;

    @Autowired
    private MaterialRepositorio materialRepo;

    @Autowired
    private ProductoRepositorio productoRepo;

    @Autowired
    private ActividadMaterialV2Repositorio actividadMaterialV2Repo;

    @Autowired
    private ServiciosRepositorio serviciosRepo;

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> listar() {
        List<ActividadMaterialV2> relaciones = actividadMaterialV2Repo.findAll();
        Map<Integer, Long> materialesPorActividad = contarRelaciones(
                relaciones,
                relacion -> relacion.getActividad() != null ? relacion.getActividad().getIdActividad() : null
        );
        Map<Integer, Long> actividadesPorMaterial = contarRelaciones(
                relaciones,
                relacion -> relacion.getMaterial() != null ? relacion.getMaterial().getIdMaterial() : null
        );

        List<CatalogoItemResponse> respuesta = new ArrayList<>();

        actividadRepo.findAll().forEach(actividad ->
                respuesta.add(toResponseActividad(
                        actividad,
                        materialesPorActividad.getOrDefault(actividad.getIdActividad(), 0L).intValue()
                ))
        );

        materialRepo.findAll().forEach(material ->
                respuesta.add(toResponseMaterial(
                        material,
                        actividadesPorMaterial.getOrDefault(material.getIdMaterial(), 0L).intValue()
                ))
        );

        productoRepo.findAll().forEach(producto ->
                respuesta.add(toResponseProducto(producto))
        );

        return respuesta;
    }

    @Transactional(readOnly = true)
    public CatalogoItemResponse obtenerPorId(Integer id) {
        TipoItemCotizacion tipo = obtenerTipoDesdeId(id);
        Integer idReal = obtenerIdReal(id, tipo);

        if (tipo == TipoItemCotizacion.ACTIVIDAD) {
            Actividad actividad = actividadRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));
            int relaciones = actividadMaterialV2Repo
                    .findByActividad_IdActividadAndActivoTrue(actividad.getIdActividad())
                    .size();
            return toResponseActividad(actividad, relaciones);
        }

        if (tipo == TipoItemCotizacion.MATERIAL) {
            Material material = materialRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Material no encontrado"));
            int relaciones = contarRelacionesMaterial(material.getIdMaterial());
            return toResponseMaterial(material, relaciones);
        }

        Producto producto = productoRepo.findById(idReal)
                .orElseThrow(() -> noEncontrado("Producto no encontrado"));
        return toResponseProducto(producto);
    }

    @Transactional
    public CatalogoItemResponse actualizarPrecio(Integer id, ActualizarPrecioCatalogoRequest dto) {
        TipoItemCotizacion tipo = obtenerTipoDesdeId(id);
        Integer idReal = obtenerIdReal(id, tipo);

        if (tipo == TipoItemCotizacion.ACTIVIDAD) {
            Actividad actividad = actividadRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));

            if (dto.getPrecioUnitarioVenta() != null) {
                actividad.setPrecioUnitarioVenta(dto.getPrecioUnitarioVenta());
            }

            if (dto.getActivo() != null) {
                actividad.setActivo(dto.getActivo());
            }

            Actividad actualizada = actividadRepo.save(actividad);
            int relaciones = actividadMaterialV2Repo
                    .findByActividad_IdActividadAndActivoTrue(actualizada.getIdActividad())
                    .size();
            return toResponseActividad(actualizada, relaciones);
        }

        if (tipo == TipoItemCotizacion.MATERIAL) {
            Material material = materialRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Material no encontrado"));

            aplicarPrecioMaterial(material, dto);
            Material actualizado = materialRepo.save(material);
            return toResponseMaterial(actualizado, contarRelacionesMaterial(actualizado.getIdMaterial()));
        }

        Producto producto = productoRepo.findById(idReal)
                .orElseThrow(() -> noEncontrado("Producto no encontrado"));

        aplicarPrecioProducto(producto, dto);
        return toResponseProducto(productoRepo.save(producto));
    }

    @Transactional
    public CatalogoItemResponse crear(CrearCatalogoItemRequest dto) {
        TipoItemCotizacion tipo = validarTipo(dto.getTipoItem());
        Servicios servicio = serviciosRepo.findById(dto.getIdServicio())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));

        if (tipo == TipoItemCotizacion.ACTIVIDAD) {
            Actividad actividad = new Actividad();
            actividad.setServicio(servicio);
            actividad.setCodigo(generarCodigo("ACT"));
            actividad.setNombreActividad(dto.getNombreItem());
            actividad.setCategoria(dto.getCategoria());
            actividad.setModoPrecio("FIJO");
            actividad.setPrecioUnitarioVenta(valorSeguro(dto.getPrecioUnitarioVenta()));
            actividad.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
            actividad.setFechaCreacion(LocalDateTime.now());
            actividad.setFechaActualizacion(LocalDateTime.now());

            return toResponseActividad(actividadRepo.save(actividad), 0);
        }

        if (tipo == TipoItemCotizacion.MATERIAL) {
            Material material = new Material();
            material.setServicio(servicio);
            material.setCodigo(generarCodigo("MAT"));
            material.setNombreMaterial(dto.getNombreItem());
            material.setCategoria(dto.getCategoria());
            material.setModoPrecio("FIJO");
            material.setPrecioUnitarioVenta(valorSeguro(dto.getPrecioUnitarioVenta()));
            material.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
            material.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
            material.setFechaCreacion(LocalDateTime.now());
            material.setFechaActualizacion(LocalDateTime.now());

            return toResponseMaterial(materialRepo.save(material), 0);
        }

        Producto producto = new Producto();
        producto.setServicio(servicio);
        producto.setCodigo(generarCodigo("PROD"));
        producto.setNombreProducto(dto.getNombreItem());
        producto.setCategoria(dto.getCategoria());
        producto.setModoPrecio("FIJO");
        producto.setPrecioUnitarioVenta(valorSeguro(dto.getPrecioUnitarioVenta()));
        producto.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
        producto.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setFechaActualizacion(LocalDateTime.now());

        return toResponseProducto(productoRepo.save(producto));
    }

    private void aplicarPrecioMaterial(Material material, ActualizarPrecioCatalogoRequest dto) {
        if (dto.getPrecioUnitarioVenta() != null) {
            material.setPrecioUnitarioVenta(dto.getPrecioUnitarioVenta());
        }

        if (dto.getPrecioUnitarioProveedor() != null) {
            material.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
        }

        if (dto.getActivo() != null) {
            material.setActivo(dto.getActivo());
        }
    }

    private void aplicarPrecioProducto(Producto producto, ActualizarPrecioCatalogoRequest dto) {
        if (dto.getPrecioUnitarioVenta() != null) {
            producto.setPrecioUnitarioVenta(dto.getPrecioUnitarioVenta());
        }

        if (dto.getPrecioUnitarioProveedor() != null) {
            producto.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
        }

        if (dto.getActivo() != null) {
            producto.setActivo(dto.getActivo());
        }
    }

    private CatalogoItemResponse toResponseActividad(Actividad actividad, Integer relaciones) {
        CatalogoItemResponse r = new CatalogoItemResponse();
        r.setIdCatalogoItem(OFFSET_ACTIVIDAD + actividad.getIdActividad());
        r.setIdItemOrigen(actividad.getIdActividad());
        r.setTipoItem(TipoItemCotizacion.ACTIVIDAD.name());
        r.setTablaOrigen("actividad");
        r.setNombreItem(actividad.getNombreActividad());
        r.setCategoria(actividad.getCategoria());
        r.setPrecioUnitarioVenta(actividad.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(null);
        r.setActivo(actividad.getActivo());
        r.setRelacionesV2(relaciones);
        cargarServicio(r, actividad.getServicio());
        return r;
    }

    private CatalogoItemResponse toResponseMaterial(Material material, Integer relaciones) {
        CatalogoItemResponse r = new CatalogoItemResponse();
        r.setIdCatalogoItem(OFFSET_MATERIAL + material.getIdMaterial());
        r.setIdItemOrigen(material.getIdMaterial());
        r.setTipoItem(TipoItemCotizacion.MATERIAL.name());
        r.setTablaOrigen("material");
        r.setNombreItem(material.getNombreMaterial());
        r.setCategoria(material.getCategoria());
        r.setPrecioUnitarioVenta(material.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(material.getPrecioUnitarioProveedor());
        r.setActivo(material.getActivo());
        r.setRelacionesV2(relaciones);
        cargarServicio(r, material.getServicio());
        return r;
    }

    private CatalogoItemResponse toResponseProducto(Producto producto) {
        CatalogoItemResponse r = new CatalogoItemResponse();
        r.setIdCatalogoItem(OFFSET_PRODUCTO + producto.getIdProducto());
        r.setIdItemOrigen(producto.getIdProducto());
        r.setTipoItem(TipoItemCotizacion.PRODUCTO.name());
        r.setTablaOrigen("producto");
        r.setNombreItem(producto.getNombreProducto());
        r.setCategoria(producto.getCategoria());
        r.setPrecioUnitarioVenta(producto.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(producto.getPrecioUnitarioProveedor());
        r.setActivo(producto.getActivo());
        r.setRelacionesV2(0);
        cargarServicio(r, producto.getServicio());
        return r;
    }

    private void cargarServicio(CatalogoItemResponse response, Servicios servicio) {
        if (servicio == null) {
            return;
        }

        response.setIdServicio(servicio.getIdServicio());
        response.setNombreServicio(servicio.getNombreServicio());
    }

    private TipoItemCotizacion obtenerTipoDesdeId(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el id del item");
        }

        if (id >= OFFSET_PRODUCTO) {
            return TipoItemCotizacion.PRODUCTO;
        }

        if (id >= OFFSET_MATERIAL) {
            return TipoItemCotizacion.MATERIAL;
        }

        if (id >= OFFSET_ACTIVIDAD) {
            return TipoItemCotizacion.ACTIVIDAD;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El id no pertenece al catalogo V2. Vuelva a abrir el modulo de precios."
        );
    }

    private Integer obtenerIdReal(Integer id, TipoItemCotizacion tipo) {
        if (tipo == TipoItemCotizacion.ACTIVIDAD) {
            return id - OFFSET_ACTIVIDAD;
        }

        if (tipo == TipoItemCotizacion.MATERIAL) {
            return id - OFFSET_MATERIAL;
        }

        return id - OFFSET_PRODUCTO;
    }

    private TipoItemCotizacion validarTipo(String tipoItem) {
        try {
            return TipoItemCotizacion.valueOf(tipoItem);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de item invalido");
        }
    }

    private Map<Integer, Long> contarRelaciones(
            List<ActividadMaterialV2> relaciones,
            Function<ActividadMaterialV2, Integer> obtenerId
    ) {
        return relaciones.stream()
                .filter(relacion -> Boolean.TRUE.equals(relacion.getActivo()))
                .map(obtenerId)
                .filter(id -> id != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private int contarRelacionesMaterial(Integer idMaterial) {
        return (int) actividadMaterialV2Repo.findAll()
                .stream()
                .filter(relacion -> Boolean.TRUE.equals(relacion.getActivo()))
                .filter(relacion -> relacion.getMaterial() != null)
                .filter(relacion -> idMaterial.equals(relacion.getMaterial().getIdMaterial()))
                .count();
    }

    private String generarCodigo(String prefijo) {
        return prefijo + "_" + System.currentTimeMillis();
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
