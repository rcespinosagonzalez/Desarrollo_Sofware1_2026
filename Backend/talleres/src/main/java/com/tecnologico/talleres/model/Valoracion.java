package com.tecnologico.talleres.model;

public class Valoracion {

    private String cliente;
    private int estrellas;
    private String comentario;
    private String fecha;

    public Valoracion() {
    }

    public Valoracion(String cliente, int estrellas, String comentario, String fecha) {
        this.cliente = cliente;
        this.estrellas = estrellas;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
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

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
