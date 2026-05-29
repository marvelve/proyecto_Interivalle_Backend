package com.interivalle.Controlador;

import com.interivalle.DTO.MesonGranitoRequest;
import com.interivalle.Modelo.MesonGranito;
import com.interivalle.Servicio.MesonGranitoService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    public MesonGranito guardar(@RequestBody MesonGranitoRequest req) {
        // Guarda un adicional de meson/marmol/granito.
        return mesonGranitoService.guardar(req);
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<MesonGranito> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return mesonGranitoService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public MesonGranito obtenerPorId(@PathVariable Integer id) {
        return mesonGranitoService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public MesonGranito actualizar(@PathVariable Integer id, @RequestBody MesonGranitoRequest req) {
        return mesonGranitoService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        mesonGranitoService.eliminar(id);
    }
}
