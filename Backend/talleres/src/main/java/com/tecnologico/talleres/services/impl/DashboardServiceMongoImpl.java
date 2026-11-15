package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import com.tecnologico.talleres.model.HorarioDia;
import com.tecnologico.talleres.services.DashboardService;
import com.tecnologico.talleres.util.HorarioAtencion;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Dashboards sobre MongoDB. A esta escala (miles de documentos) bastan consultas de conteo con indice y
// unos pocos documentos con solo los campos necesarios; si la base crece, estos calculos pasan a pipelines
// de agregacion sin cambiar lo que devuelve este servicio.
@Service
public class DashboardServiceMongoImpl implements DashboardService {

    private static final String[] ESTADOS = {"solicitada", "confirmada", "completada", "cancelada"};
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM", ES);
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("d MMM", ES);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MongoTemplate mongo;
    private final ZoneId zona;

    public DashboardServiceMongoImpl(MongoTemplate mongo, @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.mongo = mongo;
        this.zona = ZoneId.of(zona);
    }

    // ------------------------------------------------------------------ admin

    @Override
    public Map<String, Object> resumenAdmin() {
        Instant ahora = Instant.now();
        Instant hace30 = ahora.minus(30, ChronoUnit.DAYS);
        LocalDate hoy = LocalDate.now(zona);

        // Talleres: solo los campos que se usan en estadisticas
        Query qt = new Query();
        qt.fields().include("nombre").include("estado").include("servicios").include("resumen")
                .include("creadoEn").include("destacadoHasta").include("ubicacion.ciudad");
        List<TallerDoc> talleres = mongo.find(qt, TallerDoc.class);

        int nuevos = 0;
        int destacados = 0;
        long sumaEstrellas = 0;
        long totalValoraciones = 0;
        Map<String, Integer> porCiudad = new HashMap<>();
        Map<String, Integer> porServicio = new HashMap<>();
        for (TallerDoc t : talleres) {
            if (t.creadoEn != null && t.creadoEn.isAfter(hace30)) {
                nuevos++;
            }
            if (t.destacadoHasta != null && t.destacadoHasta.isAfter(ahora)) {
                destacados++;
            }
            if (t.resumen != null) {
                sumaEstrellas += t.resumen.suma;
                totalValoraciones += t.resumen.total;
            }
            if (t.ubicacion != null && t.ubicacion.ciudad != null && !t.ubicacion.ciudad.isBlank()) {
                porCiudad.merge(t.ubicacion.ciudad.trim(), 1, Integer::sum);
            }
            for (String s : partirServicios(t.servicios)) {
                porServicio.merge(s, 1, Integer::sum);
            }
        }

        long citasTotal = mongo.count(new Query(), CitaDoc.class);
        long canceladas = contarEstado(null, "cancelada");
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("talleres", talleres.size());
        kpis.put("talleresNuevos30d", nuevos);
        kpis.put("destacadosActivos", destacados);
        kpis.put("clientes", mongo.count(Query.query(Criteria.where("rol").is("user").and("activo").is(true)), UsuarioDoc.class));
        kpis.put("clientesNuevos30d", mongo.count(Query.query(Criteria.where("rol").is("user").and("activo").is(true)
                .and("creadoEn").gt(hace30)), UsuarioDoc.class));
        kpis.put("duenos", mongo.count(Query.query(Criteria.where("rol").is("taller").and("activo").is(true)), UsuarioDoc.class));
        kpis.put("citasTotal", citasTotal);
        kpis.put("citasHoy", contarRango(null, dia(hoy), dia(hoy.plusDays(1)), false));
        kpis.put("citasPendientes", mongo.count(Query.query(Criteria.where("estado").is("solicitada")
                .and("fechaHora").gt(ahora)), CitaDoc.class));
        kpis.put("valoraciones", totalValoraciones);
        kpis.put("promedioGeneral", totalValoraciones == 0 ? 0.0 : redondear((double) sumaEstrellas / totalValoraciones));
        kpis.put("tasaCancelacion", citasTotal == 0 ? 0.0 : redondear(100.0 * canceladas / citasTotal));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("kpis", kpis);
        r.put("citasPorMes", citasPorMes(null));
        r.put("estados", estados(null));
        r.put("ciudades", top(porCiudad, 6));
        r.put("servicios", top(porServicio, 8));

        // Mejores talleres: promedio alto con al menos 5 valoraciones para que no gane uno con una sola
        List<TallerDoc> conNota = new ArrayList<>();
        for (TallerDoc t : talleres) {
            if (t.resumen != null && t.resumen.total >= 5) {
                conNota.add(t);
            }
        }
        conNota.sort((a, b) -> Double.compare(
                (double) b.resumen.suma / b.resumen.total, (double) a.resumen.suma / a.resumen.total));
        List<Map<String, Object>> mejores = new ArrayList<>();
        for (TallerDoc t : conNota.subList(0, Math.min(5, conNota.size()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", Ids.texto(t.id));
            m.put("nombre", t.nombre);
            m.put("ciudad", t.ubicacion != null ? t.ubicacion.ciudad : null);
            m.put("promedio", redondear((double) t.resumen.suma / t.resumen.total));
            m.put("total", t.resumen.total);
            mejores.add(m);
        }
        r.put("mejoresTalleres", mejores);

        List<Map<String, Object>> ultimasCitas = new ArrayList<>();
        for (CitaDoc c : mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "creadoEn")).limit(6), CitaDoc.class)) {
            ultimasCitas.add(resumenCita(c));
        }
        r.put("ultimasCitas", ultimasCitas);

        List<Map<String, Object>> ultimasValoraciones = new ArrayList<>();
        for (ValoracionDoc v : mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "creadoEn")).limit(5), ValoracionDoc.class)) {
            ultimasValoraciones.add(resumenValoracion(v));
        }
        r.put("ultimasValoraciones", ultimasValoraciones);
        return r;
    }

    // ------------------------------------------------------------------ dueno de taller

    @Override
    public Map<String, Object> resumenTaller(String propietarioUsuarioId) {
        ObjectId pid = Ids.oid(propietarioUsuarioId);
        List<TallerDoc> mios = pid == null ? List.of()
                : mongo.find(Query.query(Criteria.where("propietarioId").is(pid)), TallerDoc.class);
        Map<String, Object> r = new LinkedHashMap<>();
        if (mios.isEmpty()) {
            r.put("sinTaller", true);
            return r;
        }
        List<ObjectId> ids = new ArrayList<>();
        long suma = 0;
        long totalValoraciones = 0;
        int mecanicosActivos = 0;
        for (TallerDoc t : mios) {
            ids.add(t.id);
            if (t.resumen != null) {
                suma += t.resumen.suma;
                totalValoraciones += t.resumen.total;
            }
            for (TallerDoc.MecanicoDoc m : t.mecanicos) {
                if (m.activo) {
                    mecanicosActivos++;
                }
            }
        }
        Instant ahora = Instant.now();
        LocalDate hoy = LocalDate.now(zona);
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);

        // Una sola consulta con las citas de los ultimos 14 dias y los proximos 7; de ahi salen los graficos de dias
        Instant desde = dia(hoy.minusDays(14));
        Instant hasta = dia(hoy.plusDays(7));
        Map<LocalDate, Integer> porDia = new LinkedHashMap<>();
        for (int i = -14; i < 7; i++) {
            porDia.put(hoy.plusDays(i), 0);
        }
        int citasHoy = 0;
        for (CitaDoc c : mongo.find(Query.query(Criteria.where("tallerId").in(ids)
                .and("ocupaTurno").is(true)
                .and("fechaHora").gte(desde).lt(hasta)), CitaDoc.class)) {
            LocalDate d = c.fechaHora.atZone(zona).toLocalDate();
            porDia.merge(d, 1, Integer::sum);
            if (d.equals(hoy)) {
                citasHoy++;
            }
        }
        List<String> etiquetasPasado = new ArrayList<>();
        List<Integer> valoresPasado = new ArrayList<>();
        List<String> etiquetasFuturo = new ArrayList<>();
        List<Integer> valoresFuturo = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> e : porDia.entrySet()) {
            if (e.getKey().isBefore(hoy)) {
                etiquetasPasado.add(DIA_CORTO.format(e.getKey()));
                valoresPasado.add(e.getValue());
            } else {
                etiquetasFuturo.add(e.getKey().equals(hoy) ? "Hoy" : DIA_CORTO.format(e.getKey()));
                valoresFuturo.add(e.getValue());
            }
        }

        // Ocupacion de hoy = citas de hoy / turnos disponibles (turnos del dia x mecanicos activos de cada taller)
        int capacidadHoy = 0;
        for (TallerDoc t : mios) {
            int activos = 0;
            for (TallerDoc.MecanicoDoc m : t.mecanicos) {
                if (m.activo) {
                    activos++;
                }
            }
            for (HorarioDia h : Mapeos.horarios(t)) {
                if (h.dia() == hoy.getDayOfWeek().getValue()) {
                    capacidadHoy += HorarioAtencion.turnos(h).size() * activos;
                }
            }
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("citasHoy", citasHoy);
        kpis.put("ocupacionHoy", capacidadHoy == 0 ? 0 : (int) Math.min(100, Math.round(100.0 * citasHoy / capacidadHoy)));
        kpis.put("capacidadHoy", capacidadHoy);
        kpis.put("proximas7d", valoresFuturo.stream().mapToInt(Integer::intValue).sum());
        kpis.put("pendientes", contarPendientes(ids, ahora));
        kpis.put("completadasMes", contarRango(ids, dia(primerDiaMes), dia(hoy.plusDays(1)), true));
        kpis.put("citasTotal", mongo.count(Query.query(Criteria.where("tallerId").in(ids)), CitaDoc.class));
        kpis.put("mecanicosActivos", mecanicosActivos);
        kpis.put("valoraciones", totalValoraciones);
        kpis.put("promedio", totalValoraciones == 0 ? 0.0 : redondear((double) suma / totalValoraciones));
        long totalCitas = mongo.count(Query.query(Criteria.where("tallerId").in(ids)), CitaDoc.class);
        long canceladas = contarEstado(ids, "cancelada");
        kpis.put("tasaCancelacion", totalCitas == 0 ? 0.0 : redondear(100.0 * canceladas / totalCitas));

        r.put("sinTaller", false);
        r.put("taller", mios.get(0).nombre);
        r.put("kpis", kpis);
        r.put("ultimos14", serie(etiquetasPasado, valoresPasado));
        r.put("proximos7", serie(etiquetasFuturo, valoresFuturo));
        r.put("estados", estados(ids));
        r.put("citasPorMes", citasPorMes(ids));

        // Distribucion de estrellas: 5 conteos, de 5 a 1
        List<String> etqEstrellas = new ArrayList<>();
        List<Number> valEstrellas = new ArrayList<>();
        for (int e = 5; e >= 1; e--) {
            etqEstrellas.add(e + " ★");
            valEstrellas.add(mongo.count(Query.query(Criteria.where("tallerId").in(ids).and("estrellas").is(e)), ValoracionDoc.class));
        }
        r.put("estrellas", serie(etqEstrellas, valEstrellas));

        List<Map<String, Object>> ultimasValoraciones = new ArrayList<>();
        for (ValoracionDoc v : mongo.find(Query.query(Criteria.where("tallerId").in(ids))
                .with(Sort.by(Sort.Direction.DESC, "creadoEn")).limit(5), ValoracionDoc.class)) {
            ultimasValoraciones.add(resumenValoracion(v));
        }
        r.put("ultimasValoraciones", ultimasValoraciones);

        Map<String, Object> plan = new LinkedHashMap<>();
        TallerDoc primero = mios.get(0);
        boolean destacado = primero.destacadoHasta != null && primero.destacadoHasta.isAfter(ahora);
        plan.put("destacado", destacado);
        plan.put("hasta", primero.destacadoHasta == null ? null : primero.destacadoHasta.toString().substring(0, 10));
        r.put("plan", plan);
        return r;
    }

    // ------------------------------------------------------------------ utilidades

    // Citas por mes de los ultimos 6 meses (ids == null: toda la plataforma); no cuenta las canceladas
    private Map<String, Object> citasPorMes(List<ObjectId> ids) {
        LocalDate primero = LocalDate.now(zona).withDayOfMonth(1);
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate inicio = primero.minusMonths(i);
            String mes = MES.format(inicio).replace(".", "");
            etiquetas.add(mes);
            valores.add(contarRango(ids, dia(inicio), dia(inicio.plusMonths(1)), false));
        }
        return serie(etiquetas, valores);
    }

    // Cuantas citas hay en cada estado (ids == null: toda la plataforma)
    private Map<String, Object> estados(List<ObjectId> ids) {
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (String e : ESTADOS) {
            etiquetas.add(Character.toUpperCase(e.charAt(0)) + e.substring(1) + "s");
            valores.add(contarEstado(ids, e));
        }
        return serie(etiquetas, valores);
    }

    private long contarEstado(List<ObjectId> ids, String estado) {
        Criteria c = Criteria.where("estado").is(estado);
        if (ids != null) {
            c = c.and("tallerId").in(ids);
        }
        return mongo.count(Query.query(c), CitaDoc.class);
    }

    // Citas con fechaHora en [desde, hasta). Con soloCompletadas cuenta las completadas; si no, las que ocupan turno
    private long contarRango(List<ObjectId> ids, Instant desde, Instant hasta, boolean soloCompletadas) {
        Criteria c = Criteria.where("fechaHora").gte(desde).lt(hasta);
        if (soloCompletadas) {
            c = c.and("estado").is("completada");
        } else {
            c = c.and("ocupaTurno").is(true);
        }
        if (ids != null) {
            c = c.and("tallerId").in(ids);
        }
        return mongo.count(Query.query(c), CitaDoc.class);
    }

    private long contarPendientes(List<ObjectId> ids, Instant ahora) {
        return mongo.count(Query.query(Criteria.where("tallerId").in(ids)
                .and("estado").is("solicitada").and("fechaHora").gt(ahora)), CitaDoc.class);
    }

    private Instant dia(LocalDate d) {
        return d.atStartOfDay(zona).toInstant();
    }

    // Servicios de un taller ("Frenos - Aceite - Grua" o separados por coma)
    private static List<String> partirServicios(String texto) {
        List<String> lista = new ArrayList<>();
        if (texto == null) {
            return lista;
        }
        for (String s : texto.split("\\s+-\\s+|\\s*[,;|]\\s*")) {
            String limpio = s.trim();
            if (!limpio.isEmpty()) {
                lista.add(limpio);
            }
        }
        return lista;
    }

    // Las "n" claves con mas peso, de mayor a menor
    private static Map<String, Object> top(Map<String, Integer> conteos, int n) {
        List<Map.Entry<String, Integer>> orden = new ArrayList<>(conteos.entrySet());
        orden.sort((a, b) -> b.getValue() - a.getValue());
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (Map.Entry<String, Integer> e : orden.subList(0, Math.min(n, orden.size()))) {
            etiquetas.add(e.getKey());
            valores.add(e.getValue());
        }
        return serie(etiquetas, valores);
    }

    private static Map<String, Object> serie(List<String> etiquetas, List<? extends Number> valores) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("etiquetas", etiquetas);
        s.put("valores", valores);
        return s;
    }

    private Map<String, Object> resumenCita(CitaDoc c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taller", c.tallerNombre);
        m.put("cliente", c.cliente);
        m.put("estado", c.estado);
        m.put("cuando", c.fechaHora == null ? "" : FECHA_HORA.format(LocalDateTime.ofInstant(c.fechaHora, zona)));
        return m;
    }

    private static Map<String, Object> resumenValoracion(ValoracionDoc v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taller", v.tallerNombre);
        m.put("cliente", v.clienteNombre);
        m.put("estrellas", v.estrellas);
        m.put("comentario", v.comentario == null ? "" : v.comentario);
        return m;
    }

    private static double redondear(double x) {
        return Math.round(x * 10.0) / 10.0;
    }
}
