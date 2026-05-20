package com.interivalle.Repositorio;

import com.interivalle.Modelo.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepositorio extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigo(String codigo);
    
    List<Producto> findByServicio_IdServiciosAndActivoTrueOrderBySemanaAscIdProductoAsc(Integer idServicio);

    Optional<Producto> findByIdCatalogoItemOrigen(Integer idCatalogoItemOrigen);

    List<Producto> findByActivoTrue();

    List<Producto> findByServicio_IdServiciosAndActivoTrue(Integer idServicio);

    @Query("""
        SELECT p
        FROM Producto p
        WHERE (:idServicio IS NULL OR p.servicio.idServicios = :idServicio)
          AND (:categoria IS NULL OR LOWER(p.categoria) LIKE LOWER(CONCAT('%', :categoria, '%')))
          AND (:activo IS NULL OR p.activo = :activo)
          AND (:texto IS NULL OR
               LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR
               LOWER(p.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
        ORDER BY p.servicio.idServicios ASC, p.semana ASC, p.categoria ASC, p.nombreProducto ASC
    """)
    List<Producto> filtrar(
            @Param("idServicio") Integer idServicio,
            @Param("categoria") String categoria,
            @Param("activo") Boolean activo,
            @Param("texto") String texto
    );
}
