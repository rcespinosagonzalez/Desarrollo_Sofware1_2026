package com.tecnologico.talleres.model;

// Horario de atencion de un taller en un dia de la semana (1 = lunes ... 7 = domingo).
// Las horas van como texto "HH:mm" para que el JSON y el formulario sean simples.
public record HorarioDia(int dia, String apertura, String cierre) {}
