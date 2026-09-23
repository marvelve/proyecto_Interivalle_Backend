package com.interivalle.Controlador;

import com.interivalle.DTO.ObraBlancaRequest;
import com.interivalle.DTO.ObraBlancaResponse;
import com.interivalle.Servicio.ObraBlancaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/obra-blanca")
@CrossOrigin(origins = "*")
public class ObraBlancaControler {

    @Autowired
    private ObraBlancaService obraBlancaService;

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PostMapping
    public ObraBlancaResponse guardar(@Valid @RequestBody ObraBlancaRequest req, Authentication authentication) {
        // Guarda una actividad adicional de Mano de Obra / Obra Blanca.
        return obraBlancaService.guardar(req, authentication.getName());
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<ObraBlancaResponse> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return obraBlancaService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public ObraBlancaResponse obtenerPorId(@PathVariable Integer id) {
        return obraBlancaService.obtenerPorId(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PutMapping("/{id}")
    public ObraBlancaResponse actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ObraBlancaRequest req,
            Authentication authentication
    ) {
        return obraBlancaService.actualizar(id, req, authentication.getName());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id, Authentication authentication) {
        obraBlancaService.eliminar(id, authentication.getName());
    }
}
