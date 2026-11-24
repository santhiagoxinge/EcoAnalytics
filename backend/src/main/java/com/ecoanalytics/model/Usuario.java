package com.ecoanalytics.model;

import com.ecoanalytics.exception.ExcepcionNegocio;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "usuarios")
public class Usuario implements ConIdSecuencial {

    @Id
    private Long id;
    private String nombreCompleto;
    @Indexed(unique = true)
    private String correo;
    private String contrasenaCifrada;
    private Rol rol;
    private EstadoUsuario estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoIngreso;

    protected Usuario() {
    }

    public Usuario(String nombreCompleto, String correo, String contrasenaCifrada, Rol rol, EstadoUsuario estado) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo.toLowerCase();
        this.contrasenaCifrada = contrasenaCifrada;
        this.rol = rol;
        this.estado = estado;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** La propia cuenta sabe si puede ingresar al sistema (Experto en información). */
    public void validarInicioSesion() {
        if (estado == EstadoUsuario.PENDIENTE) {
            throw ExcepcionNegocio.prohibido("Su cuenta está pendiente de verificación institucional por un administrador.");
        }
        if (estado == EstadoUsuario.INACTIVO) {
            throw ExcepcionNegocio.prohibido("Su cuenta se encuentra inactiva. Contacte al administrador del sistema.");
        }
    }

    public void registrarIngreso() {
        this.ultimoIngreso = LocalDateTime.now();
    }

    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
    }

    public void activar() {
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void actualizar(String nombreCompleto, String correo, Rol rol) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo.toLowerCase();
        this.rol = rol;
    }

    public boolean esPendiente() {
        return estado == EstadoUsuario.PENDIENTE;
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCorreo() { return correo; }
    public String getContrasenaCifrada() { return contrasenaCifrada; }
    public Rol getRol() { return rol; }
    public EstadoUsuario getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getUltimoIngreso() { return ultimoIngreso; }
}
