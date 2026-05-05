package com.interivalle.Servicio;

import com.interivalle.DTO.ActividadRequest;
import com.interivalle.DTO.ActividadResponse;
import com.interivalle.Modelo.Actividad;
import com.interivalle.Modelo.CatalogoItem;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.ActividadRepositorio;
import com.interivalle.Repositorio.CatalogoItemRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActividadService {

    @Autowired
    private ActividadRepositorio actividadRepo;

    @Autowired
    private CatalogoItemRepositorio catalogoItemRepo;

    @Autowired
    private ServiciosRepositorio serviciosRepo;

    public List<ActividadResponse> listar(Integer idServicio, String categoria, Boolean activo, String texto) {
        return actividadRepo.filtrar(idServicio, limpiar(categoria), activo, limpiar(texto))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ActividadResponse obtenerPorId(Integer id) {
        return toResponse(buscarActividad(id));
    }

    public ActividadResponse crear(ActividadRequest req) {
        validarCrear(req);

        Actividad actividad = new Actividad();
        aplicarDatos(actividad, req, true);

        return toResponse(actividadRepo.save(actividad));
    }

    public ActividadResponse actualizar(Integer id, ActividadRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos de la actividad");
        }

        Actividad actividad = buscarActividad(id);
        aplicarDatos(actividad, req, false);

        return toResponse(actividadRepo.save(actividad));
    }

    public ActividadResponse cambiarEstado(Integer id, Boolean activo) {
        Actividad actividad = buscarActividad(id);
        actividad.setActivo(Boolean.TRUE.equals(activo));

        return toResponse(actividadRepo.save(actividad));
    }

    public Map<String, Integer> migrarDesdeCatalogoItem() {
        List<CatalogoItem> actividadesCatalogo =
                catalogoItemRepo.findByTipoItemAndActivoTrue(TipoItemCotizacion.ACTIVIDAD);

        int creadas = 0;
        int existentes = 0;

        for (CatalogoItem item : actividadesCatalogo) {
            if (actividadRepo.findByIdCatalogoItemOrigen(item.getIdCatalogoItem()).isPresent()) {
                existentes++;
                continue;
            }

            Actividad actividad = new Actividad();
            actividad.setIdCatalogoItemOrigen(item.getIdCatalogoItem());
            actividad.setServicio(item.getServicio());
            actividad.setCodigo(limpiar(item.getCodigo()));
            actividad.setNombreActividad(item.getNombreItem());
            actividad.setDescripcion(item.getDescripcion());
            actividad.setCategoria(item.getCategoria());
            actividad.setUnidad(item.getUnidad());
            actividad.setModoPrecio(item.getModoPrecio());
            actividad.setPrecioUnitarioVenta(item.getPrecioUnitarioVenta());
            actividad.setFormulaCode(item.getFormulaCode());
            actividad.setParamsJson(item.getParamsJson());
            actividad.setFactor(item.getFactor());
            actividad.setSemana(item.getSemana());
            actividad.setOrden(item.getOrden());
            actividad.setActivo(item.getActivo() == null ? true : item.getActivo());
            actividad.setCreadoPor(item.getCreadoPor());

            actividadRepo.save(actividad);
            creadas++;
        }

        return Map.of(
                "origen", actividadesCatalogo.size(),
                "creadas", creadas,
                "existentes", existentes
        );
    }

    private void validarCrear(ActividadRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos de la actividad");
        }

        if (req.getIdServicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el servicio de la actividad");
        }

        if (limpiar(req.getNombreActividad()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la actividad es obligatorio");
        }
    }

    private Actividad buscarActividad(Integer id) {
        return actividadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Actividad no encontrada"
                ));
    }

    private void aplicarDatos(Actividad actividad, ActividadRequest req, boolean crear) {
        if (crear || req.getIdServicio() != null) {
            actividad.setServicio(buscarServicio(req.getIdServicio()));
        }

        if (crear || req.getCodigo() != null) actividad.setCodigo(limpiar(req.getCodigo()));
        if (crear || req.getNombreActividad() != null) {
            String nombre = limpiar(req.getNombreActividad());
            if (nombre == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la actividad es obligatorio");
            }
            actividad.setNombreActividad(nombre);
        }
        if (crear || req.getDescripcion() != null) actividad.setDescripcion(limpiar(req.getDescripcion()));
        if (crear || req.getCategoria() != null) actividad.setCategoria(limpiar(req.getCategoria()));
        if (crear || req.getUnidad() != null) actividad.setUnidad(limpiar(req.getUnidad()));
        if (crear || req.getModoPrecio() != null) actividad.setModoPrecio(limpiar(req.getModoPrecio()));
        if (crear || req.getPrecioUnitarioVenta() != null) actividad.setPrecioUnitarioVenta(req.getPrecioUnitarioVenta());
        if (crear || req.getFormulaCode() != null) actividad.setFormulaCode(limpiar(req.getFormulaCode()));
        if (crear || req.getParamsJson() != null) actividad.setParamsJson(limpiar(req.getParamsJson()));
        if (crear || req.getFactor() != null) actividad.setFactor(req.getFactor());
        if (crear || req.getSemana() != null) actividad.setSemana(req.getSemana());
        if (crear || req.getOrden() != null) actividad.setOrden(req.getOrden());
        if (crear || req.getActivo() != null) actividad.setActivo(req.getActivo() == null ? true : req.getActivo());
        if (crear || req.getCreadoPor() != null) actividad.setCreadoPor(req.getCreadoPor());
    }

    private Servicios buscarServicio(Integer idServicio) {
        return serviciosRepo.findById(idServicio)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));
    }

    private ActividadResponse toResponse(Actividad actividad) {
        ActividadResponse r = new ActividadResponse();
        r.setIdActividad(actividad.getIdActividad());
        r.setIdCatalogoItemOrigen(actividad.getIdCatalogoItemOrigen());
        r.setCodigo(actividad.getCodigo());
        r.setNombreActividad(actividad.getNombreActividad());
        r.setDescripcion(actividad.getDescripcion());
        r.setCategoria(actividad.getCategoria());
        r.setUnidad(actividad.getUnidad());
        r.setModoPrecio(actividad.getModoPrecio());
        r.setPrecioUnitarioVenta(actividad.getPrecioUnitarioVenta());
        r.setFormulaCode(actividad.getFormulaCode());
        r.setParamsJson(actividad.getParamsJson());
        r.setFactor(actividad.getFactor());
        r.setSemana(actividad.getSemana());
        r.setOrden(actividad.getOrden());
        r.setActivo(actividad.getActivo());
        r.setCreadoPor(actividad.getCreadoPor());
        r.setFechaCreacion(actividad.getFechaCreacion());
        r.setFechaActualizacion(actividad.getFechaActualizacion());

        if (actividad.getServicio() != null) {
            r.setIdServicio(actividad.getServicio().getIdServicio());
            r.setNombreServicio(actividad.getServicio().getNombreServicio());
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
