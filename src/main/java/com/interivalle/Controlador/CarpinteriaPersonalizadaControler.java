/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Controlador;

import com.interivalle.DTO.CarpinteriaPersonalizadaRequest;
import com.interivalle.Servicio.CarpinteriaPersonalizadaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 *
 * @author mary_
 */


@RestController
@RequestMapping("/api/cotizaciones-personalizadas/carpinteria")
@CrossOrigin(origins = "*")
public class CarpinteriaPersonalizadaControler {

    @Autowired
    private CarpinteriaPersonalizadaServicio service;

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody CarpinteriaPersonalizadaRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }
}