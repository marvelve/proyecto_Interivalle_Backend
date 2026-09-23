/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.interivalle.Repositorio;

import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Modelo.enums.TipoCotizacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/**
 *
 * @author mary_
 */

public interface CotizacionRepositorio extends JpaRepository<Cotizacion, Integer> {

    // Listar cotizaciones de un cliente (por usuario dueño de la solicitud)
    List<Cotizacion> findBySolicitud_Usuario_IdUsuario(Integer idUsuario);

    // Filtros opcionales
    List<Cotizacion> findBySolicitud_Usuario_IdUsuarioAndEstado(Integer idUsuario, EstadoCotizacion estado);

    Optional<Cotizacion> findFirstBySolicitud_IdSolicitudAndEstado(Integer idSolicitud, EstadoCotizacion estado);

    List<Cotizacion> findBySolicitud_IdSolicitud(Integer idSolicitud);

    List<Cotizacion> findBySolicitud_Usuario_IdUsuarioAndTipo(Integer idUsuario, TipoCotizacion tipo);

    List<Cotizacion> findBySolicitud_Usuario_IdUsuarioAndEstadoAndTipo(
            Integer idUsuario,
            EstadoCotizacion estado,
            TipoCotizacion tipo
    );
    
    Optional<Cotizacion> findByIdCotizacionAndSolicitud_Usuario_IdUsuario(
            Integer idCotizacion,
            Integer idUsuario
    );

    @Query("""
        SELECT DISTINCT c
        FROM Cotizacion c
        JOIN c.solicitud s
        JOIN s.usuario u
        LEFT JOIN c.detalles d
        LEFT JOIN d.servicio sv
        WHERE (:idCotizacion IS NULL OR c.idCotizacion = :idCotizacion)
          AND (:nombreProyecto IS NULL OR LOWER(s.nombreProyectoUsuario) LIKE LOWER(CONCAT('%', :nombreProyecto, '%')))
          AND (:cliente IS NULL OR LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :cliente, '%')))
          AND (:servicio IS NULL OR LOWER(sv.nombreServicio) LIKE LOWER(CONCAT('%', :servicio, '%')))
          AND (:estado IS NULL OR c.estado = :estado)
        ORDER BY c.idCotizacion DESC
        """)
    List<Cotizacion> buscarParaReporteMateriales(
            @Param("idCotizacion") Integer idCotizacion,
            @Param("nombreProyecto") String nombreProyecto,
            @Param("cliente") String cliente,
            @Param("servicio") String servicio,
            @Param("estado") EstadoCotizacion estado
    );
}
