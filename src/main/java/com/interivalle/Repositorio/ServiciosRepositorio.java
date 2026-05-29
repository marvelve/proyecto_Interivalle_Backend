package com.interivalle.Repositorio;

import com.interivalle.Modelo.Servicios;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiciosRepositorio extends JpaRepository<Servicios, Integer> {

    // Lista servicios activos para seleccionarlos en solicitudes.
    List<Servicios> findByActivoTrue();
}
