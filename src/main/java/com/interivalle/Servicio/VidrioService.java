package com.interivalle.Servicio;

import com.interivalle.DTO.VidrioRequest;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.Vidrio;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import com.interivalle.Repositorio.VidrioRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VidrioService {

    @Autowired
    private VidrioRepositorio vidrioRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionRepo;

    public Vidrio guardar(VidrioRequest req) {
        CotizacionPersonalizada cotizacion = buscarCotizacionPersonalizada(req.getIdCotizacion());
        validarCotizacionEditable(cotizacion);

        Vidrio item = new Vidrio();
        item.setCotizacionPersonalizada(cotizacion);
        cargarDatosItem(item, req);

        return vidrioRepo.save(item);
    }

    public List<Vidrio> listarPorCotizacion(Integer idCotizacion) {
        return vidrioRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);
    }

    public Vidrio obtenerPorId(Integer id) {
        return vidrioRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de vidrio no encontrado"
                ));
    }

    public Vidrio actualizar(Integer id, VidrioRequest req) {
        Vidrio item = vidrioRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de vidrio no encontrado"
                ));

        if (req.getIdCotizacion() != null) {
            CotizacionPersonalizada cotizacion = buscarCotizacionPersonalizada(req.getIdCotizacion());
            item.setCotizacionPersonalizada(cotizacion);
        }

        validarCotizacionEditable(item.getCotizacionPersonalizada());
        cargarDatosItem(item, req);

        return vidrioRepo.save(item);
    }

    public void eliminar(Integer id) {
        Vidrio item = vidrioRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de vidrio no encontrado"
                ));

        validarCotizacionEditable(item.getCotizacionPersonalizada());
        vidrioRepo.delete(item);
    }

    private CotizacionPersonalizada buscarCotizacionPersonalizada(Integer idCotizacionPersonalizada) {
        // Vidrio recibe el id de cotizacion personalizada desde el formulario.
        return cotizacionRepo.findById(idCotizacionPersonalizada)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));
    }

    private void cargarDatosItem(Vidrio item, VidrioRequest req) {
        item.setTipoVidrio(req.getTipoVidrio());
        item.setAncho(req.getAncho());
        item.setAlto(req.getAlto());
        item.setCantidad(req.getCantidad());
        item.setInstalacion(req.getInstalacion());
        item.setPrecioUnitario(req.getPrecioUnitario());
        item.setDescripcion(req.getDescripcion());
        item.setSubtotal(calcularSubtotal(req));
    }

    private BigDecimal calcularSubtotal(VidrioRequest req) {
        // Calcula por area cuando llegan ancho y alto; luego aplica cantidad e instalacion.
        if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotalBase;

        if (req.getAncho() != null && req.getAncho().compareTo(BigDecimal.ZERO) > 0
                && req.getAlto() != null && req.getAlto().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal area = req.getAncho().multiply(req.getAlto());
            subtotalBase = area.multiply(req.getPrecioUnitario());
        } else {
            subtotalBase = req.getPrecioUnitario();
        }

        if (req.getCantidad() != null && req.getCantidad() > 0) {
            subtotalBase = subtotalBase.multiply(BigDecimal.valueOf(req.getCantidad()));
        }

        // Recargo por instalacion: 10%.
        if (Boolean.TRUE.equals(req.getInstalacion())) {
            BigDecimal recargo = subtotalBase.multiply(new BigDecimal("0.10"));
            subtotalBase = subtotalBase.add(recargo);
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
