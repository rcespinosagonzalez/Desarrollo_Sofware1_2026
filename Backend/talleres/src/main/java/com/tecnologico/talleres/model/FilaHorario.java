package com.tecnologico.talleres.model;

// Una fila de la tabla de horarios de la ficha del taller: "Lunes" / "08:00 – 18:00"
public class FilaHorario {

    private final String dia;
    private final String horas;
    private final boolean hoy;
    private final boolean cerrado;

    public FilaHorario(String dia, String horas, boolean hoy, boolean cerrado) {
        this.dia = dia;
        this.horas = horas;
        this.hoy = hoy;
        this.cerrado = cerrado;
    }

    public String getDia() {
        return dia;
    }

    public String getHoras() {
        return horas;
    }

    public boolean isHoy() {
        return hoy;
    }

    public boolean isCerrado() {
        return cerrado;
    }
}
