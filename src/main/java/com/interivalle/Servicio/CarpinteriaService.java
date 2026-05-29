package com.interivalle.Servicio;

import com.interivalle.DTO.CarpinteriaRequest;
import com.interivalle.Modelo.Carpinteria;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Repositorio.CarpinteriaRepositorio;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CarpinteriaService {

    @Autowired
    private CarpinteriaRepositorio carpinteriaRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionRepo;

    public Carpinteria guardar(CarpinteriaRequest req) {
        CotizacionPersonalizada cotizacion = buscarCotizacionPersonalizada(req.getIdCotizacion());
        validarCotizacionEditable(cotizacion);

        Carpinteria item = new Carpinteria();
        item.setCotizacionPersonalizada(cotizacion);
        cargarDatosItem(item, req);

        return carpinteriaRepo.save(item);
    }

    public List<Carpinteria> listarPorCotizacion(Integer idCotizacion) {
        return carpinteriaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);
    }

    public Carpinteria obtenerPorId(Integer id) {
        return carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de carpinteria no encontrado"
                ));
    }

    public Carpinteria actualizar(Integer id, CarpinteriaRequest req) {
        Carpinteria item = carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de carpinteria no encontrado"
                ));

        if (req.getIdCotizacion() != null) {
            CotizacionPersonalizada cotizacion = buscarCotizacionPersonalizada(req.getIdCotizacion());
            item.setCotizacionPersonalizada(cotizacion);
        }

        validarCotizacionEditable(item.getCotizacionPersonalizada());
        cargarDatosItem(item, req);

        return carpinteriaRepo.save(item);
    }

    public void eliminar(Integer id) {
        Carpinteria item = carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de carpinteria no encontrado"
                ));

        validarCotizacionEditable(item.getCotizacionPersonalizada());
        carpinteriaRepo.delete(item);
    }

    private CotizacionPersonalizada buscarCotizacionPersonalizada(Integer idCotizacionPersonalizada) {
        // Carpinteria recibe el id de cotizacion personalizada, no el id de cotizacion base.
        return cotizacionRepo.findById(idCotizacionPersonalizada)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));
    }

    private void cargarDatosItem(Carpinteria item, CarpinteriaRequest req) {
        item.setTipoMueble(req.getTipoMueble());
        item.setMaterial(req.getMaterial());
        item.setLargo(valorDecimal(req.getLargo()));
        item.setAncho(valorDecimal(req.getAncho()));
        item.setAlto(valorDecimal(req.getAlto()));
        item.setCantidad(valorEntero(req.getCantidad()));
        item.setPrecioUnitario(valorDecimal(req.getPrecioUnitario()));
        item.setDescripcion(req.getDescripcion());
        item.setSubtotal(calcularSubtotal(req));
    }

    private BigDecimal calcularSubtotal(CarpinteriaRequest req) {
        // Calcula por area si llegan largo y ancho; si no, toma el precio unitario como base.
        if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotalBase;

        if (req.getLargo() != null && req.getLargo().compareTo(BigDecimal.ZERO) > 0
                && req.getAncho() != null && req.getAncho().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal area = req.getLargo().multiply(req.getAncho());
            subtotalBase = area.multiply(req.getPrecioUnitario());
        } else {
            subtotalBase = req.getPrecioUnitario();
        }

        if (req.getCantidad() != null && req.getCantidad() > 0) {
            subtotalBase = subtotalBase.multiply(BigDecimal.valueOf(req.getCantidad()));
        }

        return subtotalBase;
    }

    private BigDecimal valorDecimal(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private Integer valorEntero(Integer valor) {
        return valor == null ? 0 : valor;
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
