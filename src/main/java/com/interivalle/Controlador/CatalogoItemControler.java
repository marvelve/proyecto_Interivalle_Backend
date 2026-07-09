/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Controlador;

import com.interivalle.DTO.ActualizarPrecioCatalogoRequest;
import com.interivalle.DTO.ActividadMaterialV2Request;
import com.interivalle.DTO.ActividadMaterialV2Response;
import com.interivalle.DTO.CatalogoItemResponse;
import com.interivalle.DTO.CrearCatalogoItemRequest;
import com.interivalle.Servicio.CatalogoItemService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 *
 * @author mary_
 */
@RestController
@RequestMapping({"/api/catalogo-v2", "/api/catalogo-items"})
@CrossOrigin(origins = "*")
public class CatalogoItemControler {

    @Autowired
    private CatalogoItemService service;

    @GetMapping
    public List<CatalogoItemResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CatalogoItemResponse obtenerPorId(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PutMapping("/{id}/precio")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CatalogoItemResponse actualizarPrecio(
            @PathVariable Integer id,
            @RequestBody ActualizarPrecioCatalogoRequest dto
    ) {
        return service.actualizarPrecio(id, dto);
    }
    
        @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public CatalogoItemResponse crear(@RequestBody CrearCatalogoItemRequest dto) {
        return service.crear(dto);
    }

    @GetMapping("/materiales/{idMaterial}/relaciones")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    public List<ActividadMaterialV2Response> listarRelacionesMaterial(@PathVariable Integer idMaterial) {
        return service.listarRelacionesMaterial(idMaterial);
    }

    @GetMapping("/actividades/{idActividad}/relaciones")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    public List<ActividadMaterialV2Response> listarRelacionesActividad(@PathVariable Integer idActividad) {
        return service.listarRelacionesActividad(idActividad);
    }

    @PostMapping("/materiales/{idMaterial}/relaciones")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ActividadMaterialV2Response crearRelacionMaterial(
            @PathVariable Integer idMaterial,
            @RequestBody ActividadMaterialV2Request dto
    ) {
        return service.crearRelacionMaterial(idMaterial, dto);
    }

    @PutMapping("/materiales/{idMaterial}/relaciones/{idRelacion}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ActividadMaterialV2Response actualizarRelacionMaterial(
            @PathVariable Integer idMaterial,
            @PathVariable Integer idRelacion,
            @RequestBody ActividadMaterialV2Request dto
    ) {
        return service.actualizarRelacionMaterial(idMaterial, idRelacion, dto);
    }
}
