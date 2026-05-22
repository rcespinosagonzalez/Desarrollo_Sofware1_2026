package com.tecnologico.talleres.model;

// Datos de tabla taller + ubicacion + promedio de valoraciones (JOIN en el servicio)
public class Taller {

    private Long id;
    private String nombre;
    private String direccion;
    private String ciudad;
    private Double lat;
    private Double lng;
    private String telefono;
    private String descripcion;
    private String horario;
    private String servicios;
    private double calificacionPromedio;
    private int numValoraciones;

    // Solo en formulario alta admin: creo usuario con rol taller
    private String emailCliente;
    private String claveCliente;
    private String nombreCliente;

    // Solo lectura: dueno real del registro en BD
    private String emailPropietario;
    private String nombrePropietario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Direccion en ubicacion_taller
    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    // Para marcador en Leaflet
    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    // Mapea a servicios_texto en BD
    public String getServicios() {
        return servicios;
    }

    public void setServicios(String servicios) {
        this.servicios = servicios;
    }

    // AVG de tabla valoracion
    public double getCalificacionPromedio() {
        return calificacionPromedio;
    }

    public void setCalificacionPromedio(double calificacionPromedio) {
        this.calificacionPromedio = calificacionPromedio;
    }

    public int getNumValoraciones() {
        return numValoraciones;
    }

    public void setNumValoraciones(int numValoraciones) {
        this.numValoraciones = numValoraciones;
    }

    // Correo del nuevo dueno al dar de alta taller
    public String getEmailCliente() {
        return emailCliente;
    }

    public void setEmailCliente(String emailCliente) {
        this.emailCliente = emailCliente;
    }

    public String getClaveCliente() {
        return claveCliente;
    }

    public void setClaveCliente(String claveCliente) {
        this.claveCliente = claveCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getEmailPropietario() {
        return emailPropietario;
    }

    public void setEmailPropietario(String emailPropietario) {
        this.emailPropietario = emailPropietario;
    }

    public String getNombrePropietario() {
        return nombrePropietario;
    }

    public void setNombrePropietario(String nombrePropietario) {
        this.nombrePropietario = nombrePropietario;
    }
}
