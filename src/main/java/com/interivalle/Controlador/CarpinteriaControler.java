package com.interivalle.Controlador;

import com.interivalle.DTO.CarpinteriaRequest;
import com.interivalle.Modelo.Carpinteria;
import com.interivalle.Servicio.CarpinteriaService;
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
@RequestMapping("/api/carpinteria")
@CrossOrigin(origins = "*")
public class CarpinteriaControler {

    @Autowired
    private CarpinteriaService carpinteriaService;

    @PostMapping
    public Carpinteria guardar(@RequestBody CarpinteriaRequest req) {
        // Guarda un adicional de carpinteria dentro de una cotizacion personalizada.
        return carpinteriaService.guardar(req);
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public List<Carpinteria> listarPorCotizacion(@PathVariable Integer idCotizacion) {
        return carpinteriaService.listarPorCotizacion(idCotizacion);
    }

    @GetMapping("/{id}")
    public Carpinteria obtenerPorId(@PathVariable Integer id) {
        return carpinteriaService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public Carpinteria actualizar(@PathVariable Integer id, @RequestBody CarpinteriaRequest req) {
        return carpinteriaService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        carpinteriaService.eliminar(id);
    }
}
