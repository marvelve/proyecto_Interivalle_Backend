package com.interivalle.Controlador;

import com.interivalle.DTO.CotizacionPersonalizadaDetalleResponse;
import com.interivalle.DTO.CotizacionPersonalizadaRequest;
import com.interivalle.DTO.CotizacionPersonalizadaResponse;
import com.interivalle.Servicio.CotizacionPersonalizadaService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cotizaciones-personalizadas")
@CrossOrigin(origins = "*")
public class CotizacionPersonalizadaControler {

    @Autowired
    private CotizacionPersonalizadaService cotizacionService;

    @PostMapping
    public CotizacionPersonalizadaResponse crear(@RequestBody CotizacionPersonalizadaRequest req) {
        // Crea la cabecera de actividades adicionales asociada a una cotizacion base.
        return cotizacionService.crear(req);
    }

    @GetMapping
    public List<CotizacionPersonalizadaResponse> listarTodas() {
        return cotizacionService.listarTodas();
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<CotizacionPersonalizadaResponse> listarPorUsuario(@PathVariable Integer idUsuario) {
        return cotizacionService.listarPorUsuario(idUsuario);
    }

    @PutMapping("/{id}/estado")
    public CotizacionPersonalizadaResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado
    ) {
        return cotizacionService.cambiarEstado(id, estado);
    }

    @PutMapping("/{id}/recalcular")
    public CotizacionPersonalizadaResponse recalcular(@PathVariable Integer id) {
        // Recalcula el total despues de guardar los adicionales por servicio.
        return cotizacionService.recalcular(id);
    }

    @GetMapping("/{idCotizacionPersonalizada}")
    public CotizacionPersonalizadaResponse obtenerPorId(@PathVariable Integer idCotizacionPersonalizada) {
        return cotizacionService.obtenerPorId(idCotizacionPersonalizada);
    }

    @GetMapping("/cotizacion/{idCotizacion}/detalle")
    public CotizacionPersonalizadaDetalleResponse obtenerDetallePorCotizacion(@PathVariable Integer idCotizacion) {
        return cotizacionService.obtenerDetallePorCotizacion(idCotizacion);
    }

    @GetMapping("/{idCotizacionPersonalizada}/detalle")
    public CotizacionPersonalizadaDetalleResponse obtenerDetallePorIdPersonalizada(
            @PathVariable Integer idCotizacionPersonalizada
    ) {
        return cotizacionService.obtenerDetalle(idCotizacionPersonalizada);
    }
}
