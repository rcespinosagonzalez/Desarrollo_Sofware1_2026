package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.BusquedaDoc;
import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.HorarioDia;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.repositories.UsuarioRepositorio;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.util.GeocodificacionOpenStreetMap;
import com.tecnologico.talleres.util.HorarioAtencion;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

// Implementacion con MongoTemplate: consultas con Criteria/Query/Update y mapeo a los modelos de las vistas
@Service
public class TallerServiceMongoImpl implements TallerService {

    private static final String ORIGEN_HISTORIAL_BUSCADOR = "buscador";
    private static final int LIMITE_HISTORIAL = 30;
    private static final int LIMITE_ADMIN_VALORACIONES = 100;
    private static final int LIMITE_ADMIN_CITAS = 500;
    private static final Sort POR_ID_DESC = Sort.by(Sort.Direction.DESC, "_id");

    private final MongoTemplate mongo;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepositorio usuarios;
    private final ZoneId zona;

    public TallerServiceMongoImpl(MongoTemplate mongo, PasswordEncoder passwordEncoder, UsuarioRepositorio usuarios,
                                  @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.mongo = mongo;
        this.passwordEncoder = passwordEncoder;
        this.usuarios = usuarios;
        this.zona = ZoneId.of(zona);
    }

    @Override
    public List<Taller> listarTodos() {
        return aModelos(mongo.find(new Query().with(POR_ID_DESC), TallerDoc.class));
    }

    @Override
    public List<Taller> listarTalleresDelPropietario(String propietarioUsuarioId) {
        ObjectId pid = Ids.oid(propietarioUsuarioId);
        if (pid == null) {
            return List.of();
        }
        return aModelos(mongo.find(
                Query.query(Criteria.where("propietarioId").is(pid)).with(POR_ID_DESC), TallerDoc.class));
    }

    @Override
    public Taller obtenerPorId(String id) {
        ObjectId oid = Ids.oid(id);
        if (oid == null) {
            return null;
        }
        TallerDoc d = mongo.findById(oid, TallerDoc.class);
        if (d == null) {
            return null;
        }
        return Mapeos.taller(d, usuarios.porIds(Set.of(d.propietarioId)).get(d.propietarioId));
    }

    // Convierte la lista y trae los duenos de todos los talleres con una sola consulta
    private List<Taller> aModelos(List<TallerDoc> docs) {
        Set<ObjectId> ids = new HashSet<>();
        for (TallerDoc d : docs) {
            if (d.propietarioId != null) {
                ids.add(d.propietarioId);
            }
        }
        Map<ObjectId, UsuarioDoc> duenos = usuarios.porIds(ids);
        List<Taller> lista = new ArrayList<>(docs.size());
        for (TallerDoc d : docs) {
            lista.add(Mapeos.taller(d, duenos.get(d.propietarioId)));
        }
        return lista;
    }

    // Alta: opcional creo usuario dueno (rol taller). Update: valido que sea mi taller
    @Override
    public void guardar(Taller taller, String propietarioSesion, boolean esAdministrador) {
        if (taller == null) {
            return;
        }
        ObjectId id = Ids.oid(taller.getId());
        TallerDoc existente = id == null ? null : mongo.findById(id, TallerDoc.class);

        completarCoordenadas(taller);
        // El horario es obligatorio: sin el no se pueden agendar citas
        List<HorarioDia> nuevoHorario = HorarioAtencion.desdeFormulario(
                taller.getDiasAtencion(), taller.getHoraApertura(), taller.getHoraCierre());

        if (existente != null) {
            ObjectId sesion = Ids.oid(propietarioSesion);
            if (!esAdministrador && (existente.propietarioId == null || !Objects.equals(existente.propietarioId, sesion))) {
                throw new IllegalArgumentException("No puedes editar un taller que no es tuyo.");
            }
            mongo.updateFirst(
                    Query.query(Criteria.where("_id").is(existente.id)),
                    new Update()
                            .set("nombre", taller.getNombre())
                            .set("telefono", taller.getTelefono())
                            .set("descripcion", taller.getDescripcion())
                            .set("servicios", taller.getServicios())
                            .set("ubicacion", ubicacion(taller))
                            .set("horarios", aDocHorarios(nuevoHorario))
                            .set("actualizadoEn", Instant.now()),
                    TallerDoc.class);
            return;
        }

        ObjectId propietarioTaller = Ids.oid(propietarioSesion);
        UsuarioDoc duenoNuevo = null;
        String em = taller.getEmailCliente();
        String cl = taller.getClaveCliente();
        if (em != null && !em.isBlank() && cl != null && !cl.isBlank()) {
            em = em.trim().toLowerCase();
            if (cl.length() < 8) {
                throw new IllegalArgumentException("La contraseña del dueño debe tener al menos 8 caracteres.");
            }
            String nomDueno = taller.getNombreCliente();
            if (nomDueno == null || nomDueno.isBlank()) {
                nomDueno = taller.getNombre() != null ? taller.getNombre() : "Dueño del taller";
            }
            try {
                duenoNuevo = usuarios.crear(em, passwordEncoder.encode(cl), nomDueno, "taller");
            } catch (DuplicateKeyException ex) {
                throw new IllegalArgumentException("Ese correo ya está registrado; usa otro.");
            }
            propietarioTaller = duenoNuevo.id;
        }

        TallerDoc d = new TallerDoc();
        d.id = new ObjectId();
        d.propietarioId = propietarioTaller;
        d.nombre = taller.getNombre();
        d.telefono = taller.getTelefono();
        d.descripcion = taller.getDescripcion();
        d.estado = "publicado";
        d.servicios = taller.getServicios() == null || taller.getServicios().isBlank()
                ? "Servicios generales" : taller.getServicios();
        d.ubicacion = ubicacion(taller);
        d.horarios = aDocHorarios(nuevoHorario);
        // Todo taller nuevo arranca con un mecanico para poder recibir citas; el dueño agrega mas desde su panel
        TallerDoc.MecanicoDoc m = new TallerDoc.MecanicoDoc();
        m.id = new ObjectId();
        m.nombres = "Mecánico";
        m.apellidos = "Principal";
        m.activo = true;
        d.mecanicos = new ArrayList<>(List.of(m));
        d.creadoEn = Instant.now();
        d.actualizadoEn = d.creadoEn;
        try {
            mongo.insert(d);
        } catch (RuntimeException ex) {
            // Sin transacciones: si falla el taller, no dejo el usuario dueño huerfano
            if (duenoNuevo != null) {
                usuarios.borrar(duenoNuevo.id);
            }
            throw ex;
        }
        taller.setId(Ids.texto(d.id));
        taller.setCalificacionPromedio(0);
        taller.setNumValoraciones(0);
    }

    // Sin coordenadas llamo Nominatim; si no encuentra la direccion rechazo (no invento una ubicacion)
    private static void completarCoordenadas(Taller taller) {
        if (taller.getLat() != null && taller.getLng() != null) {
            if (Math.abs(taller.getLat()) > 90 || Math.abs(taller.getLng()) > 180) {
                throw new IllegalArgumentException("Las coordenadas del taller no son válidas.");
            }
            return;
        }
        double[] coords = GeocodificacionOpenStreetMap.buscar(taller.getDireccion(), taller.getCiudad());
        if (coords == null) {
            throw new IllegalArgumentException(
                    "No pudimos ubicar esa dirección en el mapa. Escríbela con más detalle (calle, número y ciudad).");
        }
        taller.setLat(coords[0]);
        taller.setLng(coords[1]);
    }

    private static TallerDoc.UbicacionDoc ubicacion(Taller taller) {
        TallerDoc.UbicacionDoc u = new TallerDoc.UbicacionDoc();
        u.direccion = taller.getDireccion() != null ? taller.getDireccion() : "";
        u.ciudad = taller.getCiudad() == null || taller.getCiudad().isBlank() ? "Cartagena" : taller.getCiudad().trim();
        u.departamento = "Bolivar";
        u.pais = "CO";
        // GeoJSON: primero longitud y despues latitud
        u.punto = new GeoJsonPoint(taller.getLng(), taller.getLat());
        return u;
    }

    private static List<TallerDoc.HorarioDoc> aDocHorarios(List<HorarioDia> horarios) {
        List<TallerDoc.HorarioDoc> lista = new ArrayList<>();
        for (HorarioDia h : horarios) {
            TallerDoc.HorarioDoc d = new TallerDoc.HorarioDoc();
            d.dia = h.dia();
            d.apertura = h.apertura();
            d.cierre = h.cierre();
            lista.add(d);
        }
        return lista;
    }

    @Override
    public List<Valoracion> listarValoracionesPorTaller(String tallerId) {
        ObjectId tid = Ids.oid(tallerId);
        if (tid == null) {
            return List.of();
        }
        return aValoraciones(mongo.find(
                Query.query(Criteria.where("tallerId").is(tid)).with(Sort.by(Sort.Direction.DESC, "creadoEn")),
                ValoracionDoc.class));
    }

    @Override
    public List<Valoracion> listarTodasValoraciones() {
        return aValoraciones(mongo.find(
                new Query().with(Sort.by(Sort.Direction.DESC, "creadoEn")).limit(LIMITE_ADMIN_VALORACIONES),
                ValoracionDoc.class));
    }

    @Override
    public List<Valoracion> listarValoracionesPorPropietario(String propietarioUsuarioId) {
        List<ObjectId> talleres = idsTalleresDe(propietarioUsuarioId);
        if (talleres.isEmpty()) {
            return List.of();
        }
        return aValoraciones(mongo.find(
                Query.query(Criteria.where("tallerId").in(talleres)).with(Sort.by(Sort.Direction.DESC, "creadoEn")),
                ValoracionDoc.class));
    }

    private List<Valoracion> aValoraciones(List<ValoracionDoc> docs) {
        List<Valoracion> lista = new ArrayList<>(docs.size());
        for (ValoracionDoc d : docs) {
            lista.add(Mapeos.valoracion(d, zona));
        }
        return lista;
    }

    // Ids de los talleres de un dueno (solo el _id, no el documento completo)
    private List<ObjectId> idsTalleresDe(String propietarioUsuarioId) {
        ObjectId pid = Ids.oid(propietarioUsuarioId);
        if (pid == null) {
            return List.of();
        }
        Query q = Query.query(Criteria.where("propietarioId").is(pid));
        q.fields().include("_id");
        List<ObjectId> ids = new ArrayList<>();
        for (TallerDoc t : mongo.find(q, TallerDoc.class)) {
            ids.add(t.id);
        }
        return ids;
    }

    @Override
    public List<Cita> listarCitasPorPropietario(String propietarioUsuarioId) {
        List<ObjectId> talleres = idsTalleresDe(propietarioUsuarioId);
        if (talleres.isEmpty()) {
            return List.of();
        }
        return aCitas(mongo.find(
                Query.query(Criteria.where("tallerId").in(talleres)).with(Sort.by(Sort.Direction.DESC, "fechaHora")),
                CitaDoc.class));
    }

    @Override
    public List<Cita> listarTodasCitas() {
        return aCitas(mongo.find(
                new Query().with(Sort.by(Sort.Direction.DESC, "fechaHora")).limit(LIMITE_ADMIN_CITAS),
                CitaDoc.class));
    }

    @Override
    public List<Cita> listarCitasPorCliente(String clienteUsuarioId) {
        ObjectId cid = Ids.oid(clienteUsuarioId);
        if (cid == null) {
            return List.of();
        }
        return aCitas(mongo.find(
                Query.query(Criteria.where("clienteId").is(cid)).with(Sort.by(Sort.Direction.DESC, "fechaHora")),
                CitaDoc.class));
    }

    private List<Cita> aCitas(List<CitaDoc> docs) {
        List<Cita> lista = new ArrayList<>(docs.size());
        for (CitaDoc d : docs) {
            lista.add(Mapeos.cita(d, zona));
        }
        return lista;
    }

    @Override
    public List<BusquedaHistorial> listarHistorialUsuario(String usuarioId) {
        ObjectId uid = Ids.oid(usuarioId);
        if (uid == null) {
            return List.of();
        }
        List<BusquedaHistorial> lista = new ArrayList<>();
        for (BusquedaDoc d : mongo.find(
                Query.query(Criteria.where("usuarioId").is(uid)
                                .orOperator(Criteria.where("origen").is(null),
                                        Criteria.where("origen").is(ORIGEN_HISTORIAL_BUSCADOR)))
                        .with(Sort.by(Sort.Direction.DESC, "creadoEn"))
                        .limit(LIMITE_HISTORIAL),
                BusquedaDoc.class)) {
            lista.add(Mapeos.busqueda(d, zona));
        }
        return lista;
    }

    @Override
    public void registrarHistorialBusqueda(String usuarioId, String termino, Double origenLat, Double origenLng,
                                           Integer radioM, Integer resultados) {
        ObjectId uid = Ids.oid(usuarioId);
        if (uid == null || termino == null || termino.isBlank()) {
            return;
        }
        String limpio = termino.trim();
        // Evito duplicar la misma busqueda en menos de 1 minuto
        boolean repetido = mongo.exists(
                Query.query(Criteria.where("usuarioId").is(uid)
                        .and("termino").is(limpio)
                        .and("creadoEn").gt(Instant.now().minusSeconds(60))),
                BusquedaDoc.class);
        if (repetido) {
            return;
        }
        BusquedaDoc d = new BusquedaDoc();
        d.id = new ObjectId();
        d.usuarioId = uid;
        d.termino = limpio;
        d.origenLat = origenLat;
        d.origenLng = origenLng;
        d.radioM = radioM;
        d.resultados = resultados;
        d.origen = ORIGEN_HISTORIAL_BUSCADOR;
        d.creadoEn = Instant.now();
        mongo.insert(d);
    }

    @Override
    public UltimaCalificacion obtenerUltimaCalificacion(String usuarioId) {
        ObjectId uid = Ids.oid(usuarioId);
        ValoracionDoc v = uid == null ? null : mongo.findOne(
                Query.query(Criteria.where("clienteId").is(uid)).with(Sort.by(Sort.Direction.DESC, "creadoEn")),
                ValoracionDoc.class);
        if (v == null) {
            return new UltimaCalificacion("—", "—", 0, "Sin valoraciones.");
        }
        String servicio = v.servicio != null && !v.servicio.isBlank() ? v.servicio : "Valoración";
        return new UltimaCalificacion(v.tallerNombre, servicio, v.estrellas, v.comentario != null ? v.comentario : "");
    }

    @Override
    public void registrarCalificacion(UltimaCalificacion calificacion, String usuarioId) {
        if (calificacion == null) {
            throw new IllegalArgumentException("Faltan los datos de la valoración.");
        }
        TallerDoc taller = resolverTallerPorNombre(calificacion.getTallerNombre());
        if (taller == null) {
            throw new IllegalArgumentException("No encontramos un taller con ese nombre.");
        }
        int estrellas = calificacion.getEstrellas();
        if (estrellas < 1 || estrellas > 5) {
            throw new IllegalArgumentException("Las estrellas deben ser de 1 a 5.");
        }
        UsuarioDoc cliente = usuarios.buscarPorId(usuarioId);
        if (cliente == null) {
            throw new IllegalArgumentException("Inicia sesión para valorar un taller.");
        }
        ValoracionDoc v = new ValoracionDoc();
        v.id = new ObjectId();
        v.clienteId = cliente.id;
        v.clienteNombre = cliente.nombre;
        v.tallerId = taller.id;
        v.tallerNombre = taller.nombre;
        v.estrellas = estrellas;
        String servicio = calificacion.getServicio() == null ? "" : calificacion.getServicio().trim();
        v.servicio = servicio.isEmpty() ? null : servicio;
        v.comentario = calificacion.getComentario() == null ? "" : calificacion.getComentario().trim();
        v.creadoEn = Instant.now();
        mongo.insert(v);
        // $inc es atomico: el promedio del taller no se descuadra aunque varias personas valoren a la vez
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(taller.id)),
                new Update().inc("resumen.suma", estrellas).inc("resumen.total", 1),
                TallerDoc.class);
    }

    // Al valorar escribo el nombre del taller; busco el exacto (sin mayusculas) o uno parecido
    private TallerDoc resolverTallerPorNombre(String nombreTaller) {
        if (nombreTaller == null || nombreTaller.isBlank()) {
            return null;
        }
        String limpio = nombreTaller.trim();
        TallerDoc exacto = mongo.findOne(
                Query.query(Criteria.where("nombre").regex("^" + Pattern.quote(limpio) + "$", "i")), TallerDoc.class);
        if (exacto != null) {
            return exacto;
        }
        return mongo.findOne(
                Query.query(Criteria.where("nombre").regex(Pattern.quote(limpio), "i")), TallerDoc.class);
    }

    @Override
    public void destacar(String tallerId, int dias) {
        ObjectId id = Ids.oid(tallerId);
        TallerDoc t = id == null ? null : mongo.findById(id, TallerDoc.class);
        if (t == null) {
            throw new IllegalArgumentException("El taller no existe.");
        }
        if (dias < 1 || dias > 365) {
            throw new IllegalArgumentException("Elige entre 1 y 365 días.");
        }
        // Si ya esta destacado, los dias se suman al vencimiento actual; si no, cuentan desde hoy
        Instant base = t.destacadoHasta != null && t.destacadoHasta.isAfter(Instant.now())
                ? t.destacadoHasta : Instant.now();
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update().set("destacadoHasta", base.plus(dias, java.time.temporal.ChronoUnit.DAYS)),
                TallerDoc.class);
    }

    @Override
    public void quitarDestacado(String tallerId) {
        ObjectId id = Ids.oid(tallerId);
        if (id == null) {
            throw new IllegalArgumentException("El taller no existe.");
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update().unset("destacadoHasta"), TallerDoc.class);
    }
}
