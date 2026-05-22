package com.tecnologico.talleres.model;

import java.time.LocalDateTime;

// Registro de una busqueda en el mapa (tabla historial_busqueda)
public class BusquedaHistorial {

    private Long id;
    private Long usuarioId;
    private String termino;
    private Double origenLat;
    private Double origenLng;
    private Integer radioM;
    private Integer resultados;
    private String origen;
    private LocalDateTime createdAt;

    // Texto armado para mostrar en la lista del cliente
    private String texto;
    private String fechaHora;

    // Constructor vacio para frameworks y mapeo manual
    public BusquedaHistorial() {
    }

    // Fabrica: paso de fila JDBC a objeto listo para Thymeleaf/JSON
    public static BusquedaHistorial desdeBd(
            long id,
            long usuarioId,
            String termino,
            Double origenLat,
            Double origenLng,
            Integer radioM,
            Integer resultados,
            String origen,
            LocalDateTime createdAt,
            String fechaFormateada) {
        BusquedaHistorial h = new BusquedaHistorial();
        h.id = id;
        h.usuarioId = usuarioId;
        h.termino = termino;
        h.origenLat = origenLat;
        h.origenLng = origenLng;
        h.radioM = radioM;
        h.resultados = resultados;
        h.origen = origen;
        h.createdAt = createdAt;
        h.fechaHora = fechaFormateada;
        String display = termino != null ? termino : "";
        if (resultados != null && resultados > 0) {
            display = display + " · " + resultados + (resultados == 1 ? " taller" : " talleres");
        }
        h.texto = display;
        return h;
    }

    // PK historial_busqueda
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Cliente que hizo la busqueda
    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    // Texto que escribio en el buscador del mapa
    public String getTermino() {
        return termino;
    }

    public void setTermino(String termino) {
        this.termino = termino;
    }

    // Latitud del punto desde donde busco
    public Double getOrigenLat() {
        return origenLat;
    }

    public void setOrigenLat(Double origenLat) {
        this.origenLat = origenLat;
    }

    // Longitud del punto desde donde busco
    public Double getOrigenLng() {
        return origenLng;
    }

    public void setOrigenLng(Double origenLng) {
        this.origenLng = origenLng;
    }

    // Radio en metros (0 si no aplica)
    public Integer getRadioM() {
        return radioM;
    }

    public void setRadioM(Integer radioM) {
        this.radioM = radioM;
    }

    // Cuantos talleres coincidieron en esa busqueda
    public Integer getResultados() {
        return resultados;
    }

    public void setResultados(Integer resultados) {
        this.resultados = resultados;
    }

    // Siempre buscador en esta app
    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    // Fecha real en BD
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Linea que ve el cliente en el historial
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    // Fecha ya formateada para la vista (dd/MM/yyyy HH:mm)
    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }
}
