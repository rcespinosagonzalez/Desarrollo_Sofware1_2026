package com.tecnologico.talleres.model;

// Cita entre cliente (usuario_id) y taller; notas_usuario trae texto del formulario
public class Cita {

    private Long id;
    private Long usuarioId;
    private Long tallerId;
    private Long servicioId;
    private String estado;
    private String cliente;
    private String detalle;
    private String fecha;
    private String hora;

    // Nombre del taller viene del JOIN en listados
    private String tallerNombre;

    // Para formularios y JDBC
    public Cita() {
    }

    // Datos minimos al armar listas en memoria
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

    // FK usuario (cliente)
    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    // FK taller donde pide la cita
    public Long getTallerId() {
        return tallerId;
    }

    public void setTallerId(Long tallerId) {
        this.tallerId = tallerId;
    }

    // Servicio opcional (puede ser null)
    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    // solicitada, confirmada, etc.
    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Nombre que escribe el cliente (tambien va en notas_usuario)
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

    // Solo lectura en tablas con JOIN
    public String getTallerNombre() {
        return tallerNombre;
    }

    public void setTallerNombre(String tallerNombre) {
        this.tallerNombre = tallerNombre;
    }
}
