package com.interivalle.Controlador;

import com.interivalle.DTO.CotizacionPersonalizadaDetalleResponse;
import com.interivalle.DTO.CotizacionPersonalizadaRequest;
import com.interivalle.DTO.CotizacionPersonalizadaResponse;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Servicio.CotizacionPersonalizadaService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cotizaciones-personalizadas")
@CrossOrigin(origins = "*")
public class CotizacionPersonalizadaControler {

    @Autowired
    private CotizacionPersonalizadaService cotizacionService;

    @Autowired
    private CotizacionRepositorio cotizacionBaseRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionPersonalizadaRepo;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PostMapping
    public CotizacionPersonalizadaResponse crear(
            @RequestBody CotizacionPersonalizadaRequest req,
            Authentication authentication
    ) {
        validarAccesoClienteCotizacionBase(req.getIdCotizacion(), authentication);
        // Crea la cabecera de actividades adicionales asociada a una cotizacion base.
        return cotizacionService.crear(req);
    }

    @GetMapping
    public List<CotizacionPersonalizadaResponse> listarTodas() {
        return cotizacionService.listarTodas();
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<CotizacionPersonalizadaResponse> listarPorUsuario(@PathVariable Integer idUsuario) {
        return cotizacionService.listarPorUsuario(idUsuario);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}/estado")
    public CotizacionPersonalizadaResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado
    ) {
        return cotizacionService.cambiarEstado(id, estado);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PutMapping("/{id}/recalcular")
    public CotizacionPersonalizadaResponse recalcular(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        validarAccesoClienteCotizacionPersonalizada(id, authentication);
        // Recalcula el total despues de guardar los adicionales por servicio.
        return cotizacionService.recalcular(id);
    }

    @GetMapping("/{idCotizacionPersonalizada}")
    public CotizacionPersonalizadaResponse obtenerPorId(@PathVariable Integer idCotizacionPersonalizada) {
        return cotizacionService.obtenerPorId(idCotizacionPersonalizada);
    }

    @GetMapping("/cotizacion/{idCotizacion}/detalle")
    public CotizacionPersonalizadaDetalleResponse obtenerDetallePorCotizacion(@PathVariable Integer idCotizacion) {
        return cotizacionService.obtenerDetallePorCotizacion(idCotizacion);
    }

    @GetMapping("/{idCotizacionPersonalizada}/detalle")
    public CotizacionPersonalizadaDetalleResponse obtenerDetallePorIdPersonalizada(
            @PathVariable Integer idCotizacionPersonalizada
    ) {
        return cotizacionService.obtenerDetalle(idCotizacionPersonalizada);
    }

    private void validarAccesoClienteCotizacionBase(
            Integer idCotizacion,
            Authentication authentication
    ) {
        if (!esCliente(authentication)) {
            return;
        }

        Cotizacion cotizacion = cotizacionBaseRepo.findById(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion base no encontrada"
                ));
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        if (cotizacion.getSolicitud() == null
                || cotizacion.getSolicitud().getUsuario() == null
                || !usuario.getIdUsuario().equals(
                        cotizacion.getSolicitud().getUsuario().getIdUsuario()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puede adicionar actividades a una cotizacion que no le pertenece"
            );
        }
    }

    private void validarAccesoClienteCotizacionPersonalizada(
            Integer idCotizacionPersonalizada,
            Authentication authentication
    ) {
        if (!esCliente(authentication)) {
            return;
        }

        CotizacionPersonalizada cotizacion = cotizacionPersonalizadaRepo
                .findById(idCotizacionPersonalizada)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion personalizada no encontrada"
                ));
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        if (cotizacion.getUsuario() == null
                || !usuario.getIdUsuario().equals(cotizacion.getUsuario().getIdUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puede modificar una cotizacion que no le pertenece"
            );
        }
    }

    private boolean esCliente(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "CLIENTE".equals(authority.getAuthority()));
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        return usuarioRepo.findByCorreoUsuario(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"
                ));
    }
}
