package com.interivalle.Servicio;

import com.interivalle.DTO.AuthResponse;
import com.interivalle.DTO.LoginRequest;
import com.interivalle.DTO.RegisterRequest;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServicio {

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public void register(RegisterRequest dto) {
        // Valida que el correo no exista antes de crear el usuario.
        if (usuarioRepo.existsByCorreoUsuario(dto.getCorreoUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }

        // Crea el usuario con la contrasena encriptada.
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setCorreoUsuario(dto.getCorreoUsuario());
        usuario.setContrasenaUsuario(passwordEncoder.encode(dto.getContrasenaUsuario()));
        usuario.setCelularUsuario(dto.getCelularUsuario());
        usuario.setCiudadUsuario(dto.getCiudadUsuario());

        // Se guarda con el rol por defecto definido en la entidad Usuario.
        usuarioRepo.save(usuario);
    }

    public AuthResponse login(LoginRequest dto) {
        // Busca el usuario por correo para validar sus credenciales.
        Usuario usuario = usuarioRepo.findByCorreoUsuario(dto.getCorreoUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Correo o contrasena invalidos"
                ));

        boolean contrasenaValida = passwordEncoder.matches(
                dto.getContrasenaUsuario(),
                usuario.getContrasenaUsuario()
        );

        if (!contrasenaValida) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contrasena invalidos");
        }

        // Genera el token que despues consume Spring Security en las rutas protegidas.
        String token = jwtService.generarToken(usuario.getCorreoUsuario(), usuario.getIdRol());
        return new AuthResponse(token, usuario.getCorreoUsuario(), usuario.getIdRol());
    }
}
