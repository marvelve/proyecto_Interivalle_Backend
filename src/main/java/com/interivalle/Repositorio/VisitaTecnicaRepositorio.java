package com.interivalle.Repositorio;

import com.interivalle.Modelo.VisitaTecnica;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitaTecnicaRepositorio extends JpaRepository<VisitaTecnica, Integer> {

    Optional<VisitaTecnica> findBySolicitud_IdSolicitud(Integer idSolicitud);

    List<VisitaTecnica> findByEstadoVisita(String estadoVisita);

    // Evita crear la misma visita para el mismo proyecto, fecha y hora.
    boolean existsBySolicitud_Usuario_CorreoUsuarioAndSolicitud_NombreProyectoUsuarioAndFechaVisitaAndHoraVisita(
            String correoUsuario,
            String nombreProyectoUsuario,
            LocalDate fechaVisita,
            LocalTime horaVisita
    );

    // Evita que un cliente tenga dos visitas en la misma fecha y hora.
    boolean existsBySolicitud_Usuario_CorreoUsuarioAndFechaVisitaAndHoraVisita(
            String correoUsuario,
            LocalDate fechaVisita,
            LocalTime horaVisita
    );

    // Valida si ya existe una visita para el mismo proyecto.
    Optional<VisitaTecnica> findBySolicitud_Usuario_CorreoUsuarioAndSolicitud_NombreProyectoUsuario(
            String correoUsuario,
            String nombreProyectoUsuario
    );
}
