package com.interivalle.Controlador;

import com.interivalle.DTO.ReporteMaterialesCotizacionResponse;
import com.interivalle.Servicio.ReporteMaterialesCotizacionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteMaterialesCotizacionControler {

    private final ReporteMaterialesCotizacionService service;

    public ReporteMaterialesCotizacionControler(ReporteMaterialesCotizacionService service) {
        this.service = service;
    }

    @GetMapping("/materiales-cotizacion")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    public List<ReporteMaterialesCotizacionResponse> consultar(
            @RequestParam(required = false) Integer idCotizacion,
            @RequestParam(required = false) String nombreProyecto,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String servicio,
            @RequestParam(required = false) String estado
    ) {
        return service.consultar(idCotizacion, nombreProyecto, cliente, servicio, estado);
    }
}
