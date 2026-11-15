package com.tecnologico.talleres.model;

// Vehiculo del cliente tal como lo muestra la pagina de perfil
public class Vehiculo {

    private String id;
    private String placa;
    // Codigo guardado: carro, camioneta, moto, camion u otro
    private String tipo;
    // Texto para mostrar: Carro, Camioneta, Moto, Camión u Otro
    private String tipoTexto;
    private String marca;
    private String modelo;
    private Integer anio;
    private String color;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipoTexto() {
        return tipoTexto;
    }

    public void setTipoTexto(String tipoTexto) {
        this.tipoTexto = tipoTexto;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    // "Mazda 3 · 2019" para el titulo de la tarjeta
    public String getDescripcion() {
        StringBuilder sb = new StringBuilder(marca == null ? "" : marca);
        if (modelo != null && !modelo.isBlank()) {
            sb.append(' ').append(modelo);
        }
        if (anio != null) {
            sb.append(" · ").append(anio);
        }
        return sb.toString();
    }
}
