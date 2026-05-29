package com.interivalle.DTO;

public class AuthResponse {

    // Respuesta que recibe el frontend despues del login.
    private String token;
    private String tipo = "Bearer";
    private String correoUsuario;
    private Integer idRol;

    public AuthResponse(String token, String correoUsuario, Integer idRol) {
        this.token = token;
        this.correoUsuario = correoUsuario;
        this.idRol = idRol;
    }

    public String getToken() {
        return token;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public Integer getIdRol() {
        return idRol;
    }
}
