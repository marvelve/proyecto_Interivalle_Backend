package com.interivalle.Repositorio;

import com.interivalle.Modelo.Solicitud;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepositorio extends JpaRepository<Solicitud, Integer> {

    Optional<Solicitud> findByIdSolicitud(Integer idSolicitud);

    // Permite listar las solicitudes de un cliente por correo.
    List<Solicitud> findByUsuarioCorreoUsuario(String correoUsuario);
}
