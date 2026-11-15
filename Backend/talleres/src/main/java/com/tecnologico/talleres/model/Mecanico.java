package com.tecnologico.talleres.model;

// Mecanico de un taller; cada cita de un turno ocupa un mecanico
public class Mecanico {

    private String id;
    private String tallerId;
    private String nombres;
    private String apellidos;
    private String telefono;
    private boolean activo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTallerId() {
        return tallerId;
    }

    public void setTallerId(String tallerId) {
        this.tallerId = tallerId;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}
