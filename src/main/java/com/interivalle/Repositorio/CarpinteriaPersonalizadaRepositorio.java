/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.interivalle.Repositorio;

import com.interivalle.Modelo.CarpinteriaPersonalizada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 *
 * @author mary_
 */

public interface CarpinteriaPersonalizadaRepositorio extends JpaRepository<CarpinteriaPersonalizada, Integer> {

    List<CarpinteriaPersonalizada> findByCotizacionPersonalizada_IdCotizacionPersonalizada(Integer idCotizacionPersonalizada);
}
