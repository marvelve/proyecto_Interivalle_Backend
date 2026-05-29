package com.interivalle.Controlador;

import com.interivalle.DTO.CotizacionBaseFormularioResponse;
import com.interivalle.DTO.CotizacionBaseResponse;
import com.interivalle.DTO.CotizacionResponse;
import com.interivalle.DTO.CotizacionVistaCompletaResponse;
import com.interivalle.DTO.GenerarCotizacionBaseRequest;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Servicio.CotizacionBaseV2Service;
import com.interivalle.Servicio.CotizacionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cotizaciones")
@CrossOrigin(origins = "http://localhost:5173")
public class CotizacionControler {

    private static final int ROL_ADMIN = 1;
    private static final int ROL_SUPERVISOR = 2;

    @Autowired
    private CotizacionService cotizacionService;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private CotizacionBaseV2Service cotizacionBaseV2Service;

    @GetMapping
    public List<CotizacionResponse> listarTodas(Authentication authentication) {
        validarAdminSupervisor(
                authentication,
                "No tiene permisos para listar todas las cotizaciones"
        );
        return cotizacionService.listarTodas();
    }

    @GetMapping("/{idCotizacion}")
    public CotizacionResponse verDetalleGeneral(
            @PathVariable Integer idCotizacion,
            Authentication authentication
    ) {
        validarAdminSupervisor(authentication, "No tiene permisos para ver esta cotizacion");
        return cotizacionService.verDetalleAdminSupervisor(idCotizacion);
    }

    @GetMapping("/{idCotizacion}/vista-completa")
    public CotizacionVistaCompletaResponse vistaCompletaGeneral(
            @PathVariable Integer idCotizacion,
            Authentication authentication
    ) {
        validarAdminSupervisor(authentication, "No tiene permisos para ver esta cotizacion");
        return cotizacionService.obtenerVistaCompletaAdminSupervisor(idCotizacion);
    }

    @GetMapping("/{idCotizacion}/base-formulario")
    public CotizacionBaseFormularioResponse obtenerFormularioBaseGeneral(
            @PathVariable Integer idCotizacion,
            Authentication authentication
    ) {
        validarAdminSupervisor(authentication, "No tiene permisos para gestionar esta cotizacion");
        return cotizacionService.obtenerFormularioBaseAdminSupervisor(idCotizacion);
    }

    @PostMapping("/generar-base")
    public CotizacionBaseResponse generarBaseGeneral(
            @RequestBody GenerarCotizacionBaseRequest req,
            Authentication authentication
    ) {
        validarAdminSupervisor(authentication, "No tiene permisos para gestionar esta cotizacion");

        // Genera la cotizacion base usando el catalogo nuevo V2.
        Cotizacion cotizacion = cotizacionBaseV2Service.generarCotizacionBaseV2(req);

        return construirCotizacionBaseResponse(
                cotizacion,
                req,
                "Cotizacion base generada correctamente con catalogo V2"
        );
    }

    @PutMapping("/{idCotizacion}/base")
    public CotizacionBaseResponse actualizarBaseGeneral(
            @PathVariable Integer idCotizacion,
            @RequestBody GenerarCotizacionBaseRequest req,
            Authentication authentication
    ) {
        validarAdminSupervisor(authentication, "No tiene permisos para gestionar esta cotizacion");

        // Actualiza la cotizacion base y recalcula sus detalles.
        Cotizacion cotizacion = cotizacionBaseV2Service.actualizarCotizacionBaseV2(idCotizacion, req);

        return construirCotizacionBaseResponse(
                cotizacion,
                req,
                "Cotizacion base actualizada correctamente con catalogo V2"
        );
    }

    private Usuario validarAdminSupervisor(Authentication authentication, String mensajePermiso) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Integer idRol = usuario.getIdRol();

        // Solo Admin y Supervisor pueden gestionar cotizaciones desde esta ruta.
        if (idRol != ROL_ADMIN && idRol != ROL_SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    mensajePermiso
            );
        }

        return usuario;
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String correo = authentication.getName();

        // Consulta el usuario del token para validar permisos.
        return usuarioRepo.findByCorreoUsuario(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    private CotizacionBaseResponse construirCotizacionBaseResponse(
            Cotizacion cotizacion,
            GenerarCotizacionBaseRequest req,
            String mensaje
    ) {
        CotizacionBaseResponse resp = new CotizacionBaseResponse();
        resp.setIdCotizacion(cotizacion.getIdCotizacion());
        resp.setSolicitudId(
                cotizacion.getSolicitud() != null
                        ? cotizacion.getSolicitud().getIdSolicitud()
                        : req.getSolicitudId()
        );
        resp.setMensaje(mensaje);
        resp.setManoObraProcesada(req.getManoObra() != null);
        resp.setCarpinteriaProcesada(req.getCarpinteria() != null);
        resp.setVidrioProcesado(req.getVidrio() != null);
        resp.setMezonProcesado(req.getMezon() != null);
        return resp;
    }
}
