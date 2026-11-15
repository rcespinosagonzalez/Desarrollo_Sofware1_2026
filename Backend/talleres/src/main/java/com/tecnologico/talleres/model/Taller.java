package com.tecnologico.talleres.model;

import com.tecnologico.talleres.util.HorarioAtencion;

import java.util.ArrayList;
import java.util.List;

// Taller tal como lo ven las pantallas: ubicacion y horario aplanados, promedio calculado del resumen de valoraciones
public class Taller {

    // ObjectId de MongoDB en texto hexadecimal (24 caracteres)
    private String id;
    private String nombre;
    private String direccion;
    private String ciudad;
    private Double lat;
    private Double lng;
    private String telefono;
    private String descripcion;
    // Horario estructurado (una fila por dia); "horario" es el texto que se muestra y se calcula de aqui
    private List<HorarioDia> horarios = new ArrayList<>();

    // Solo formulario: dias marcados (1 = lunes ... 7 = domingo) y una apertura/cierre en formato HH:mm
    private List<Integer> diasAtencion = new ArrayList<>();
    private String horaApertura;
    private String horaCierre;
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

    // Direccion (ubicacion.direccion en el documento)
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

    // Texto para pantallas y JSON, por ejemplo "Lun - Sab 8:00 a 18:00"; null si no hay horario
    public String getHorario() {
        return HorarioAtencion.texto(horarios);
    }

    public List<HorarioDia> getHorarios() {
        return horarios;
    }

    // Al cargar de MongoDB tambien relleno los campos del formulario de edicion
    public void setHorarios(List<HorarioDia> horarios) {
        this.horarios = horarios == null ? new ArrayList<>() : horarios;
        this.diasAtencion = new ArrayList<>();
        for (HorarioDia h : this.horarios) {
            this.diasAtencion.add(h.dia());
        }
        if (!this.horarios.isEmpty()) {
            this.horaApertura = this.horarios.get(0).apertura();
            this.horaCierre = this.horarios.get(0).cierre();
        }
    }

    public List<Integer> getDiasAtencion() {
        return diasAtencion;
    }

    public void setDiasAtencion(List<Integer> diasAtencion) {
        this.diasAtencion = diasAtencion == null ? new ArrayList<>() : diasAtencion;
    }

    public String getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(String horaApertura) {
        this.horaApertura = horaApertura;
    }

    public String getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(String horaCierre) {
        this.horaCierre = horaCierre;
    }

    // Texto libre de servicios (campo servicios del documento)
    public String getServicios() {
        return servicios;
    }

    public void setServicios(String servicios) {
        this.servicios = servicios;
    }

    // Promedio = resumen.suma / resumen.total del documento
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

    // Plan destacado: true mientras destacadoHasta (yyyy-MM-dd) no haya pasado
    private boolean destacado;
    private String destacadoHasta = "";

    public boolean isDestacado() {
        return destacado;
    }

    public void setDestacado(boolean destacado) {
        this.destacado = destacado;
    }

    public String getDestacadoHasta() {
        return destacadoHasta;
    }

    public void setDestacadoHasta(String destacadoHasta) {
        this.destacadoHasta = destacadoHasta;
    }

    // Servicios como lista ("Frenos - Aceite" o separados por coma): para chips y tarjetas
    public List<String> getServiciosLista() {
        List<String> lista = new ArrayList<>();
        if (servicios != null) {
            for (String s : servicios.split("\\s+-\\s+|\\s*[,;|]\\s*")) {
                if (!s.isBlank()) {
                    lista.add(s.trim());
                }
            }
        }
        return lista;
    }
}
