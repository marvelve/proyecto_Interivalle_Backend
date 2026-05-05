package com.interivalle.Repositorio;

import com.interivalle.Modelo.Actividad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActividadRepositorio extends JpaRepository<Actividad, Integer> {

    Optional<Actividad> findByCodigo(String codigo);

    Optional<Actividad> findByIdCatalogoItemOrigen(Integer idCatalogoItemOrigen);

    List<Actividad> findByActivoTrue();

    List<Actividad> findByServicio_IdServiciosAndActivoTrue(Integer idServicio);

    @Query("""
        SELECT a
        FROM Actividad a
        WHERE (:idServicio IS NULL OR a.servicio.idServicios = :idServicio)
          AND (:categoria IS NULL OR LOWER(a.categoria) LIKE LOWER(CONCAT('%', :categoria, '%')))
          AND (:activo IS NULL OR a.activo = :activo)
          AND (:texto IS NULL OR
               LOWER(a.nombreActividad) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(a.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
        ORDER BY a.servicio.idServicios ASC, a.orden ASC, a.nombreActividad ASC
    """)
    List<Actividad> filtrar(
            @Param("idServicio") Integer idServicio,
            @Param("categoria") String categoria,
            @Param("activo") Boolean activo,
            @Param("texto") String texto
    );
}
