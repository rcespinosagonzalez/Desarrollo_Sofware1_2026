package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.HorarioDia;
import com.tecnologico.talleres.services.AgendaService;
import com.tecnologico.talleres.util.HorarioAtencion;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Agenda sobre MongoDB. No hay bloqueos ni transacciones: la regla "un mecanico no tiene dos citas a la
// misma hora" la garantiza el indice unico parcial uk_citas_mecanico_turno. Si dos personas piden el mismo
// mecanico a la vez, a una el insert le falla con DuplicateKeyException y vuelve a intentar con otro mecanico.
@Service
public class AgendaServiceMongoImpl implements AgendaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final MongoTemplate mongo;
    private final ZoneId zona;

    public AgendaServiceMongoImpl(MongoTemplate mongo, @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.mongo = mongo;
        this.zona = ZoneId.of(zona);
    }

    @Override
    public List<String> horasLibres(String tallerId, LocalDate fecha) {
        return horasLibres(tallerId, fecha, null);
    }

    @Override
    public List<String> horasLibres(String tallerId, LocalDate fecha, String excluirCitaId) {
        List<String> libres = new ArrayList<>();
        ObjectId excluir = Ids.oid(excluirCitaId);
        ObjectId tid = Ids.oid(tallerId);
        if (fecha == null || tid == null) {
            return libres;
        }
        LocalDateTime ahora = LocalDateTime.now(zona);
        if (fecha.isBefore(ahora.toLocalDate())) {
            return libres;
        }
        TallerDoc taller = mongo.findById(tid, TallerDoc.class);
        if (taller == null) {
            return libres;
        }
        HorarioDia h = HorarioAtencion.deDia(Mapeos.horarios(taller), fecha.getDayOfWeek().getValue());
        if (h == null) {
            return libres;
        }
        long mecanicos = taller.mecanicos.stream().filter(m -> m.activo).count();
        if (mecanicos == 0) {
            return libres;
        }
        // Cuantas citas activas hay en cada hora de ese dia (cada cita ocupa un mecanico)
        Instant desde = fecha.atStartOfDay(zona).toInstant();
        Instant hasta = fecha.plusDays(1).atStartOfDay(zona).toInstant();
        Map<LocalTime, Integer> ocupadas = new HashMap<>();
        for (CitaDoc c : mongo.find(Query.query(Criteria.where("tallerId").is(tid)
                .and("fechaHora").gte(desde).lt(hasta)
                .and("ocupaTurno").is(true)), CitaDoc.class)) {
            if (excluir != null && excluir.equals(c.id)) {
                continue;
            }
            ocupadas.merge(LocalDateTime.ofInstant(c.fechaHora, zona).toLocalTime(), 1, Integer::sum);
        }
        for (LocalTime turno : HorarioAtencion.turnos(h)) {
            boolean futuro = LocalDateTime.of(fecha, turno).isAfter(ahora);
            if (futuro && ocupadas.getOrDefault(turno, 0) < mecanicos) {
                libres.add(turno.format(HORA));
            }
        }
        return libres;
    }

    @Override
    public void solicitarCita(String tallerId, String clienteUsuarioId, String cliente, String motivo,
                              String fecha, String hora, String vehiculo) {
        ObjectId tid = Ids.oid(tallerId);
        if (tid == null) {
            throw new IllegalArgumentException("Elige un taller para la cita.");
        }
        ObjectId clienteOid = Ids.oid(clienteUsuarioId);
        if (clienteOid == null) {
            throw new IllegalArgumentException("Inicia sesión para pedir una cita.");
        }
        String detalle = motivo == null ? "" : motivo.trim();
        if (detalle.isEmpty()) {
            throw new IllegalArgumentException("Escribe el motivo de la cita.");
        }
        if (detalle.length() > 300) {
            throw new IllegalArgumentException("El motivo es demasiado largo (máximo 300 caracteres).");
        }
        LocalDateTime inicio = leerInicio(fecha, hora);

        TallerDoc taller = mongo.findById(tid, TallerDoc.class);
        if (taller == null) {
            throw new IllegalArgumentException("El taller elegido no existe.");
        }
        HorarioAtencion.validarTurno(Mapeos.horarios(taller), inicio);
        if (!inicio.isAfter(LocalDateTime.now(zona))) {
            throw new IllegalArgumentException("La fecha y hora de la cita ya pasaron.");
        }
        List<TallerDoc.MecanicoDoc> activos = taller.mecanicos.stream().filter(m -> m.activo).toList();
        if (activos.isEmpty()) {
            throw new IllegalArgumentException("Este taller aún no tiene mecánicos disponibles.");
        }

        Instant fechaHora = inicio.atZone(zona).toInstant();
        String nombre = cliente == null || cliente.isBlank() ? "Cliente web" : cliente.trim();
        // Cada intento que choca descubre un mecanico mas ocupado; con N mecanicos bastan N + 1 intentos
        for (int intento = 0; intento <= activos.size(); intento++) {
            Set<ObjectId> ocupados = new HashSet<>();
            for (CitaDoc c : mongo.find(Query.query(Criteria.where("tallerId").is(tid)
                    .and("fechaHora").is(fechaHora)
                    .and("ocupaTurno").is(true)), CitaDoc.class)) {
                ocupados.add(c.mecanicoId);
            }
            TallerDoc.MecanicoDoc libre = null;
            for (TallerDoc.MecanicoDoc m : activos) {
                if (!ocupados.contains(m.id)) {
                    libre = m;
                    break;
                }
            }
            if (libre == null) {
                throw new IllegalArgumentException("Esa hora ya no está disponible. Elige otra.");
            }
            CitaDoc cita = new CitaDoc();
            cita.id = new ObjectId();
            cita.clienteId = clienteOid;
            cita.tallerId = tid;
            cita.mecanicoId = libre.id;
            cita.tallerNombre = taller.nombre;
            cita.mecanicoNombre = (libre.nombres + " " + libre.apellidos).trim();
            cita.fechaHora = fechaHora;
            cita.estado = "solicitada";
            cita.ocupaTurno = true;
            cita.cliente = nombre;
            cita.motivo = detalle;
            cita.vehiculo = vehiculo == null || vehiculo.isBlank() ? null : vehiculo.trim();
            cita.creadoEn = Instant.now();
            try {
                mongo.insert(cita);
                return;
            } catch (DuplicateKeyException ex) {
                // Otra persona tomo a ese mecanico justo ahora: releo y pruebo con otro
            }
        }
        throw new IllegalArgumentException("Esa hora ya no está disponible. Elige otra.");
    }

    @Override
    public void cambiarEstadoCita(String citaId, String propietarioUsuarioId, String nuevoEstado) {
        ObjectId cid = Ids.oid(citaId);
        ObjectId pid = Ids.oid(propietarioUsuarioId);
        if (cid == null || pid == null) {
            throw new IllegalArgumentException("La cita no existe.");
        }
        CitaDoc cita = mongo.findById(cid, CitaDoc.class);
        // La cita tiene que ser de un taller de este dueno
        if (cita == null || !mongo.exists(Query.query(Criteria.where("_id").is(cita.tallerId)
                .and("propietarioId").is(pid)), TallerDoc.class)) {
            throw new IllegalArgumentException("La cita no existe.");
        }
        String actual = cita.estado == null ? "solicitada" : cita.estado;
        String nuevo = nuevoEstado == null ? "" : nuevoEstado.trim().toLowerCase(Locale.ROOT);
        boolean permitido = switch (actual) {
            case "solicitada" -> nuevo.equals("confirmada") || nuevo.equals("cancelada");
            case "confirmada" -> nuevo.equals("completada") || nuevo.equals("cancelada");
            default -> false;
        };
        if (!permitido) {
            throw new IllegalArgumentException("Esta cita ya no admite ese cambio.");
        }
        if (nuevo.equals("completada") && cita.fechaHora != null && cita.fechaHora.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Solo puedes completar una cita cuando ya llegó su hora.");
        }
        Update cambios = new Update().set("estado", nuevo);
        if (nuevo.equals("cancelada")) {
            // Al cancelar, el turno del mecanico queda libre para otros clientes
            cambios.set("ocupaTurno", false);
        }
        // Se exige que siga en el estado leido: si otro cambio llego primero, no se pisa
        var r = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(cid).and("estado").is(actual)), cambios, CitaDoc.class);
        if (r.getMatchedCount() == 0) {
            throw new IllegalArgumentException("La cita cambió mientras la revisabas. Actualiza e intenta de nuevo.");
        }
    }

    @Override
    public Cita citaDelCliente(String citaId, String clienteUsuarioId) {
        CitaDoc d = buscarDelCliente(citaId, clienteUsuarioId);
        return d == null ? null : Mapeos.cita(d, zona);
    }

    @Override
    public void editarCita(String citaId, String clienteUsuarioId, String cliente, String motivo,
                           String fecha, String hora) {
        CitaDoc actual = buscarDelCliente(citaId, clienteUsuarioId);
        if (actual == null) {
            throw new IllegalArgumentException("La cita no existe.");
        }
        if (!Mapeos.editable(actual)) {
            throw new IllegalArgumentException("Esta cita ya no se puede modificar.");
        }
        String detalle = motivo == null ? "" : motivo.trim();
        if (detalle.isEmpty()) {
            throw new IllegalArgumentException("Escribe el motivo de la cita.");
        }
        if (detalle.length() > 300) {
            throw new IllegalArgumentException("El motivo es demasiado largo (máximo 300 caracteres).");
        }
        String nombre = cliente == null || cliente.isBlank() ? actual.cliente : cliente.trim();
        if (nombre != null && nombre.length() > 120) {
            throw new IllegalArgumentException("El nombre de contacto es demasiado largo (máximo 120 caracteres).");
        }
        LocalDateTime inicio = leerInicio(fecha, hora);
        Instant nuevaFecha = inicio.atZone(zona).toInstant();

        // Solo se actualiza si la cita sigue siendo de este cliente y sigue ocupando turno
        Query propia = Query.query(Criteria.where("_id").is(actual.id)
                .and("clienteId").is(actual.clienteId)
                .and("ocupaTurno").is(true));

        // Mismo turno: solo cambian el motivo y el nombre, el mecanico y el estado se quedan igual
        if (nuevaFecha.equals(actual.fechaHora)) {
            var r = mongo.updateFirst(propia, new Update().set("motivo", detalle).set("cliente", nombre), CitaDoc.class);
            if (r.getMatchedCount() == 0) {
                throw new IllegalArgumentException("Esta cita ya no se puede modificar.");
            }
            return;
        }

        // Otro turno: mismas validaciones que al pedir la cita
        TallerDoc taller = mongo.findById(actual.tallerId, TallerDoc.class);
        if (taller == null) {
            throw new IllegalArgumentException("El taller de esta cita ya no existe.");
        }
        HorarioAtencion.validarTurno(Mapeos.horarios(taller), inicio);
        if (!inicio.isAfter(LocalDateTime.now(zona))) {
            throw new IllegalArgumentException("La fecha y hora de la cita ya pasaron.");
        }
        List<TallerDoc.MecanicoDoc> activos = taller.mecanicos.stream().filter(m -> m.activo).toList();
        if (activos.isEmpty()) {
            throw new IllegalArgumentException("Este taller aún no tiene mecánicos disponibles.");
        }
        // Igual que al crear: el indice unico decide quien gana si dos personas piden el mismo turno
        for (int intento = 0; intento <= activos.size(); intento++) {
            Set<ObjectId> ocupados = new HashSet<>();
            for (CitaDoc c : mongo.find(Query.query(Criteria.where("tallerId").is(actual.tallerId)
                    .and("fechaHora").is(nuevaFecha)
                    .and("ocupaTurno").is(true)), CitaDoc.class)) {
                ocupados.add(c.mecanicoId);
            }
            TallerDoc.MecanicoDoc libre = null;
            // Prefiero conservar el mismo mecanico si esta libre en el turno nuevo
            for (TallerDoc.MecanicoDoc m : activos) {
                if (!ocupados.contains(m.id) && m.id.equals(actual.mecanicoId)) {
                    libre = m;
                    break;
                }
            }
            if (libre == null) {
                for (TallerDoc.MecanicoDoc m : activos) {
                    if (!ocupados.contains(m.id)) {
                        libre = m;
                        break;
                    }
                }
            }
            if (libre == null) {
                throw new IllegalArgumentException("Esa hora ya no está disponible. Elige otra.");
            }
            Update cambios = new Update()
                    .set("fechaHora", nuevaFecha)
                    .set("mecanicoId", libre.id)
                    .set("mecanicoNombre", (libre.nombres + " " + libre.apellidos).trim())
                    .set("motivo", detalle)
                    .set("cliente", nombre)
                    // Cambio de turno: el taller la tiene que revisar de nuevo
                    .set("estado", "solicitada");
            try {
                var r = mongo.updateFirst(propia, cambios, CitaDoc.class);
                if (r.getMatchedCount() == 0) {
                    throw new IllegalArgumentException("Esta cita ya no se puede modificar.");
                }
                return;
            } catch (DuplicateKeyException ex) {
                // Otra persona tomo a ese mecanico justo ahora: releo y pruebo con otro
            }
        }
        throw new IllegalArgumentException("Esa hora ya no está disponible. Elige otra.");
    }

    @Override
    public void cancelarCita(String citaId, String clienteUsuarioId) {
        CitaDoc actual = buscarDelCliente(citaId, clienteUsuarioId);
        if (actual == null) {
            throw new IllegalArgumentException("La cita no existe.");
        }
        if (!Mapeos.editable(actual)) {
            throw new IllegalArgumentException("Esta cita ya no se puede cancelar.");
        }
        // ocupaTurno en false saca la cita del indice unico: el turno queda libre para otros clientes
        var r = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(actual.id)
                        .and("clienteId").is(actual.clienteId)
                        .and("ocupaTurno").is(true)),
                new Update().set("estado", "cancelada").set("ocupaTurno", false),
                CitaDoc.class);
        if (r.getMatchedCount() == 0) {
            throw new IllegalArgumentException("Esta cita ya no se puede cancelar.");
        }
    }

    // Busca por id Y por dueno: un cliente nunca puede tocar la cita de otro
    private CitaDoc buscarDelCliente(String citaId, String clienteUsuarioId) {
        ObjectId cid = Ids.oid(citaId);
        ObjectId uid = Ids.oid(clienteUsuarioId);
        if (cid == null || uid == null) {
            return null;
        }
        return mongo.findOne(Query.query(Criteria.where("_id").is(cid).and("clienteId").is(uid)), CitaDoc.class);
    }

    private static LocalDateTime leerInicio(String fecha, String hora) {
        try {
            return LocalDateTime.of(
                    LocalDate.parse(fecha == null ? "" : fecha.trim()),
                    LocalTime.parse(hora == null ? "" : hora.trim()));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Elige una fecha y una hora válidas para la cita.");
        }
    }
}
