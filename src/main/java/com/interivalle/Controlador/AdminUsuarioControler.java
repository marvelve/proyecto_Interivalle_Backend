package com.interivalle.Controlador;

import com.interivalle.DTO.EstadoUpdate;
import com.interivalle.DTO.RolUpdate;
import com.interivalle.DTO.UsuarioCreateRequest;
import com.interivalle.DTO.UsuarioResponse;
import com.interivalle.DTO.UsuarioUpdate;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Servicio.AdminUsuarioServicio;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUsuarioControler {

    private final AdminUsuarioServicio adminService;

    public AdminUsuarioControler(AdminUsuarioServicio adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioCreateRequest dto) {
        // Crea usuarios desde el panel administrativo.
        Usuario usuario = adminService.crearUsuario(dto);
        UsuarioResponse response = adminService.toResponseDTO(usuario);

        // Devuelve 201 para indicar que el usuario fue creado correctamente.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(@RequestParam(required = false) Boolean estado) {
        List<Usuario> usuarios;

        // Si llega el parametro estado, se filtra; si no, se listan todos.
        if (estado != null) {
            usuarios = adminService.listarPorEstado(estado);
        } else {
            usuarios = adminService.listarTodos();
        }

        List<UsuarioResponse> response = usuarios.stream()
                .map(adminService::toResponseDTO)
                .collect(Collectors.toList());

        int total = response.size();

        // Header requerido por React Admin para paginar/listar correctamente.
        return ResponseEntity.ok()
                .header("Content-Range", "usuarios 0-" + (total > 0 ? total - 1 : 0) + "/" + total)
                .body(response);
    }

    @GetMapping("/{id}")
    public UsuarioResponse detalle(@PathVariable Integer id) {
        Usuario usuario = adminService.buscarPorId(id);
        return adminService.toResponseDTO(usuario);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioUpdate dto) {
        Usuario usuario = adminService.actualizarDatos(id, dto);
        return adminService.toResponseDTO(usuario);
    }

    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Integer id, @Valid @RequestBody RolUpdate dto) {
        // Cambia el rol manteniendo la validacion del servicio.
        Usuario usuario = adminService.cambiarRol(id, dto);
        return adminService.toResponseDTO(usuario);
    }

    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoUpdate dto) {
        // Activa o inactiva el usuario sin eliminarlo de la base de datos.
        Usuario usuario = adminService.cambiarEstado(id, dto);
        return adminService.toResponseDTO(usuario);
    }
}
