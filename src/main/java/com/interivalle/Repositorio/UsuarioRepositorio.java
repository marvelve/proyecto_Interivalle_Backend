package com.interivalle.Repositorio;

import com.interivalle.Modelo.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Integer> {

    // Busca un usuario por correo. Se usa en login y validaciones por cliente.
    Optional<Usuario> findByCorreoUsuario(String correoUsuario);

    // Verifica si el correo ya existe antes de registrar.
    boolean existsByCorreoUsuario(String correoUsuario);

    // Filtra usuarios activos o inactivos desde el modulo admin.
    List<Usuario> findByEstadoUsuario(Boolean estadoUsuario);

    // Permite consultar usuarios por rol.
    List<Usuario> findByIdRol(Integer idRol);
}
