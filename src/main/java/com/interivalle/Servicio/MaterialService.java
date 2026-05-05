package com.interivalle.Servicio;

import com.interivalle.DTO.MaterialRequest;
import com.interivalle.DTO.MaterialResponse;
import com.interivalle.Modelo.CatalogoItem;
import com.interivalle.Modelo.Material;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.CatalogoItemRepositorio;
import com.interivalle.Repositorio.MaterialRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepositorio materialRepo;

    @Autowired
    private CatalogoItemRepositorio catalogoItemRepo;

    @Autowired
    private ServiciosRepositorio serviciosRepo;

    public List<MaterialResponse> listar(Integer idServicio, String categoria, Boolean activo, String texto) {
        return materialRepo.filtrar(idServicio, limpiar(categoria), activo, limpiar(texto))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MaterialResponse obtenerPorId(Integer id) {
        return toResponse(buscarMaterial(id));
    }

    public MaterialResponse crear(MaterialRequest req) {
        validarCrear(req);

        Material material = new Material();
        aplicarDatos(material, req, true);

        return toResponse(materialRepo.save(material));
    }

    public MaterialResponse actualizar(Integer id, MaterialRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos del material");
        }

        Material material = buscarMaterial(id);
        aplicarDatos(material, req, false);

        return toResponse(materialRepo.save(material));
    }

    public MaterialResponse cambiarEstado(Integer id, Boolean activo) {
        Material material = buscarMaterial(id);
        material.setActivo(Boolean.TRUE.equals(activo));

        return toResponse(materialRepo.save(material));
    }

    public Map<String, Integer> migrarDesdeCatalogoItem() {
        List<CatalogoItem> materialesCatalogo =
                catalogoItemRepo.findByTipoItemAndActivoTrue(TipoItemCotizacion.MATERIAL);

        int creados = 0;
        int existentes = 0;

        for (CatalogoItem item : materialesCatalogo) {
            String nombre = limpiar(item.getNombreItem());

            if (materialRepo.findByIdCatalogoItemOrigen(item.getIdCatalogoItem()).isPresent()) {
                existentes++;
                continue;
            }

            Optional<Material> existentePorNombre =
                    nombre == null ? Optional.empty() : materialRepo.findFirstByNombreMaterialIgnoreCase(nombre);

            if (existentePorNombre.isPresent()) {
                existentes++;
                continue;
            }

            Material material = new Material();
            material.setIdCatalogoItemOrigen(item.getIdCatalogoItem());
            material.setServicio(item.getServicio());
            material.setCodigo(limpiar(item.getCodigo()));
            material.setNombreMaterial(nombre);
            material.setDescripcion(item.getDescripcion());
            material.setCategoria(item.getCategoria());
            material.setUnidad(item.getUnidad());
            material.setModoPrecio(item.getModoPrecio());
            material.setPrecioUnitarioVenta(item.getPrecioUnitarioVenta());
            material.setPrecioUnitarioProveedor(item.getPrecioUnitarioProveedor());
            material.setActivo(item.getActivo() == null ? true : item.getActivo());
            material.setCreadoPor(item.getCreadoPor());

            materialRepo.save(material);
            creados++;
        }

        return Map.of(
                "origen", materialesCatalogo.size(),
                "creados", creados,
                "existentes", existentes
        );
    }

    private void validarCrear(MaterialRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos del material");
        }

        if (req.getIdServicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el servicio del material");
        }

        if (limpiar(req.getNombreMaterial()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del material es obligatorio");
        }
    }

    private Material buscarMaterial(Integer id) {
        return materialRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Material no encontrado"
                ));
    }

    private void aplicarDatos(Material material, MaterialRequest req, boolean crear) {
        if (crear || req.getIdServicio() != null) {
            material.setServicio(buscarServicio(req.getIdServicio()));
        }

        if (crear || req.getCodigo() != null) material.setCodigo(limpiar(req.getCodigo()));
        if (crear || req.getNombreMaterial() != null) {
            String nombre = limpiar(req.getNombreMaterial());
            if (nombre == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del material es obligatorio");
            }
            material.setNombreMaterial(nombre);
        }
        if (crear || req.getDescripcion() != null) material.setDescripcion(limpiar(req.getDescripcion()));
        if (crear || req.getCategoria() != null) material.setCategoria(limpiar(req.getCategoria()));
        if (crear || req.getUnidad() != null) material.setUnidad(limpiar(req.getUnidad()));
        if (crear || req.getModoPrecio() != null) material.setModoPrecio(limpiar(req.getModoPrecio()));
        if (crear || req.getPrecioUnitarioVenta() != null) material.setPrecioUnitarioVenta(req.getPrecioUnitarioVenta());
        if (crear || req.getPrecioUnitarioProveedor() != null) material.setPrecioUnitarioProveedor(req.getPrecioUnitarioProveedor());
        if (crear || req.getActivo() != null) material.setActivo(req.getActivo() == null ? true : req.getActivo());
        if (crear || req.getCreadoPor() != null) material.setCreadoPor(req.getCreadoPor());
    }

    private Servicios buscarServicio(Integer idServicio) {
        return serviciosRepo.findById(idServicio)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));
    }

    private MaterialResponse toResponse(Material material) {
        MaterialResponse r = new MaterialResponse();
        r.setIdMaterial(material.getIdMaterial());
        r.setIdCatalogoItemOrigen(material.getIdCatalogoItemOrigen());
        r.setCodigo(material.getCodigo());
        r.setNombreMaterial(material.getNombreMaterial());
        r.setDescripcion(material.getDescripcion());
        r.setCategoria(material.getCategoria());
        r.setUnidad(material.getUnidad());
        r.setModoPrecio(material.getModoPrecio());
        r.setPrecioUnitarioVenta(material.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(material.getPrecioUnitarioProveedor());
        r.setActivo(material.getActivo());
        r.setCreadoPor(material.getCreadoPor());
        r.setFechaCreacion(material.getFechaCreacion());
        r.setFechaActualizacion(material.getFechaActualizacion());

        if (material.getServicio() != null) {
            r.setIdServicio(material.getServicio().getIdServicio());
            r.setNombreServicio(material.getServicio().getNombreServicio());
        }

        return r;
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
