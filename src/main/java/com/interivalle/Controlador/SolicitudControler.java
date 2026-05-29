package com.interivalle.Controlador;

import com.interivalle.DTO.CrearSolicitud;
import com.interivalle.DTO.ReprogramarVisitaRequest;
import com.interivalle.DTO.SolicitudResponse;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Servicio.SolicitudService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = "*")
public class SolicitudControler {

    @Autowired
    private SolicitudService service;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@RequestBody CrearSolicitud dto) {
        // Crea solicitudes de cotizacion base o visita tecnica.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearSolicitud(dto));
    }

    @PutMapping("/{idSolicitud}/generar")
    public ResponseEntity<SolicitudResponse> generarCotizacion(@PathVariable Integer idSolicitud) {
        // Cambia una solicitud de cotizacion base a estado GENERADA.
        return ResponseEntity.ok(service.generarCotizacion(idSolicitud));
    }

    @GetMapping("/{idSolicitud}")
    public ResponseEntity<SolicitudResponse> obtener(@PathVariable Integer idSolicitud) {
        return ResponseEntity.ok(service.obtenerSolicitud(idSolicitud));
    }

    @GetMapping
    public List<SolicitudResponse> listarSolicitudes(
            @RequestParam(required = false) String correoUsuario
    ) {
        // Cliente: filtra por correo. Admin/Supervisor: lista todo.
        if (correoUsuario != null && !correoUsuario.isBlank()) {
            return service.listarPorCorreoUsuario(correoUsuario);
        }

        return service.listarTodas();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','CLIENTE')")
    @PutMapping("/{idSolicitud}/reprogramar")
    public ResponseEntity<SolicitudResponse> reprogramarVisita(
            @PathVariable Integer idSolicitud,
            @RequestBody ReprogramarVisitaRequest req,
            Authentication authentication
    ) {
        // Se toma el usuario autenticado para validar permisos en el servicio.
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        Usuario usuario = usuarioRepo.findByCorreoUsuario(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return ResponseEntity.ok(
                service.reprogramarVisita(idSolicitud, req, usuario.getIdUsuario())
        );
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR')")
    @PutMapping("/{idSolicitud}/realizada")
    public ResponseEntity<SolicitudResponse> marcarVisitaRealizada(@PathVariable Integer idSolicitud) {
        // Admin o supervisor marcan la visita como realizada.
        return ResponseEntity.ok(service.marcarVisitaRealizada(idSolicitud));
    }

    @PreAuthorize("hasAuthority('SUPERVISOR')")
    @PutMapping("/{idSolicitud}/confirmar-visita")
    public ResponseEntity<SolicitudResponse> confirmarVisitaTecnica(@PathVariable Integer idSolicitud) {
        // Supervisor confirma la visita y se notifica al cliente.
        return ResponseEntity.ok(service.confirmarVisitaTecnica(idSolicitud));
    }
}
