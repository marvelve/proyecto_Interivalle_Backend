package com.interivalle.Servicio;

import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CotizacionAdicionalPermisoService {

    private static final int ROL_ADMIN = 1;
    private static final int ROL_SUPERVISOR = 2;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    public void validarPuedeModificar(CotizacionPersonalizada cotizacionPersonalizada, String correoUsuario) {
        if (cotizacionPersonalizada == null || cotizacionPersonalizada.getCotizacion() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotizacion personalizada no encontrada");
        }

        validarPuedeModificar(cotizacionPersonalizada.getCotizacion(), correoUsuario);
    }

    public void validarPuedeModificar(Cotizacion cotizacionBase, String correoUsuario) {
        validarCotizacionEditable(cotizacionBase);

        Usuario usuario = obtenerUsuarioAutenticado(correoUsuario);
        if (esAdminSupervisor(usuario)) {
            return;
        }

        Integer idUsuarioCotizacion = obtenerIdClienteCotizacion(cotizacionBase);
        if (idUsuarioCotizacion == null || !idUsuarioCotizacion.equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puede modificar una cotizacion que no le pertenece"
            );
        }
    }

    private Usuario obtenerUsuarioAutenticado(String correoUsuario) {
        if (correoUsuario == null || correoUsuario.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        return usuarioRepo.findByCorreoUsuario(correoUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"
                ));
    }

    private boolean esAdminSupervisor(Usuario usuario) {
        Integer idRol = usuario.getIdRol();
        return idRol != null && (idRol == ROL_ADMIN || idRol == ROL_SUPERVISOR);
    }

    private Integer obtenerIdClienteCotizacion(Cotizacion cotizacionBase) {
        if (cotizacionBase.getSolicitud() != null
                && cotizacionBase.getSolicitud().getUsuario() != null) {
            return cotizacionBase.getSolicitud().getUsuario().getIdUsuario();
        }

        if (cotizacionBase.getCreadaPor() != null) {
            return cotizacionBase.getCreadaPor().getIdUsuario();
        }

        return null;
    }

    private void validarCotizacionEditable(Cotizacion cotizacionBase) {
        if (cotizacionBase == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotizacion base no encontrada");
        }

        EstadoCotizacion estado = cotizacionBase.getEstado();
        if (estado == EstadoCotizacion.APROBADA
                || estado == EstadoCotizacion.APROBADA_CLIENTE
                || estado == EstadoCotizacion.APROBADA_FINAL
                || estado == EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cotizacion no se puede modificar porque esta en estado " + estado.name()
            );
        }
    }
}
