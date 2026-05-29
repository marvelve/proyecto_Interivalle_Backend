package com.interivalle.Servicio;

import com.interivalle.DTO.ObraBlancaRequest;
import com.interivalle.DTO.ObraBlancaResponse;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.ObraBlanca;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import com.interivalle.Repositorio.ObraBlancaRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ObraBlancaService {

    @Autowired
    private ObraBlancaRepositorio obraBlancaRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionRepo;

    public ObraBlancaResponse guardar(ObraBlancaRequest req) {
        validarDatosBasicos(req);

        // La obra blanca se guarda buscando la cabecera por el id de la cotizacion base.
        CotizacionPersonalizada cotizacion = cotizacionRepo
                .findByCotizacion_IdCotizacion(req.getIdCotizacion())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion personalizada no encontrada para la cotizacion base: " + req.getIdCotizacion()
                ));

        validarCotizacionEditable(cotizacion);

        String lugarNormalizado = normalizarLugar(req.getLugar());
        validarActividadRepetida(req.getIdCotizacion(), req.getIdActividad(), lugarNormalizado);

        ObraBlanca item = new ObraBlanca();
        item.setCotizacionPersonalizada(cotizacion);
        cargarDatosItem(item, req, lugarNormalizado);

        ObraBlanca guardado = obraBlancaRepo.save(item);
        return toResponse(guardado);
    }

    public List<ObraBlancaResponse> listarPorCotizacion(Integer idCotizacion) {
        // Lista adicionales de Obra Blanca asociados a la cotizacion base.
        return obraBlancaRepo
                .findByCotizacionPersonalizada_Cotizacion_IdCotizacionOrderByIdObraBlancaAsc(idCotizacion)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ObraBlancaResponse obtenerPorId(Integer id) {
        ObraBlanca item = obraBlancaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de obra blanca no encontrado"
                ));

        return toResponse(item);
    }

    public ObraBlancaResponse actualizar(Integer id, ObraBlancaRequest req) {
        ObraBlanca item = obraBlancaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de obra blanca no encontrado"
                ));

        Integer idCotizacionBase = asignarCotizacionSiLlegaEnRequest(item, req);
        validarCotizacionEditable(item.getCotizacionPersonalizada());
        validarDatosBasicosParaActualizar(req);

        String lugarNormalizado = normalizarLugar(req.getLugar());
        validarActividadRepetidaAlActualizar(id, idCotizacionBase, req.getIdActividad(), lugarNormalizado);

        cargarDatosItem(item, req, lugarNormalizado);

        ObraBlanca actualizado = obraBlancaRepo.save(item);
        return toResponse(actualizado);
    }

    public void eliminar(Integer id) {
        ObraBlanca item = obraBlancaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de obra blanca no encontrado"
                ));

        validarCotizacionEditable(item.getCotizacionPersonalizada());
        obraBlancaRepo.delete(item);
    }

    private Integer asignarCotizacionSiLlegaEnRequest(ObraBlanca item, ObraBlancaRequest req) {
        Integer idCotizacionBase = null;

        if (req.getIdCotizacionPersonalizada() != null) {
            CotizacionPersonalizada cotizacion = cotizacionRepo.findById(req.getIdCotizacionPersonalizada())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cotizacion personalizada no encontrada"
                    ));

            item.setCotizacionPersonalizada(cotizacion);
            idCotizacionBase = cotizacion.getCotizacion().getIdCotizacion();
        } else if (req.getIdCotizacion() != null) {
            CotizacionPersonalizada cotizacion = cotizacionRepo
                    .findByCotizacion_IdCotizacion(req.getIdCotizacion())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cotizacion personalizada no encontrada para la cotizacion base: " + req.getIdCotizacion()
                    ));

            item.setCotizacionPersonalizada(cotizacion);
            idCotizacionBase = req.getIdCotizacion();
        } else if (item.getCotizacionPersonalizada() != null
                && item.getCotizacionPersonalizada().getCotizacion() != null) {
            idCotizacionBase = item.getCotizacionPersonalizada().getCotizacion().getIdCotizacion();
        }

        return idCotizacionBase;
    }

    private void validarDatosBasicos(ObraBlancaRequest req) {
        if (req.getIdCotizacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idCotizacion es obligatorio");
        }

        validarDatosBasicosParaActualizar(req);
    }

    private void validarDatosBasicosParaActualizar(ObraBlancaRequest req) {
        if (req.getIdActividad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idActividad es obligatorio");
        }

        if (req.getLugar() == null || req.getLugar().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lugar es obligatorio");
        }
    }

    private void validarActividadRepetida(Integer idCotizacion, Integer idActividad, String lugarNormalizado) {
        boolean existe = obraBlancaRepo
                .existsByCotizacionPersonalizada_Cotizacion_IdCotizacionAndIdActividadAndLugarIgnoreCase(
                        idCotizacion,
                        idActividad,
                        lugarNormalizado
                );

        if (existe) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta actividad ya esta en la cotizacion para ese mismo lugar"
            );
        }
    }

    private void validarActividadRepetidaAlActualizar(
            Integer idActual,
            Integer idCotizacionBase,
            Integer idActividad,
            String lugarNormalizado
    ) {
        ObraBlanca existente = obraBlancaRepo
                .findByCotizacionPersonalizada_Cotizacion_IdCotizacionAndIdActividadAndLugarIgnoreCase(
                        idCotizacionBase,
                        idActividad,
                        lugarNormalizado
                )
                .orElse(null);

        if (existente != null && !existente.getIdObraBlanca().equals(idActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta actividad ya esta en la cotizacion para ese mismo lugar"
            );
        }
    }

    private void cargarDatosItem(ObraBlanca item, ObraBlancaRequest req, String lugarNormalizado) {
        item.setIdActividad(req.getIdActividad());
        item.setActividad(req.getActividad());
        item.setLugar(lugarNormalizado);
        item.setUnidad(req.getUnidad());
        item.setCantidad(req.getCantidad());
        item.setSemanas(req.getSemanas());
        item.setPrecioUnitario(req.getPrecioUnitario());
        item.setMedida(req.getMedida());
        item.setDescripcion(req.getDescripcion());
        item.setSubtotal(calcularSubtotal(req));
    }

    private ObraBlancaResponse toResponse(ObraBlanca item) {
        ObraBlancaResponse resp = new ObraBlancaResponse();
        resp.setIdObraBlanca(item.getIdObraBlanca());
        resp.setIdActividad(item.getIdActividad());
        resp.setActividad(item.getActividad());
        resp.setLugar(item.getLugar());
        resp.setUnidad(item.getUnidad());
        resp.setTipoCobro(item.getUnidad());
        resp.setCantidad(item.getCantidad());
        resp.setSemanas(item.getSemanas());
        resp.setPrecioUnitario(item.getPrecioUnitario());
        resp.setMedida(item.getMedida());
        resp.setSubtotal(item.getSubtotal());
        resp.setDescripcion(item.getDescripcion());

        if (item.getCotizacionPersonalizada() != null) {
            resp.setIdCotizacionPersonalizada(
                    item.getCotizacionPersonalizada().getIdCotizacionPersonalizada()
            );

            if (item.getCotizacionPersonalizada().getCotizacion() != null) {
                resp.setIdCotizacion(
                        item.getCotizacionPersonalizada().getCotizacion().getIdCotizacion()
                );
            }
        }

        return resp;
    }

    private String normalizarLugar(String lugar) {
        return lugar == null ? "" : lugar.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private BigDecimal calcularSubtotal(ObraBlancaRequest req) {
        // El subtotal depende del tipo de dato enviado: medida o cantidad.
        if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotalBase = BigDecimal.ZERO;

        if (req.getMedida() != null && req.getMedida().compareTo(BigDecimal.ZERO) > 0) {
            subtotalBase = req.getMedida().multiply(req.getPrecioUnitario());
        } else if (req.getCantidad() != null && req.getCantidad() > 0) {
            subtotalBase = req.getPrecioUnitario().multiply(BigDecimal.valueOf(req.getCantidad()));
        }

        if (req.getSemanas() != null && req.getSemanas() > 0) {
            subtotalBase = subtotalBase.multiply(BigDecimal.valueOf(req.getSemanas()));
        }

        return subtotalBase;
    }

    private void validarCotizacionEditable(CotizacionPersonalizada cotizacion) {
        if (cotizacion == null || cotizacion.getCotizacion() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotizacion personalizada no encontrada");
        }

        EstadoCotizacion estado = cotizacion.getCotizacion().getEstado();
        if (estado == EstadoCotizacion.APROBADA || estado == EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cotizacion no se puede modificar porque esta en estado " + estado.name()
            );
        }
    }
}
