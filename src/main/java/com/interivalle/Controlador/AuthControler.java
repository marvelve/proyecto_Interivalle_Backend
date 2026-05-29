package com.interivalle.Controlador;

import com.interivalle.DTO.AuthResponse;
import com.interivalle.DTO.LoginRequest;
import com.interivalle.DTO.RegisterRequest;
import com.interivalle.Servicio.AuthServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthControler {

    @Autowired
    private AuthServicio authServicio;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest dto) {
        // Flujo publico de registro de clientes.
        authServicio.register(dto);

        // Respuesta HTTP usada por el frontend despues de crear el usuario.
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuario registrado correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest dto) {
        // Autentica y devuelve el token JWT con los datos basicos del usuario.
        return ResponseEntity.ok(authServicio.login(dto));
    }
}
