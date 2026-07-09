package com.interivalle.Servicio;

import com.interivalle.DTO.ActualizarPrecioCatalogoRequest;
import com.interivalle.DTO.ActividadMaterialV2Request;
import com.interivalle.DTO.ActividadMaterialV2Response;
import com.interivalle.DTO.CatalogoItemResponse;
import com.interivalle.DTO.CrearCatalogoItemRequest;
import com.interivalle.Modelo.Actividad;
import com.interivalle.Modelo.ActividadPersonalizada;
import com.interivalle.Modelo.ActividadMaterialV2;
import com.interivalle.Modelo.Material;
import com.interivalle.Modelo.Producto;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.ActividadMaterialV2Repositorio;
import com.interivalle.Repositorio.ActividadPersonalizadaRepositorio;
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
    private static final int OFFSET_ACTIVIDAD_ADICIONAL = 4_000_000;
    private static final String TIPO_ACTIVIDAD_ADICIONAL = "ACTIVIDAD ADICIONAL";

    @Autowired
    private ActividadRepositorio actividadRepo;

    @Autowired
    private MaterialRepositorio materialRepo;

    @Autowired
    private ProductoRepositorio productoRepo;

    @Autowired
    private ActividadMaterialV2Repositorio actividadMaterialV2Repo;

    @Autowired
    private ActividadPersonalizadaRepositorio actividadPersonalizadaRepo;

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

        actividadPersonalizadaRepo.findAll().forEach(actividad ->
                respuesta.add(toResponseActividadAdicional(actividad))
        );

        return respuesta;
    }

    @Transactional(readOnly = true)
    public CatalogoItemResponse obtenerPorId(Integer id) {
        String tipo = obtenerTipoDesdeId(id);
        Integer idReal = obtenerIdReal(id, tipo);

        if (TipoItemCotizacion.ACTIVIDAD.name().equals(tipo)) {
            Actividad actividad = actividadRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));
            int relaciones = actividadMaterialV2Repo
                    .findByActividad_IdActividadAndActivoTrue(actividad.getIdActividad())
                    .size();
            return toResponseActividad(actividad, relaciones);
        }

        if (TipoItemCotizacion.MATERIAL.name().equals(tipo)) {
            Material material = materialRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Material no encontrado"));
            int relaciones = contarRelacionesMaterial(material.getIdMaterial());
            return toResponseMaterial(material, relaciones);
        }

        if (TIPO_ACTIVIDAD_ADICIONAL.equals(tipo)) {
            ActividadPersonalizada actividad = actividadPersonalizadaRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad adicional no encontrada"));
            return toResponseActividadAdicional(actividad);
        }

        Producto producto = productoRepo.findById(idReal)
                .orElseThrow(() -> noEncontrado("Producto no encontrado"));
        return toResponseProducto(producto);
    }

    @Transactional
    public CatalogoItemResponse actualizarPrecio(Integer id, ActualizarPrecioCatalogoRequest dto) {
        String tipo = obtenerTipoDesdeId(id);
        Integer idReal = obtenerIdReal(id, tipo);

        if (TipoItemCotizacion.ACTIVIDAD.name().equals(tipo)) {
            Actividad actividad = actividadRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));

            if (dto.getNombreItem() != null) {
                actividad.setNombreActividad(dto.getNombreItem());
            }

            if (dto.getCategoria() != null) {
                actividad.setCategoria(dto.getCategoria());
            }

            if (dto.getIdServicio() != null) {
                actividad.setServicio(buscarServicio(dto.getIdServicio()));
            }

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

        if (TipoItemCotizacion.MATERIAL.name().equals(tipo)) {
            Material material = materialRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Material no encontrado"));

            if (dto.getNombreItem() != null) {
                material.setNombreMaterial(dto.getNombreItem());
            }

            if (dto.getCategoria() != null) {
                material.setCategoria(dto.getCategoria());
            }

            if (dto.getIdServicio() != null) {
                material.setServicio(buscarServicio(dto.getIdServicio()));
            }

            aplicarPrecioMaterial(material, dto);
            Material actualizado = materialRepo.save(material);
            return toResponseMaterial(actualizado, contarRelacionesMaterial(actualizado.getIdMaterial()));
        }

        if (TIPO_ACTIVIDAD_ADICIONAL.equals(tipo)) {
            ActividadPersonalizada actividad = actividadPersonalizadaRepo.findById(idReal)
                    .orElseThrow(() -> noEncontrado("Actividad adicional no encontrada"));

            if (dto.getNombreItem() != null) {
                actividad.setNombreActividad(dto.getNombreItem());
            }

            if (dto.getCategoria() != null) {
                actividad.setTipoCobro(dto.getCategoria());
            }

            if (dto.getIdServicio() != null) {
                actividad.setServicios(buscarServicio(dto.getIdServicio()));
            }

            if (dto.getPrecioUnitarioVenta() != null) {
                actividad.setPrecioUnitario(dto.getPrecioUnitarioVenta());
            }

            if (dto.getActivo() != null) {
                actividad.setEstado(dto.getActivo());
            }

            return toResponseActividadAdicional(actividadPersonalizadaRepo.save(actividad));
        }

        Producto producto = productoRepo.findById(idReal)
                .orElseThrow(() -> noEncontrado("Producto no encontrado"));

        if (dto.getNombreItem() != null) {
            producto.setNombreProducto(dto.getNombreItem());
        }

        if (dto.getCategoria() != null) {
            producto.setCategoria(dto.getCategoria());
        }

        if (dto.getIdServicio() != null) {
            producto.setServicio(buscarServicio(dto.getIdServicio()));
        }

        aplicarPrecioProducto(producto, dto);
        return toResponseProducto(productoRepo.save(producto));
    }

    @Transactional
    public CatalogoItemResponse crear(CrearCatalogoItemRequest dto) {
        String tipoItem = normalizarTipo(dto.getTipoItem());
        Servicios servicio = serviciosRepo.findById(dto.getIdServicio())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));

        if (TipoItemCotizacion.ACTIVIDAD.name().equals(tipoItem)) {
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

        if (TipoItemCotizacion.MATERIAL.name().equals(tipoItem)) {
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

            Material creado = materialRepo.save(material);
            crearRelacionesInicialesMaterial(creado, dto.getRelacionesActividad());
            return toResponseMaterial(creado, contarRelacionesMaterial(creado.getIdMaterial()));
        }

        if (TIPO_ACTIVIDAD_ADICIONAL.equals(tipoItem)) {
            ActividadPersonalizada actividad = new ActividadPersonalizada();
            actividad.setServicios(servicio);
            actividad.setNombreActividad(dto.getNombreItem());
            actividad.setTipoCobro(dto.getCategoria());
            actividad.setPrecioUnitario(valorSeguro(dto.getPrecioUnitarioVenta()));
            actividad.setEstado(dto.getActivo() != null ? dto.getActivo() : true);

            return toResponseActividadAdicional(actividadPersonalizadaRepo.save(actividad));
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

    @Transactional(readOnly = true)
    public List<ActividadMaterialV2Response> listarRelacionesMaterial(Integer idMaterial) {
        validarMaterialExiste(idMaterial);
        return actividadMaterialV2Repo
                .findByMaterial_IdMaterialOrderByActividad_Servicio_IdServiciosAscActividad_NombreActividadAsc(idMaterial)
                .stream()
                .map(this::toResponseRelacion)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActividadMaterialV2Response> listarRelacionesActividad(Integer idActividad) {
        validarActividadExiste(idActividad);
        return actividadMaterialV2Repo
                .findByActividad_IdActividadOrderByMaterial_NombreMaterialAsc(idActividad)
                .stream()
                .map(this::toResponseRelacion)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActividadMaterialV2Response crearRelacionMaterial(Integer idMaterial, ActividadMaterialV2Request dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos de la relacion");
        }

        Material material = materialRepo.findById(idMaterial)
                .orElseThrow(() -> noEncontrado("Material no encontrado"));
        Actividad actividad = actividadRepo.findById(dto.getIdActividad())
                .orElseThrow(() -> noEncontrado("Actividad no encontrada"));

        ActividadMaterialV2 relacion = new ActividadMaterialV2();
        relacion.setMaterial(material);
        relacion.setActividad(actividad);
        aplicarDatosRelacion(relacion, dto);

        return toResponseRelacion(actividadMaterialV2Repo.save(relacion));
    }

    @Transactional
    public ActividadMaterialV2Response actualizarRelacionMaterial(
            Integer idMaterial,
            Integer idRelacion,
            ActividadMaterialV2Request dto
    ) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos de la relacion");
        }

        ActividadMaterialV2 relacion = actividadMaterialV2Repo.findById(idRelacion)
                .orElseThrow(() -> noEncontrado("Relacion no encontrada"));

        if (relacion.getMaterial() == null
                || !idMaterial.equals(relacion.getMaterial().getIdMaterial())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La relacion no pertenece al material indicado");
        }

        if (dto.getIdActividad() != null
                && (relacion.getActividad() == null
                || !dto.getIdActividad().equals(relacion.getActividad().getIdActividad()))) {
            Actividad actividad = actividadRepo.findById(dto.getIdActividad())
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));
            relacion.setActividad(actividad);
        }

        aplicarDatosRelacion(relacion, dto);
        return toResponseRelacion(actividadMaterialV2Repo.save(relacion));
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

    private void aplicarDatosRelacion(ActividadMaterialV2 relacion, ActividadMaterialV2Request dto) {
        if (dto.getCantidad() != null) {
            relacion.setCantidad(dto.getCantidad());
        } else if (relacion.getCantidad() == null) {
            relacion.setCantidad(BigDecimal.ONE);
        }

        if (dto.getFactor() != null) {
            relacion.setFactor(dto.getFactor());
        }

        if (dto.getModoCantidad() != null) {
            relacion.setModoCantidad(dto.getModoCantidad());
        } else if (relacion.getModoCantidad() == null) {
            relacion.setModoCantidad("POR_ACTIVIDAD");
        }

        if (dto.getActivo() != null) {
            relacion.setActivo(dto.getActivo());
        } else if (relacion.getActivo() == null) {
            relacion.setActivo(true);
        }

        if (dto.getUnidadMaterial() != null && relacion.getMaterial() != null) {
            relacion.getMaterial().setUnidad(dto.getUnidadMaterial());
            materialRepo.save(relacion.getMaterial());
        }

        // Compatibilidad con esquemas existentes: la cotizacion usa siempre la semana de la actividad.
        if (relacion.getActividad() != null) {
            relacion.setSemana(relacion.getActividad().getSemana());
        }
    }

    private void crearRelacionesInicialesMaterial(
            Material material,
            List<ActividadMaterialV2Request> relaciones
    ) {
        if (material == null || relaciones == null || relaciones.isEmpty()) {
            return;
        }

        for (ActividadMaterialV2Request dtoRelacion : relaciones) {
            if (dtoRelacion == null || dtoRelacion.getIdActividad() == null) {
                continue;
            }

            Actividad actividad = actividadRepo.findById(dtoRelacion.getIdActividad())
                    .orElseThrow(() -> noEncontrado("Actividad no encontrada"));

            ActividadMaterialV2 relacion = new ActividadMaterialV2();
            relacion.setMaterial(material);
            relacion.setActividad(actividad);
            aplicarDatosRelacion(relacion, dtoRelacion);
            actividadMaterialV2Repo.save(relacion);
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

    private ActividadMaterialV2Response toResponseRelacion(ActividadMaterialV2 relacion) {
        ActividadMaterialV2Response r = new ActividadMaterialV2Response();
        r.setIdActividadMaterialV2(relacion.getIdActividadMaterialV2());
        r.setCantidad(relacion.getCantidad());
        r.setFactor(relacion.getFactor());
        r.setModoCantidad(relacion.getModoCantidad());
        r.setActivo(relacion.getActivo());

        if (relacion.getActividad() != null) {
            Actividad actividad = relacion.getActividad();
            r.setIdActividad(actividad.getIdActividad());
            r.setNombreActividad(actividad.getNombreActividad());

            if (actividad.getServicio() != null) {
                r.setIdServicio(actividad.getServicio().getIdServicio());
                r.setNombreServicio(actividad.getServicio().getNombreServicio());
            }
        }

        if (relacion.getMaterial() != null) {
            Material material = relacion.getMaterial();
            r.setIdMaterial(material.getIdMaterial());
            r.setNombreMaterial(material.getNombreMaterial());
            r.setUnidadMaterial(material.getUnidad());
        }

        return r;
    }

    private CatalogoItemResponse toResponseActividadAdicional(ActividadPersonalizada actividad) {
        CatalogoItemResponse r = new CatalogoItemResponse();
        r.setIdCatalogoItem(OFFSET_ACTIVIDAD_ADICIONAL + actividad.getIdActividad());
        r.setIdItemOrigen(actividad.getIdActividad());
        r.setTipoItem(TIPO_ACTIVIDAD_ADICIONAL);
        r.setTablaOrigen("actividad_personalizada");
        r.setNombreItem(actividad.getNombreActividad());
        r.setCategoria(actividad.getTipoCobro());
        r.setPrecioUnitarioVenta(actividad.getPrecioUnitario());
        r.setPrecioUnitarioProveedor(null);
        r.setActivo(actividad.getEstado());
        r.setRelacionesV2(0);
        cargarServicio(r, actividad.getServicios());
        return r;
    }

    private void cargarServicio(CatalogoItemResponse response, Servicios servicio) {
        if (servicio == null) {
            return;
        }

        response.setIdServicio(servicio.getIdServicio());
        response.setNombreServicio(servicio.getNombreServicio());
    }

    private String obtenerTipoDesdeId(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el id del item");
        }

        if (id >= OFFSET_ACTIVIDAD_ADICIONAL) {
            return TIPO_ACTIVIDAD_ADICIONAL;
        }

        if (id >= OFFSET_PRODUCTO) {
            return TipoItemCotizacion.PRODUCTO.name();
        }

        if (id >= OFFSET_MATERIAL) {
            return TipoItemCotizacion.MATERIAL.name();
        }

        if (id >= OFFSET_ACTIVIDAD) {
            return TipoItemCotizacion.ACTIVIDAD.name();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El id no pertenece al catalogo V2. Vuelva a abrir el modulo de precios."
        );
    }

    private Integer obtenerIdReal(Integer id, String tipo) {
        if (TIPO_ACTIVIDAD_ADICIONAL.equals(tipo)) {
            return id - OFFSET_ACTIVIDAD_ADICIONAL;
        }

        if (TipoItemCotizacion.ACTIVIDAD.name().equals(tipo)) {
            return id - OFFSET_ACTIVIDAD;
        }

        if (TipoItemCotizacion.MATERIAL.name().equals(tipo)) {
            return id - OFFSET_MATERIAL;
        }

        return id - OFFSET_PRODUCTO;
    }

    private String normalizarTipo(String tipoItem) {
        if (tipoItem != null && TIPO_ACTIVIDAD_ADICIONAL.equals(tipoItem.trim().toUpperCase())) {
            return TIPO_ACTIVIDAD_ADICIONAL;
        }

        try {
            return TipoItemCotizacion.valueOf(tipoItem).name();
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

    private Servicios buscarServicio(Integer idServicio) {
        return serviciosRepo.findById(idServicio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
    }

    private void validarMaterialExiste(Integer idMaterial) {
        if (idMaterial == null || !materialRepo.existsById(idMaterial)) {
            throw noEncontrado("Material no encontrado");
        }
    }

    private void validarActividadExiste(Integer idActividad) {
        if (idActividad == null || !actividadRepo.existsById(idActividad)) {
            throw noEncontrado("Actividad no encontrada");
        }
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
