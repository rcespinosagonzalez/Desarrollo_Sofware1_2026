package com.tecnologico.talleres.model;

// Formulario de valorar taller en buscar-talleres (no es entidad de BD)
public class UltimaCalificacion {

    private String tallerNombre;
    private String servicio;
    private int estrellas = 5;
    private String comentario;

    // Thymeleaf y Spring necesitan constructor sin args
    public UltimaCalificacion() {
    }

    // Valor por defecto cuando el cliente aun no ha valorado
    public UltimaCalificacion(String tallerNombre, String servicio, int estrellas, String comentario) {
        this.tallerNombre = tallerNombre;
        this.servicio = servicio;
        this.estrellas = estrellas;
        this.comentario = comentario;
    }

    // Nombre del taller a valorar (input del formulario)
    public String getTallerNombre() {
        return tallerNombre;
    }

    public void setTallerNombre(String tallerNombre) {
        this.tallerNombre = tallerNombre;
    }

    // Tipo de servicio (va dentro del comentario en BD)
    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    // De 1 a 5 estrellas
    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    // Texto libre del cliente
    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
