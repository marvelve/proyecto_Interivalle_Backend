/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
/**
 *
 * @author mary_
 */

@Service
public class CarpinteriaService {

    @Autowired
    private CarpinteriaRepositorio carpinteriaRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionRepo;

    public Carpinteria guardar(CarpinteriaRequest req) {
        CotizacionPersonalizada cotizacion = cotizacionRepo.findById(req.getIdCotizacion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CotizaciÃ³n no encontrada"));

        validarCotizacionEditable(cotizacion);

        Carpinteria item = new Carpinteria();
        item.setCotizacionPersonalizada(cotizacion);
        item.setCotizacion(cotizacion);
        validarCotizacionEditable(item.getCotizacionPersonalizada());

        item.setTipoMueble(req.getTipoMueble());
        item.setMaterial(req.getMaterial());
        item.setLargo(valorDecimal(req.getLargo()));
        item.setAncho(valorDecimal(req.getAncho()));
        item.setAlto(valorDecimal(req.getAlto()));
        item.setCantidad(valorEntero(req.getCantidad()));
        item.setPrecioUnitario(valorDecimal(req.getPrecioUnitario()));
        item.setDescripcion(req.getDescripcion());
        item.setSubtotal(calcularSubtotal(req));

        return carpinteriaRepo.save(item);
    }

    public List<Carpinteria> listarPorCotizacion(Integer idCotizacion) {
        return carpinteriaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);
    }

    public Carpinteria obtenerPorId(Integer id) {
        return carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de carpinterÃ­a no encontrado"));
    }

    public Carpinteria actualizar(Integer id, CarpinteriaRequest req) {
        Carpinteria item = carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de carpinterÃ­a no encontrado"));

        if (req.getIdCotizacion() != null) {
            CotizacionPersonalizada cotizacion = cotizacionRepo.findById(req.getIdCotizacion())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CotizaciÃ³n no encontrada"));
            item.setCotizacionPersonalizada(cotizacion);
            item.setCotizacion(cotizacion);
        validarCotizacionEditable(item.getCotizacionPersonalizada());

        }

        validarCotizacionEditable(item.getCotizacionPersonalizada());

        item.setTipoMueble(req.getTipoMueble());
        item.setMaterial(req.getMaterial());
        item.setLargo(valorDecimal(req.getLargo()));
        item.setAncho(valorDecimal(req.getAncho()));
        item.setAlto(valorDecimal(req.getAlto()));
        item.setCantidad(valorEntero(req.getCantidad()));
        item.setPrecioUnitario(valorDecimal(req.getPrecioUnitario()));
        item.setDescripcion(req.getDescripcion());
        item.setSubtotal(calcularSubtotal(req));

        return carpinteriaRepo.save(item);
    }

    public void eliminar(Integer id) {
        Carpinteria item = carpinteriaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de carpinterÃ­a no encontrado"));

        validarCotizacionEditable(item.getCotizacionPersonalizada());

        carpinteriaRepo.delete(item);
    }

    private BigDecimal calcularSubtotal(CarpinteriaRequest req) {
        if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotalBase = BigDecimal.ZERO;

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CotizaciÃƒÂ³n personalizada no encontrada");
        }

        EstadoCotizacion estado = cotizacion.getCotizacion().getEstado();
        if (estado == EstadoCotizacion.APROBADA || estado == EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cotizaciÃƒÂ³n no se puede modificar porque estÃƒÂ¡ en estado " + estado.name()
            );
        }
    }
}

