package com.tecnologico.talleres.util;

import com.tecnologico.talleres.model.HorarioDia;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Reglas puras del horario de atencion y de los turnos (sin base de datos, facil de probar).
// Cada cita ocupa un turno de TURNO_MINUTOS que empieza en la hora de apertura y avanza de a un turno.
public final class HorarioAtencion {

    public static final int TURNO_MINUTOS = 60;

    private static final String[] DIAS = {"Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"};
    private static final DateTimeFormatter HORA_FORM = DateTimeFormatter.ofPattern("HH:mm");

    private HorarioAtencion() {
    }

    // Formulario del taller (dias marcados + una apertura y un cierre) -> filas de horario.
    // Lanza IllegalArgumentException con un mensaje para el usuario si algo no sirve.
    public static List<HorarioDia> desdeFormulario(List<Integer> dias, String apertura, String cierre) {
        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException("Marca al menos un día de atención.");
        }
        LocalTime a = leerHora(apertura, "apertura");
        LocalTime c = leerHora(cierre, "cierre");
        if (!a.isBefore(c)) {
            throw new IllegalArgumentException("La hora de cierre debe ser después de la de apertura.");
        }
        if (java.time.Duration.between(a, c).toMinutes() < TURNO_MINUTOS) {
            throw new IllegalArgumentException(
                    "El horario debe permitir al menos un turno de " + TURNO_MINUTOS + " minutos.");
        }
        List<HorarioDia> lista = new ArrayList<>();
        for (int d : new TreeSet<>(dias)) {
            if (d < 1 || d > 7) {
                throw new IllegalArgumentException("Día de atención no válido.");
            }
            lista.add(new HorarioDia(d, a.format(HORA_FORM), c.format(HORA_FORM)));
        }
        return lista;
    }

    private static LocalTime leerHora(String texto, String cual) {
        try {
            return LocalTime.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Escribe una hora de " + cual + " válida (por ejemplo 08:00).");
        }
    }

    public static HorarioDia deDia(List<HorarioDia> horarios, int dia) {
        if (horarios == null) {
            return null;
        }
        for (HorarioDia h : horarios) {
            if (h.dia() == dia) {
                return h;
            }
        }
        return null;
    }

    // Horas de inicio de los turnos de ese dia: apertura, apertura+1 turno... mientras el turno quepa antes del cierre
    public static List<LocalTime> turnos(HorarioDia h) {
        List<LocalTime> lista = new ArrayList<>();
        if (h == null) {
            return lista;
        }
        LocalTime t = LocalTime.parse(h.apertura());
        LocalTime cierre = LocalTime.parse(h.cierre());
        while (!t.plusMinutes(TURNO_MINUTOS).isAfter(cierre) && t.plusMinutes(TURNO_MINUTOS).isAfter(t)) {
            lista.add(t);
            t = t.plusMinutes(TURNO_MINUTOS);
        }
        return lista;
    }

    // Comprueba que la cita caiga en un turno valido del horario; si no, lanza IllegalArgumentException
    public static void validarTurno(List<HorarioDia> horarios, LocalDateTime inicio) {
        if (horarios == null || horarios.isEmpty()) {
            throw new IllegalArgumentException("Este taller aún no tiene horario de atención configurado.");
        }
        HorarioDia h = deDia(horarios, inicio.getDayOfWeek().getValue());
        if (h == null) {
            throw new IllegalArgumentException("El taller no atiende ese día (" + nombreDia(inicio.getDayOfWeek()) + ").");
        }
        List<LocalTime> turnos = turnos(h);
        LocalTime hora = inicio.toLocalTime();
        if (turnos.contains(hora)) {
            return;
        }
        LocalTime ap = LocalTime.parse(h.apertura());
        LocalTime ci = LocalTime.parse(h.cierre());
        if (hora.isBefore(ap) || hora.plusMinutes(TURNO_MINUTOS).isAfter(ci)) {
            throw new IllegalArgumentException("Ese día el taller atiende de " + sinCero(ap) + " a " + sinCero(ci)
                    + "; la última cita empieza una hora antes del cierre.");
        }
        throw new IllegalArgumentException("Elige una hora de inicio de turno (" + sinCero(turnos.get(0))
                + ", " + sinCero(turnos.size() > 1 ? turnos.get(1) : turnos.get(0)) + "...).");
    }

    // Texto para mostrar, por ejemplo "Lun - Sab 8:00 a 18:00" o "Todos los dias 7:00 a 19:00"
    public static String texto(List<HorarioDia> horarios) {
        if (horarios == null || horarios.isEmpty()) {
            return null;
        }
        // Agrupo los dias que comparten la misma apertura y cierre
        Map<String, TreeSet<Integer>> grupos = new LinkedHashMap<>();
        for (HorarioDia h : horarios) {
            String clave = sinCero(LocalTime.parse(h.apertura())) + " a " + sinCero(LocalTime.parse(h.cierre()));
            grupos.computeIfAbsent(clave, k -> new TreeSet<>()).add(h.dia());
        }
        List<String> partes = new ArrayList<>();
        for (Map.Entry<String, TreeSet<Integer>> g : grupos.entrySet()) {
            partes.add(textoDias(g.getValue()) + " " + g.getKey());
        }
        return String.join("; ", partes);
    }

    private static String textoDias(TreeSet<Integer> dias) {
        if (dias.size() == 7) {
            return "Todos los dias";
        }
        List<String> tramos = new ArrayList<>();
        Integer ini = null;
        Integer prev = null;
        for (int d : dias) {
            if (ini == null) {
                ini = d;
            } else if (d != prev + 1) {
                tramos.add(tramo(ini, prev));
                ini = d;
            }
            prev = d;
        }
        tramos.add(tramo(ini, prev));
        return String.join(", ", tramos);
    }

    // 3 o mas dias seguidos se muestran como rango ("Lun - Vie"); menos, uno por uno
    private static String tramo(int ini, int fin) {
        if (fin - ini >= 2) {
            return DIAS[ini - 1] + " - " + DIAS[fin - 1];
        }
        return fin == ini ? DIAS[ini - 1] : DIAS[ini - 1] + ", " + DIAS[fin - 1];
    }

    private static String nombreDia(DayOfWeek d) {
        return DIAS[d.getValue() - 1];
    }

    // 08:00 -> "8:00"
    private static String sinCero(LocalTime t) {
        return t.getHour() + ":" + String.format("%02d", t.getMinute());
    }
}
