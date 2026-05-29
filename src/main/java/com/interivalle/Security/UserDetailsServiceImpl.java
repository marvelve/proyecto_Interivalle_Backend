package com.interivalle.Security;

import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.UsuarioRepositorio;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        // Spring Security carga el usuario usando el correo que viene en el token.
        Usuario usuario = usuarioRepo.findByCorreoUsuario(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Se convierte idRol al nombre de autoridad que usa el sistema.
        String role = switch (usuario.getIdRol()) {
            case 1 -> "ADMIN";
            case 2 -> "SUPERVISOR";
            case 3 -> "CLIENTE";
            default -> throw new UsernameNotFoundException("Rol invalido");
        };

        return new org.springframework.security.core.userdetails.User(
                usuario.getCorreoUsuario(),
                usuario.getContrasenaUsuario(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
