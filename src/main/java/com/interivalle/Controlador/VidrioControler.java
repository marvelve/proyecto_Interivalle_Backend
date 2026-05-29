package com.interivalle.Controlador;

import com.interivalle.DTO.VidrioRequest;
import com.interivalle.Modelo.Vidrio;
import com.interivalle.Servicio.VidrioService;
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
@RequestMapping("/api/vidrio")
@CrossOrigin(origins = "*")
public class VidrioControler {

    @Autowired
    private VidrioService vidrioService;

    @PostMapping
    public Vidrio guardar(@RequestBody VidrioRequest req) {
        // Guarda un adicional de divisiones en vidrio.
        return vidrioService.guardar(req);
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<Vidrio> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return vidrioService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public Vidrio obtenerPorId(@PathVariable Integer id) {
        return vidrioService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public Vidrio actualizar(@PathVariable Integer id, @RequestBody VidrioRequest req) {
        return vidrioService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        vidrioService.eliminar(id);
    }
}
