package com.interivalle.Servicio;

import com.interivalle.DTO.EstadoUpdate;
import com.interivalle.DTO.RolUpdate;
import com.interivalle.DTO.UsuarioCreateRequest;
import com.interivalle.DTO.UsuarioResponse;
import com.interivalle.DTO.UsuarioUpdate;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.UsuarioRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUsuarioServicio {

    private final UsuarioRepositorio usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminUsuarioServicio(UsuarioRepositorio usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepo.findAll();
    }

    public List<Usuario> listarPorEstado(Boolean estado) {
        return usuarioRepo.findByEstadoUsuario(estado);
    }

    public Usuario crearUsuario(UsuarioCreateRequest dto) {
        // Valida que no exista otro usuario con el mismo correo.
        Optional<Usuario> existente = usuarioRepo.findByCorreoUsuario(dto.getCorreoUsuario());
        if (existente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }

        // Crea el usuario con los datos enviados desde React Admin.
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setCorreoUsuario(dto.getCorreoUsuario());
        usuario.setContrasenaUsuario(passwordEncoder.encode(dto.getContrasenaUsuario()));
        usuario.setCelularUsuario(dto.getCelularUsuario());
        usuario.setCiudadUsuario(dto.getCiudadUsuario());

        // Si no mandan idRol, se asigna CLIENTE por defecto.
        if (dto.getIdRol() == null) {
            usuario.setIdRol(3);
        } else {
            usuario.setIdRol(dto.getIdRol());
        }

        // Todo usuario nuevo queda activo.
        usuario.setEstadoUsuario(true);

        return usuarioRepo.save(usuario);
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario actualizarDatos(Integer id, UsuarioUpdate dto) {
        Usuario usuario = buscarPorId(id);

        // Solo se actualizan los campos enviados en la peticion.
        if (dto.getNombreUsuario() != null) {
            usuario.setNombreUsuario(dto.getNombreUsuario());
        }

        if (dto.getCelularUsuario() != null) {
            usuario.setCelularUsuario(dto.getCelularUsuario());
        }

        if (dto.getCiudadUsuario() != null) {
            usuario.setCiudadUsuario(dto.getCiudadUsuario());
        }

        return usuarioRepo.save(usuario);
    }

    public Usuario cambiarRol(Integer id, RolUpdate dto) {
        // Roles validos: 1=ADMIN, 2=SUPERVISOR, 3=CLIENTE.
        if (dto.getIdRol() < 1 || dto.getIdRol() > 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "idRol invalido (1=ADMIN,2=SUPERVISOR,3=CLIENTE)"
            );
        }

        Usuario usuario = buscarPorId(id);
        usuario.setIdRol(dto.getIdRol());
        return usuarioRepo.save(usuario);
    }

    public Usuario cambiarEstado(Integer id, EstadoUpdate dto) {
        Usuario usuario = buscarPorId(id);
        usuario.setEstadoUsuario(dto.getEstadoUsuario());
        return usuarioRepo.save(usuario);
    }

    // Convierte la entidad a DTO para no devolver la contrasena.
    public UsuarioResponse toResponseDTO(Usuario usuario) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setCorreoUsuario(usuario.getCorreoUsuario());
        dto.setCelularUsuario(usuario.getCelularUsuario());
        dto.setCiudadUsuario(usuario.getCiudadUsuario());
        dto.setIdRol(usuario.getIdRol());
        dto.setEstadoUsuario(usuario.getEstadoUsuario());
        dto.setFechaRegistroUsuario(usuario.getFechaRegistroUsuario());
        return dto;
    }
}
