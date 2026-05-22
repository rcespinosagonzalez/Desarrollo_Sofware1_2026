package com.tecnologico.talleres.model;

import java.time.LocalDateTime;

// Estrellas y comentario de un cliente sobre un taller
public class Valoracion {

    private Long id;
    private Long usuarioId;
    private Long tallerId;
    private Long citaId;
    private int estrellas;
    private String comentario;
    private LocalDateTime createdAt;

    // Nombre del cliente: JOIN con usuario
    private String cliente;
    private String tallerNombre;
    private String fecha;

    public Valoracion() {
    }

    // Constructor corto para vistas que no traen todos los campos
    public Valoracion(String cliente, int estrellas, String comentario, String fecha) {
        this.cliente = cliente;
        this.estrellas = estrellas;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getTallerId() {
        return tallerId;
    }

    public void setTallerId(Long tallerId) {
        this.tallerId = tallerId;
    }

    // Opcional: valoracion ligada a una cita
    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getTallerNombre() {
        return tallerNombre;
    }

    public void setTallerNombre(String tallerNombre) {
        this.tallerNombre = tallerNombre;
    }

    // Fecha en texto para tablas HTML
    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
