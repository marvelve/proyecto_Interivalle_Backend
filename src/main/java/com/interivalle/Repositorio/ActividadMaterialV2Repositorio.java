/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.interivalle.Repositorio;

import com.interivalle.Modelo.ActividadMaterialV2;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author mary_
 */
public interface ActividadMaterialV2Repositorio extends JpaRepository<ActividadMaterialV2, Integer> {

    List<ActividadMaterialV2> findByActividad_IdActividadAndActivoTrue(Integer idActividad);

    List<ActividadMaterialV2> findByActividad_IdActividadAndActivoTrueOrderBySemanaAscIdActividadMaterialV2Asc(
            Integer idActividad
    );

}
