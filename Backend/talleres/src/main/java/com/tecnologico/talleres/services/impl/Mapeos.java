package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.BusquedaDoc;
import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.HorarioDia;
import com.tecnologico.talleres.model.Mecanico;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.util.Ids;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Paso de documentos de MongoDB a los modelos que usan las vistas
final class Mapeos {

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Mapeos() {}

    static Taller taller(TallerDoc d, UsuarioDoc propietario) {
        Taller t = new Taller();
        t.setId(Ids.texto(d.id));
        t.setNombre(d.nombre);
        t.setTelefono(d.telefono);
        t.setDescripcion(d.descripcion);
        t.setServicios(d.servicios != null ? d.servicios : "");
        if (d.ubicacion != null) {
            t.setDireccion(d.ubicacion.direccion);
            t.setCiudad(d.ubicacion.ciudad);
            if (d.ubicacion.punto != null) {
                // GeoJSON guarda [longitud, latitud]: x es la longitud, y la latitud
                t.setLng(d.ubicacion.punto.getX());
                t.setLat(d.ubicacion.punto.getY());
            }
        }
        List<HorarioDia> horarios = new ArrayList<>();
        if (d.horarios != null) {
            for (TallerDoc.HorarioDoc h : d.horarios) {
                horarios.add(new HorarioDia(h.dia, h.apertura, h.cierre));
            }
            horarios.sort((a, b) -> Integer.compare(a.dia(), b.dia()));
        }
        t.setHorarios(horarios);
        if (d.resumen != null && d.resumen.total > 0) {
            t.setCalificacionPromedio((double) d.resumen.suma / d.resumen.total);
            t.setNumValoraciones(d.resumen.total);
        }
        if (d.destacadoHasta != null) {
            t.setDestacadoHasta(d.destacadoHasta.toString().substring(0, 10));
            t.setDestacado(d.destacadoHasta.isAfter(Instant.now()));
        }
        if (propietario != null) {
            t.setEmailPropietario(propietario.email);
            t.setNombrePropietario(propietario.nombre);
        }
        return t;
    }

    static List<HorarioDia> horarios(TallerDoc d) {
        List<HorarioDia> lista = new ArrayList<>();
        if (d.horarios != null) {
            for (TallerDoc.HorarioDoc h : d.horarios) {
                lista.add(new HorarioDia(h.dia, h.apertura, h.cierre));
            }
        }
        lista.sort((a, b) -> Integer.compare(a.dia(), b.dia()));
        return lista;
    }

    static Mecanico mecanico(TallerDoc taller, TallerDoc.MecanicoDoc m) {
        Mecanico x = new Mecanico();
        x.setId(Ids.texto(m.id));
        x.setTallerId(Ids.texto(taller.id));
        x.setNombres(m.nombres);
        x.setApellidos(m.apellidos);
        x.setTelefono(m.telefono);
        x.setActivo(m.activo);
        return x;
    }

    static Cita cita(CitaDoc d, ZoneId zona) {
        Cita c = new Cita();
        c.setId(Ids.texto(d.id));
        c.setUsuarioId(Ids.texto(d.clienteId));
        c.setTallerId(Ids.texto(d.tallerId));
        c.setEstado(d.estado);
        c.setTallerNombre(d.tallerNombre);
        c.setMecanicoNombre(d.mecanicoNombre);
        c.setCliente(d.cliente != null ? d.cliente : "Cliente");
        c.setDetalle(d.motivo != null ? d.motivo : "");
        c.setVehiculo(d.vehiculo);
        if (d.fechaHora != null) {
            LocalDateTime ldt = LocalDateTime.ofInstant(d.fechaHora, zona);
            c.setFecha(ldt.toLocalDate().toString());
            c.setHora(ldt.toLocalTime().format(HORA));
            c.setPasada(d.fechaHora.isBefore(Instant.now()));
        }
        if (d.creadoEn != null) {
            c.setCreadaEn(LocalDateTime.ofInstant(d.creadoEn, zona).format(FMT_FECHA_HORA));
        }
        c.setEditable(editable(d));
        return c;
    }

    // El cliente puede editar o cancelar una cita que sigue ocupando turno, esta solicitada o confirmada y es futura
    static boolean editable(CitaDoc d) {
        return d.ocupaTurno
                && ("solicitada".equals(d.estado) || "confirmada".equals(d.estado))
                && d.fechaHora != null
                && d.fechaHora.isAfter(Instant.now());
    }

    static Valoracion valoracion(ValoracionDoc d, ZoneId zona) {
        Valoracion v = new Valoracion();
        v.setId(Ids.texto(d.id));
        v.setUsuarioId(Ids.texto(d.clienteId));
        v.setTallerId(Ids.texto(d.tallerId));
        v.setCitaId(Ids.texto(d.citaId));
        v.setEstrellas(d.estrellas);
        v.setCliente(d.clienteNombre);
        v.setTallerNombre(d.tallerNombre);
        // Las pantallas muestran "Servicio: X — comentario", como antes de migrar
        String com = d.comentario != null ? d.comentario : "";
        if (d.servicio != null && !d.servicio.isBlank()) {
            com = com.isEmpty() ? "Servicio: " + d.servicio : "Servicio: " + d.servicio + " — " + com;
        }
        v.setComentario(com);
        if (d.creadoEn != null) {
            LocalDateTime ldt = LocalDateTime.ofInstant(d.creadoEn, zona);
            v.setCreatedAt(ldt);
            v.setFecha(ldt.toLocalDate().toString());
        }
        return v;
    }

    static BusquedaHistorial busqueda(BusquedaDoc d, ZoneId zona) {
        Instant cuando = d.creadoEn;
        LocalDateTime ldt = cuando != null ? LocalDateTime.ofInstant(cuando, zona) : null;
        return BusquedaHistorial.desdeBd(
                Ids.texto(d.id),
                Ids.texto(d.usuarioId),
                d.termino,
                d.origenLat,
                d.origenLng,
                d.radioM,
                d.resultados,
                d.origen,
                ldt,
                ldt != null ? FMT_FECHA_HORA.format(ldt) : "");
    }
}
