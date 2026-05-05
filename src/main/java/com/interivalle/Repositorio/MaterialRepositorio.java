package com.interivalle.Repositorio;

import com.interivalle.Modelo.Material;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepositorio extends JpaRepository<Material, Integer> {

    Optional<Material> findByCodigo(String codigo);

    Optional<Material> findByIdCatalogoItemOrigen(Integer idCatalogoItemOrigen);

    Optional<Material> findFirstByNombreMaterialIgnoreCase(String nombreMaterial);

    List<Material> findByActivoTrue();

    List<Material> findByServicio_IdServiciosAndActivoTrue(Integer idServicio);

    @Query("""
        SELECT m
        FROM Material m
        WHERE (:idServicio IS NULL OR m.servicio.idServicios = :idServicio)
          AND (:categoria IS NULL OR LOWER(m.categoria) LIKE LOWER(CONCAT('%', :categoria, '%')))
          AND (:activo IS NULL OR m.activo = :activo)
          AND (:texto IS NULL OR
               LOWER(m.nombreMaterial) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(m.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(m.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
        ORDER BY m.servicio.idServicios ASC, m.categoria ASC, m.nombreMaterial ASC
    """)
    List<Material> filtrar(
            @Param("idServicio") Integer idServicio,
            @Param("categoria") String categoria,
            @Param("activo") Boolean activo,
            @Param("texto") String texto
    );
}
