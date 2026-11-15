package com.tecnologico.talleres.model;

import java.time.LocalDateTime;

// Estrellas y comentario de un cliente sobre un taller
public class Valoracion {

    private String id;
    private String usuarioId;
    private String tallerId;
    private String citaId;
    private int estrellas;
    private String comentario;
    private LocalDateTime createdAt;

    // Nombre del cliente (copia guardada en la valoracion)
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTallerId() {
        return tallerId;
    }

    public void setTallerId(String tallerId) {
        this.tallerId = tallerId;
    }

    // Opcional: valoracion ligada a una cita
    public String getCitaId() {
        return citaId;
    }

    public void setCitaId(String citaId) {
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
