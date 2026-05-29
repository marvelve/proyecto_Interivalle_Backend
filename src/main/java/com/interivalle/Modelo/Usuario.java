package com.interivalle.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 5, message = "El nombre debe tener minimo 5 caracteres")
    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es valido")
    @Column(name = "correo_usuario", unique = true)
    private String correoUsuario;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, message = "contrasena debe tener minimo 6 caracteres")
    @Column(name = "contrasena_usuario")
    private String contrasenaUsuario;

    @Column(name = "celular_usuario")
    private String celularUsuario;

    @Column(name = "ciudad_usuario")
    private String ciudadUsuario;

    @Column(name = "fecha_registro_usuario")
    private LocalDateTime fechaRegistroUsuario;

    // Rol por defecto: 3 = CLIENTE.
    @Column(name = "id_rol")
    private Integer idRol = 3;

    // true = ACTIVO, false = INACTIVO.
    @Column(name = "estado_usuario")
    private Boolean estadoUsuario = true;

    @PrePersist
    protected void onCreate() {
        // Se asigna automaticamente antes de guardar el registro.
        this.fechaRegistroUsuario = LocalDateTime.now();
    }

    public Usuario() {
        // Constructor vacio requerido por JPA.
    }

    public Usuario(String nombreUsuario, String correoUsuario, String contrasenaUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.correoUsuario = correoUsuario;
        this.contrasenaUsuario = contrasenaUsuario;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }

    public String getContrasenaUsuario() {
        return contrasenaUsuario;
    }

    public void setContrasenaUsuario(String contrasenaUsuario) {
        this.contrasenaUsuario = contrasenaUsuario;
    }

    public String getCelularUsuario() {
        return celularUsuario;
    }

    public void setCelularUsuario(String celularUsuario) {
        this.celularUsuario = celularUsuario;
    }

    public String getCiudadUsuario() {
        return ciudadUsuario;
    }

    public void setCiudadUsuario(String ciudadUsuario) {
        this.ciudadUsuario = ciudadUsuario;
    }

    public LocalDateTime getFechaRegistroUsuario() {
        return fechaRegistroUsuario;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public Boolean getEstadoUsuario() {
        return estadoUsuario;
    }

    public void setEstadoUsuario(Boolean estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }
}
