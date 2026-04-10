package com.tecnologico.talleres.model;

public class BusquedaHistorial {

    private String texto;
    private String fechaHora;

    public BusquedaHistorial() {
    }

    public BusquedaHistorial(String texto, String fechaHora) {
        this.texto = texto;
        this.fechaHora = fechaHora;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }
}
