package com.interivalle.Controlador;

import com.interivalle.DTO.MaterialRequest;
import com.interivalle.DTO.MaterialResponse;
import com.interivalle.Servicio.MaterialService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materiales")
@CrossOrigin(origins = "*")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    public List<MaterialResponse> listar(
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String texto
    ) {
        return materialService.listar(idServicio, categoria, activo, texto);
    }

    @GetMapping("/{id}")
    public MaterialResponse obtenerPorId(@PathVariable Integer id) {
        return materialService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public MaterialResponse crear(@RequestBody MaterialRequest req) {
        return materialService.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public MaterialResponse actualizar(
            @PathVariable Integer id,
            @RequestBody MaterialRequest req
    ) {
        return materialService.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ADMIN')")
    public MaterialResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestBody MaterialRequest req
    ) {
        return materialService.cambiarEstado(id, req != null ? req.getActivo() : false);
    }

    @PostMapping("/migrar-catalogo")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, Integer> migrarDesdeCatalogoItem() {
        return materialService.migrarDesdeCatalogoItem();
    }
}
