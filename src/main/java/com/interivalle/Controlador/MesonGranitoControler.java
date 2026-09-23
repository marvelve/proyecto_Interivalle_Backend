package com.interivalle.Controlador;

import com.interivalle.DTO.MesonGranitoRequest;
import com.interivalle.Modelo.MesonGranito;
import com.interivalle.Servicio.MesonGranitoService;
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
@RequestMapping("/api/meson-granito")
@CrossOrigin(origins = "*")
public class MesonGranitoControler {

    @Autowired
    private MesonGranitoService mesonGranitoService;

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PostMapping
    public MesonGranito guardar(@RequestBody MesonGranitoRequest req, Authentication authentication) {
        // Guarda un adicional de meson/marmol/granito.
        return mesonGranitoService.guardar(req, authentication.getName());
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<MesonGranito> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return mesonGranitoService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public MesonGranito obtenerPorId(@PathVariable Integer id) {
        return mesonGranitoService.obtenerPorId(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PutMapping("/{id}")
    public MesonGranito actualizar(@PathVariable Integer id, @RequestBody MesonGranitoRequest req, Authentication authentication) {
        return mesonGranitoService.actualizar(id, req, authentication.getName());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id, Authentication authentication) {
        mesonGranitoService.eliminar(id, authentication.getName());
    }
}
