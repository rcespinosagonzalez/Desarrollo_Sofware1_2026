package com.tecnologico.talleres.model;

public class Cita {

    private Long id;
    private String cliente;
    private String detalle;
    private String fecha;
    private String hora;

    public Cita() {
    }

    public Cita(Long id, String cliente, String detalle, String fecha, String hora) {
        this.id = id;
        this.cliente = cliente;
        this.detalle = detalle;
        this.fecha = fecha;
        this.hora = hora;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }
}
