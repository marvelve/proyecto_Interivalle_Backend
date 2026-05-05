package com.interivalle.Controlador;

import com.interivalle.DTO.ActividadRequest;
import com.interivalle.DTO.ActividadResponse;
import com.interivalle.Servicio.ActividadService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actividades")
@CrossOrigin(origins = "*")
public class ActividadController {

    @Autowired
    private ActividadService actividadService;

    @GetMapping
    public List<ActividadResponse> listar(
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String texto
    ) {
        return actividadService.listar(idServicio, categoria, activo, texto);
    }

    @GetMapping("/{id}")
    public ActividadResponse obtenerPorId(@PathVariable Integer id) {
        return actividadService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ActividadResponse crear(@RequestBody ActividadRequest req) {
        return actividadService.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ActividadResponse actualizar(
            @PathVariable Integer id,
            @RequestBody ActividadRequest req
    ) {
        return actividadService.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ActividadResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestBody ActividadRequest req
    ) {
        return actividadService.cambiarEstado(id, req != null ? req.getActivo() : false);
    }

    @PostMapping("/migrar-catalogo")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, Integer> migrarDesdeCatalogoItem() {
        return actividadService.migrarDesdeCatalogoItem();
    }
}
