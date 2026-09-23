package com.interivalle.Controlador;

import com.interivalle.DTO.CarpinteriaRequest;
import com.interivalle.Modelo.Carpinteria;
import com.interivalle.Servicio.CarpinteriaService;
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
@RequestMapping("/api/carpinteria")
@CrossOrigin(origins = "*")
public class CarpinteriaControler {

    @Autowired
    private CarpinteriaService carpinteriaService;

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PostMapping
    public Carpinteria guardar(@RequestBody CarpinteriaRequest req, Authentication authentication) {
        // Guarda un adicional de carpinteria dentro de una cotizacion personalizada.
        return carpinteriaService.guardar(req, authentication.getName());
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<Carpinteria> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return carpinteriaService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public Carpinteria obtenerPorId(@PathVariable Integer id) {
        return carpinteriaService.obtenerPorId(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PutMapping("/{id}")
    public Carpinteria actualizar(@PathVariable Integer id, @RequestBody CarpinteriaRequest req, Authentication authentication) {
        return carpinteriaService.actualizar(id, req, authentication.getName());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id, Authentication authentication) {
        carpinteriaService.eliminar(id, authentication.getName());
    }
}
