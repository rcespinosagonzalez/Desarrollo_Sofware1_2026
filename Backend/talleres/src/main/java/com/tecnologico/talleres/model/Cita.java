package com.tecnologico.talleres.model;

// Cita entre un cliente y un taller; ocupa el turno de un mecanico
public class Cita {

    private String id;
    private String usuarioId;
    private String tallerId;
    private String estado;
    private String cliente;
    private String detalle;
    private String fecha;
    private String hora;

    // Nombre del taller viene del JOIN en listados
    private String tallerNombre;

    // Mecanico asignado automaticamente (puede faltar en citas antiguas)
    private String mecanicoNombre;

    // Vehiculo con el que va el cliente (opcional)
    private String vehiculo;

    // Para formularios y mapeo manual
    public Cita() {
    }

    // Datos minimos al armar listas en memoria
    public Cita(String id, String cliente, String detalle, String fecha, String hora) {
        this.id = id;
        this.cliente = cliente;
        this.detalle = detalle;
        this.fecha = fecha;
        this.hora = hora;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Id del usuario cliente
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    // Id del taller donde pide la cita
    public String getTallerId() {
        return tallerId;
    }

    public void setTallerId(String tallerId) {
        this.tallerId = tallerId;
    }

    // solicitada, confirmada, etc.
    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Nombre que escribe el cliente en el formulario
    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    // Motivo o descripcion del problema
    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    // yyyy-MM-dd del input date
    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    // HH:mm del input time
    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    // Solo lectura: copia del nombre del taller guardada en la cita
    public String getTallerNombre() {
        return tallerNombre;
    }

    public void setTallerNombre(String tallerNombre) {
        this.tallerNombre = tallerNombre;
    }

    public String getMecanicoNombre() {
        return mecanicoNombre;
    }

    public void setMecanicoNombre(String mecanicoNombre) {
        this.mecanicoNombre = mecanicoNombre;
    }

    // Cuando se creo la cita (dd/MM/yyyy HH:mm, en la zona del negocio)
    private String creadaEn;

    // true si la hora de la cita ya paso
    private boolean pasada;

    // true si el cliente todavia puede editarla o cancelarla (futura y sin cancelar ni completar)
    private boolean editable;

    public String getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(String creadaEn) {
        this.creadaEn = creadaEn;
    }

    public boolean isPasada() {
        return pasada;
    }

    public void setPasada(boolean pasada) {
        this.pasada = pasada;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // Texto para mostrar en pantalla (el estado en BD va en minusculas)
    public String getEstadoTexto() {
        if (estado == null) {
            return "Solicitada";
        }
        return switch (estado) {
            case "confirmada" -> "Confirmada";
            case "completada" -> "Completada";
            case "cancelada" -> "Cancelada";
            default -> "Solicitada";
        };
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }
}
