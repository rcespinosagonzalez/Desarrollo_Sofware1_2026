package com.tecnologico.talleres.model;

import java.util.ArrayList;
import java.util.List;

// Datos de la pagina de perfil del cliente: cuenta, vehiculos y un resumen de su actividad
public class Perfil {

    private String id;
    private String nombre;
    private String email;
    private String telefono = "";
    private String ciudad = "";
    // "noviembre de 2026"
    private String miembroDesde = "";
    // Letras del avatar, ej. "DB"
    private String iniciales = "?";
    private List<Vehiculo> vehiculos = new ArrayList<>();

    private long citasTotales;
    private long citasProximas;
    private long citasCompletadas;
    private long valoraciones;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getMiembroDesde() {
        return miembroDesde;
    }

    public void setMiembroDesde(String miembroDesde) {
        this.miembroDesde = miembroDesde;
    }

    public String getIniciales() {
        return iniciales;
    }

    public void setIniciales(String iniciales) {
        this.iniciales = iniciales;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }

    public long getCitasTotales() {
        return citasTotales;
    }

    public void setCitasTotales(long citasTotales) {
        this.citasTotales = citasTotales;
    }

    public long getCitasProximas() {
        return citasProximas;
    }

    public void setCitasProximas(long citasProximas) {
        this.citasProximas = citasProximas;
    }

    public long getCitasCompletadas() {
        return citasCompletadas;
    }

    public void setCitasCompletadas(long citasCompletadas) {
        this.citasCompletadas = citasCompletadas;
    }

    public long getValoraciones() {
        return valoraciones;
    }

    public void setValoraciones(long valoraciones) {
        this.valoraciones = valoraciones;
    }
}
