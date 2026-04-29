/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Servicio;

import com.interivalle.DTO.CarpinteriaPersonalizadaRequest;
import com.interivalle.Modelo.CarpinteriaPersonalizada;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Repositorio.CarpinteriaPersonalizadaRepositorio;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/**
 *
 * @author mary_
 */


@Service
@Transactional
public class CarpinteriaPersonalizadaServicio {


    @Autowired
    private CarpinteriaPersonalizadaRepositorio carpinteriaRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionPersonalizadaRepo;

    public CarpinteriaPersonalizada crear(CarpinteriaPersonalizadaRequest req) {

        if (req.getIdCotizacionPersonalizada() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar idCotizacionPersonalizada"
            );
        }

        CotizacionPersonalizada cotizacionPersonalizada = cotizacionPersonalizadaRepo
                .findById(req.getIdCotizacionPersonalizada())
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cotización personalizada no encontrada"
        ));

        if (req.getTipoMueble() == null || req.getTipoMueble().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tipo de mueble es obligatorio"
            );
        }

        if (req.getCantidad() == null || req.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe ser mayor a 0"
            );
        }

        if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio unitario debe ser mayor a 0"
            );
        }

        CarpinteriaPersonalizada carpinteria = new CarpinteriaPersonalizada();
        carpinteria.setCotizacionPersonalizada(cotizacionPersonalizada);
        carpinteria.setTipoMueble(req.getTipoMueble().trim());
        carpinteria.setMaterial(req.getMaterial());
        carpinteria.setLargo(req.getLargo());
        carpinteria.setAncho(req.getAncho());
        carpinteria.setAlto(req.getAlto());
        carpinteria.setCantidad(req.getCantidad());
        carpinteria.setPrecioUnitario(req.getPrecioUnitario());
        carpinteria.setDescripcion(req.getDescripcion());

        BigDecimal subtotal = req.getCantidad().multiply(req.getPrecioUnitario());
        carpinteria.setSubtotal(subtotal);

        return carpinteriaRepo.save(carpinteria);
    }
}
