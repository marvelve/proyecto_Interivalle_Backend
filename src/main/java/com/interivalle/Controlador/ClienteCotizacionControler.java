package com.interivalle.Controlador;

import com.interivalle.DTO.AprobarCotizacionRequest;
import com.interivalle.DTO.CotizacionBaseFormularioResponse;
import com.interivalle.DTO.CotizacionBaseResponse;
import com.interivalle.DTO.CotizacionResponse;
import com.interivalle.DTO.CotizacionVistaCompletaResponse;
import com.interivalle.DTO.CrearCotizacionRequest;
import com.interivalle.DTO.GenerarCotizacionBaseRequest;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.SolicitudRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Servicio.CotizacionBaseV2Service;
import com.interivalle.Servicio.CotizacionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/cliente/cotizaciones")
@CrossOrigin(origins = "http://localhost:5173")
public class ClienteCotizacionControler {

    @Autowired
    private CotizacionService cotizacionService;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private SolicitudRepositorio solicitudRepo;

    @Autowired
    private CotizacionRepositorio cotizacionRepo;

    @Autowired
    private CotizacionBaseV2Service cotizacionBaseV2Service;

    @PostMapping("/generar-base")
    public CotizacionBaseResponse generarBase(
            Authentication authentication,
            @RequestBody GenerarCotizacionBaseRequest req
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        // Valida que el cliente solo genere cotizacion de sus propias solicitudes.
        validarSolicitudPerteneceAlCliente(req.getSolicitudId(), usuario.getIdUsuario());

        Cotizacion cotizacion = cotizacionBaseV2Service.generarCotizacionBaseV2(req);

        return construirCotizacionBaseResponse(
                cotizacion,
                req,
                "Cotizacion base generada correctamente con catalogo V2"
        );
    }

    @PostMapping
    public CotizacionResponse crear(
            Authentication authentication,
            @RequestBody CrearCotizacionRequest req
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return cotizacionService.crearCotizacion(usuario.getIdUsuario(), req);
    }

    @GetMapping("/{idCotizacion}")
    public CotizacionResponse verDetalle(
            Authentication authentication,
            @PathVariable Integer idCotizacion
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return cotizacionService.verDetalle(usuario.getIdUsuario(), idCotizacion);
    }

    @GetMapping
    public List<CotizacionResponse> listar(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return cotizacionService.listarPorCliente(usuario.getIdUsuario());
    }

    @GetMapping("/{idCotizacion}/vista-completa")
    public CotizacionVistaCompletaResponse vistaCompleta(
            @PathVariable Integer idCotizacion,
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return cotizacionService.obtenerVistaCompleta(usuario.getIdUsuario(), idCotizacion);
    }

    @GetMapping("/{idCotizacion}/base-formulario")
    public CotizacionBaseFormularioResponse obtenerFormularioBase(
            @PathVariable Integer idCotizacion,
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return cotizacionService.obtenerFormularioBase(usuario.getIdUsuario(), idCotizacion);
    }

    @PutMapping("/{idCotizacion}/base")
    public CotizacionBaseResponse actualizarBase(
            @PathVariable Integer idCotizacion,
            Authentication authentication,
            @RequestBody GenerarCotizacionBaseRequest req
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        // Valida pertenencia antes de permitir modificar la cotizacion base.
        validarCotizacionPerteneceAlCliente(idCotizacion, usuario.getIdUsuario());

        Cotizacion cotizacion = cotizacionBaseV2Service.actualizarCotizacionBaseV2(idCotizacion, req);

        return construirCotizacionBaseResponse(
                cotizacion,
                req,
                "Cotizacion base actualizada correctamente con catalogo V2"
        );
    }

    @PutMapping("/{idCotizacion}/aprobar")
    public ResponseEntity<CotizacionResponse> aprobar(
            @PathVariable Integer idCotizacion,
            Authentication authentication,
            @RequestBody AprobarCotizacionRequest req
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        CotizacionResponse response = cotizacionService.aprobar(usuario.getIdUsuario(), idCotizacion, req);
        return ResponseEntity.ok(response);
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String correo = authentication.getName();

        // Consulta el usuario autenticado para usar su id en validaciones.
        return usuarioRepo.findByCorreoUsuario(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    private void validarSolicitudPerteneceAlCliente(Integer idSolicitud, Integer idUsuario) {
        if (idSolicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar el id de la solicitud"
            );
        }

        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada"
                ));

        if (solicitud.getUsuario() == null
                || !solicitud.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes generar cotizacion para otra solicitud"
            );
        }
    }

    private void validarCotizacionPerteneceAlCliente(Integer idCotizacion, Integer idUsuario) {
        Cotizacion cotizacion = cotizacionRepo.findById(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));

        Solicitud solicitud = cotizacion.getSolicitud();

        if (solicitud == null
                || solicitud.getUsuario() == null
                || !solicitud.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes modificar cotizacion de otra solicitud"
            );
        }
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
