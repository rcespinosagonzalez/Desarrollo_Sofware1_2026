package com.tecnologico.talleres.model;

public class UltimaCalificacion {

    private String tallerNombre;
    private String servicio;
    private int estrellas = 5;
    private String comentario;

    public UltimaCalificacion() {
    }

    public UltimaCalificacion(String tallerNombre, String servicio, int estrellas, String comentario) {
        this.tallerNombre = tallerNombre;
        this.servicio = servicio;
        this.estrellas = estrellas;
        this.comentario = comentario;
    }

    public String getTallerNombre() {
        return tallerNombre;
    }

    public void setTallerNombre(String tallerNombre) {
        this.tallerNombre = tallerNombre;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
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
}
