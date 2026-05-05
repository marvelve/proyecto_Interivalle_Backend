package com.interivalle.Controlador;

import com.interivalle.DTO.ProductoRequest;
import com.interivalle.DTO.ProductoResponse;
import com.interivalle.Servicio.ProductoService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String texto
    ) {
        return productoService.listar(idServicio, categoria, activo, texto);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtenerPorId(@PathVariable Integer id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ProductoResponse crear(@RequestBody ProductoRequest req) {
        return productoService.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ProductoResponse actualizar(
            @PathVariable Integer id,
            @RequestBody ProductoRequest req
    ) {
        return productoService.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ProductoResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestBody ProductoRequest req
    ) {
        return productoService.cambiarEstado(id, req != null ? req.getActivo() : false);
    }

    @PostMapping("/migrar-catalogo")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, Integer> migrarDesdeCatalogoItem() {
        return productoService.migrarDesdeCatalogoItem();
    }
}
