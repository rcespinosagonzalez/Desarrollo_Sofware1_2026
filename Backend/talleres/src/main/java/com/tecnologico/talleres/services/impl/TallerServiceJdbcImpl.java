package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.util.GeocodificacionOpenStreetMap;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

// Implementacion JDBC: consultas SQL directas y mapeo manual a modelos
@Service
public class TallerServiceJdbcImpl implements TallerService {

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ORIGEN_HISTORIAL_BUSCADOR = "buscador";

    // Una ubicacion por taller (la de menor id si hay varias)
    private static final String SQL_JOIN_UBICACION = """
            LEFT JOIN ubicacion_taller ubi ON ubi.taller_id = t.id
              AND ubi.id = (SELECT MIN(u2.id) FROM ubicacion_taller u2 WHERE u2.taller_id = t.id)
            """;

    // Promedio y cantidad de estrellas por taller
    private static final String SQL_JOIN_VALORACIONES = """
            LEFT JOIN (
                SELECT taller_id, AVG(estrellas) AS prom, COUNT(*) AS cnt
                FROM valoracion GROUP BY taller_id
            ) va ON va.taller_id = t.id
            """;

    private final JdbcTemplate jdbc;

    // Constructor: Spring pasa el bean JdbcTemplate
    public TallerServiceJdbcImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // IDs manuales: MAX(id)+1 porque en demo no siempre uso AUTO_INCREMENT en inserts
    private long siguienteIdEn(String tabla) {
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + tabla, Long.class);
        return max + 1;
    }

    @Override
    // SELECT con JOIN ubicacion, usuario dueno y promedio valoraciones
    public List<Taller> listarTodos() {
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto, ubi.ciudad, ubi.lat, ubi.lng,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                """ + SQL_JOIN_UBICACION + """
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                """ + SQL_JOIN_VALORACIONES + """
                ORDER BY t.id DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs));
    }

    // Paso de ResultSet a objeto Taller para el mapa y las vistas
    private static Taller mapearFilaTaller(java.sql.ResultSet rs) throws java.sql.SQLException {
        Taller t = new Taller();
        t.setId(rs.getLong("id"));
        t.setNombre(rs.getString("nombre"));
        t.setTelefono(rs.getString("telefono"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setHorario(rs.getString("horario"));
        String serv = rs.getString("servicios_texto");
        t.setServicios(serv != null ? serv : "");
        t.setDireccion(rs.getString("direccion_texto"));
        t.setCiudad(rs.getString("ciudad"));
        var lat = rs.getBigDecimal("lat");
        var lng = rs.getBigDecimal("lng");
        if (lat != null) {
            t.setLat(lat.doubleValue());
        }
        if (lng != null) {
            t.setLng(lng.doubleValue());
        }
        t.setCalificacionPromedio(rs.getDouble("cal_prom"));
        t.setNumValoraciones(rs.getInt("num_val"));
        t.setEmailPropietario(rs.getString("email_propietario"));
        t.setNombrePropietario(rs.getString("nombre_propietario"));
        return t;
    }

    @Override
    // Igual que listarTodos pero WHERE propietario_usuario_id = ?
    public List<Taller> listarTalleresDelPropietario(long propietarioUsuarioId) {
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto, ubi.ciudad, ubi.lat, ubi.lng,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                """ + SQL_JOIN_UBICACION + """
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                """ + SQL_JOIN_VALORACIONES + """
                WHERE t.propietario_usuario_id = ?
                ORDER BY t.id DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs), propietarioUsuarioId);
    }

    @Override
    // Un solo taller por id
    public Taller obtenerPorId(Long id) {
        if (id == null) {
            return null;
        }
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto, ubi.ciudad, ubi.lat, ubi.lng,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                """ + SQL_JOIN_UBICACION + """
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                """ + SQL_JOIN_VALORACIONES + """
                WHERE t.id = ?
                """;
        List<Taller> lista = jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs), id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    // Ultimo id insertado y luego obtenerPorId
    public Taller obtenerPrincipal() {
        String sql = "SELECT t.id FROM taller t ORDER BY t.id DESC LIMIT 1";
        List<Long> ids = jdbc.query(sql, (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            return null;
        }
        return obtenerPorId(ids.get(0));
    }

    // Alta: opcional creo usuario dueno (rol taller). Update: valido que sea mi taller
    @Override
    public void guardar(Taller taller, long propietarioSesion, boolean esAdministrador) {
        if (taller == null) {
            return;
        }
        Long id = taller.getId();
        if (id != null && id <= 0) {
            taller.setId(null);
            id = null;
        }
        Integer cuenta = id == null ? 0 : jdbc.queryForObject("SELECT COUNT(*) FROM taller WHERE id = ?", Integer.class, id);
        boolean existe = cuenta != null && cuenta > 0;

        completarCoordenadas(taller);

        if (existe) {
            Long dueno = jdbc.queryForObject(
                    "SELECT propietario_usuario_id FROM taller WHERE id = ?", Long.class, id);
            if (!esAdministrador && (dueno == null || !Objects.equals(dueno, propietarioSesion))) {
                throw new IllegalArgumentException("No puedes editar un taller que no es tuyo.");
            }
            jdbc.update(
                    """
                            UPDATE taller SET nombre=?, telefono=?, descripcion=?, horario=?, servicios_texto=?, updated_at=NOW()
                            WHERE id=?
                            """,
                    taller.getNombre(),
                    taller.getTelefono(),
                    taller.getDescripcion(),
                    taller.getHorario(),
                    taller.getServicios(),
                    id);
            guardarUbicacion(id, taller);
        } else {
            long propietarioTaller = propietarioSesion;
            String em = taller.getEmailCliente();
            String cl = taller.getClaveCliente();
            if (em != null && !em.isBlank() && cl != null && !cl.isBlank()) {
                em = em.trim();
                Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, em);
                if (dup != null && dup > 0) {
                    throw new IllegalArgumentException("Ese correo ya está registrado; usa otro.");
                }
                long nuevoUsuarioId = siguienteIdEn("usuario");
                String nomDueno = taller.getNombreCliente();
                if (nomDueno == null || nomDueno.isBlank()) {
                    nomDueno = taller.getNombre() != null ? taller.getNombre() : "Dueño del taller";
                }
                jdbc.update(
                        """
                                INSERT INTO usuario (id, email, password_hash, nombre, rol, activo, created_at, updated_at)
                                VALUES (?, ?, ?, ?, 'taller', TRUE, NOW(), NOW())
                                """,
                        nuevoUsuarioId,
                        em,
                        cl,
                        nomDueno);
                propietarioTaller = nuevoUsuarioId;
            }

            long nuevoId = siguienteIdEn("taller");
            taller.setId(nuevoId);
            String serv = taller.getServicios();
            if (serv == null || serv.isBlank()) {
                serv = "Servicios generales";
            }
            jdbc.update(
                    """
                            INSERT INTO taller (id, propietario_usuario_id, nombre, telefono, descripcion, estado, horario, servicios_texto, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, 'publicado', ?, ?, NOW(), NOW())
                            """,
                    nuevoId,
                    propietarioTaller,
                    taller.getNombre(),
                    taller.getTelefono(),
                    taller.getDescripcion(),
                    taller.getHorario(),
                    serv);
            guardarUbicacion(nuevoId, taller);
            taller.setCalificacionPromedio(0);
            taller.setNumValoraciones(0);
        }
    }

    // Sin coordenadas llamo Nominatim; si falla pongo Cartagena por defecto
    private static void completarCoordenadas(Taller taller) {
        if (taller.getLat() != null && taller.getLng() != null) {
            return;
        }
        double[] coords = GeocodificacionOpenStreetMap.buscar(taller.getDireccion(), taller.getCiudad());
        if (coords != null) {
            taller.setLat(coords[0]);
            taller.setLng(coords[1]);
        } else {
            taller.setLat(10.40);
            taller.setLng(-75.52);
        }
    }

    // UPDATE en ubicacion_taller; si no hay fila hago INSERT
    private void guardarUbicacion(long tallerId, Taller taller) {
        String ciudad = textoCiudad(taller.getCiudad());
        String direccion = taller.getDireccion() != null ? taller.getDireccion() : "";
        int n = jdbc.update(
                """
                        UPDATE ubicacion_taller
                        SET direccion_texto=?, ciudad=?, departamento='Bolivar', pais='CO', lat=?, lng=?
                        WHERE taller_id=?
                        """,
                direccion,
                ciudad,
                taller.getLat(),
                taller.getLng(),
                tallerId);
        if (n == 0) {
            long ubiId = siguienteIdEn("ubicacion_taller");
            jdbc.update(
                    """
                            INSERT INTO ubicacion_taller (id, taller_id, direccion_texto, ciudad, departamento, pais, lat, lng)
                            VALUES (?, ?, ?, ?, 'Bolivar', 'CO', ?, ?)
                            """,
                    ubiId,
                    tallerId,
                    direccion,
                    ciudad,
                    taller.getLat(),
                    taller.getLng());
        }
    }

    // Si viene vacio uso Cartagena
    private static String textoCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return "Cartagena";
        }
        return ciudad.trim();
    }

    @Override
    // Valoraciones de un taller con nombre del cliente
    public List<Valoracion> listarValoracionesPorTaller(Long tallerId) {
        if (tallerId == null) {
            return List.of();
        }
        String sql = """
                SELECT v.id, v.usuario_id, v.taller_id, v.cita_id, v.estrellas, v.comentario, v.created_at,
                       u.nombre AS cliente
                FROM valoracion v
                JOIN usuario u ON u.id = v.usuario_id
                WHERE v.taller_id = ?
                ORDER BY v.created_at DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearValoracion(rs), tallerId);
    }

    // ResultSet -> Valoracion
    private static Valoracion mapearValoracion(java.sql.ResultSet rs) throws java.sql.SQLException {
        Valoracion v = new Valoracion();
        v.setId(rs.getLong("id"));
        v.setUsuarioId(rs.getLong("usuario_id"));
        v.setTallerId(rs.getLong("taller_id"));
        long citaId = rs.getLong("cita_id");
        if (!rs.wasNull()) {
            v.setCitaId(citaId);
        }
        v.setEstrellas(rs.getInt("estrellas"));
        v.setComentario(rs.getString("comentario"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            v.setCreatedAt(ts.toLocalDateTime());
            v.setFecha(ts.toLocalDateTime().toLocalDate().toString());
        }
        try {
            v.setCliente(rs.getString("cliente"));
        } catch (java.sql.SQLException ignored) {
            // columna opcional en otras consultas
        }
        return v;
    }

    @Override
    // Panel admin: hasta 100 valoraciones con nombre taller
    public List<Valoracion> listarTodasValoraciones() {
        String sql = """
                SELECT v.id, v.usuario_id, v.taller_id, v.cita_id, v.estrellas, v.comentario, v.created_at,
                       u.nombre AS cliente, t.nombre AS taller_nombre
                FROM valoracion v
                JOIN usuario u ON u.id = v.usuario_id
                JOIN taller t ON t.id = v.taller_id
                ORDER BY v.created_at DESC
                LIMIT 100
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Valoracion v = mapearValoracion(rs);
            v.setCliente(rs.getString("cliente"));
            v.setTallerNombre(rs.getString("taller_nombre"));
            return v;
        });
    }

    @Override
    // Valoraciones de todos los talleres de un dueno
    public List<Valoracion> listarValoracionesPorPropietario(long propietarioUsuarioId) {
        String sql = """
                SELECT v.id, v.usuario_id, v.taller_id, v.cita_id, v.estrellas, v.comentario, v.created_at,
                       u.nombre AS cliente, t.nombre AS taller_nombre
                FROM valoracion v
                JOIN usuario u ON u.id = v.usuario_id
                JOIN taller t ON t.id = v.taller_id
                WHERE t.propietario_usuario_id = ?
                ORDER BY v.created_at DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Valoracion v = mapearValoracion(rs);
            v.setCliente(rs.getString("cliente"));
            v.setTallerNombre(rs.getString("taller_nombre"));
            return v;
        }, propietarioUsuarioId);
    }

    @Override
    // Citas de talleres donde propietario_usuario_id coincide
    public List<Cita> listarCitasPorPropietario(long propietarioUsuarioId) {
        String sql = """
                SELECT c.id, c.notas_usuario, c.fecha_hora, c.taller_id, t.nombre AS taller_nombre
                FROM cita c
                JOIN taller t ON t.id = c.taller_id
                WHERE t.propietario_usuario_id = ?
                ORDER BY c.fecha_hora DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearCita(rs), propietarioUsuarioId);
    }

    @Override
    // Todas las citas para mi-taller admin
    public List<Cita> listarTodasCitas() {
        String sql = """
                SELECT c.id, c.notas_usuario, c.fecha_hora, c.taller_id, t.nombre AS taller_nombre
                FROM cita c
                JOIN taller t ON t.id = c.taller_id
                ORDER BY c.fecha_hora DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearCita(rs));
    }

    @Override
    // Citas donde usuario_id es el cliente
    public List<Cita> listarCitasPorCliente(long clienteUsuarioId) {
        String sql = """
                SELECT c.id, c.notas_usuario, c.fecha_hora, c.taller_id, t.nombre AS taller_nombre
                FROM cita c
                JOIN taller t ON t.id = c.taller_id
                WHERE c.usuario_id = ?
                ORDER BY c.fecha_hora DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearCita(rs), clienteUsuarioId);
    }

    // ResultSet -> Cita; separo Cliente: y Detalle: de notas_usuario
    private static Cita mapearCita(java.sql.ResultSet rs) throws java.sql.SQLException {
        Cita c = new Cita();
        c.setId(rs.getLong("id"));
        try {
            c.setUsuarioId(rs.getLong("usuario_id"));
        } catch (java.sql.SQLException ignored) {
            // algunas consultas no traen usuario_id
        }
        c.setTallerId(rs.getLong("taller_id"));
        try {
            long sid = rs.getLong("servicio_id");
            if (!rs.wasNull()) {
                c.setServicioId(sid);
            }
            c.setEstado(rs.getString("estado"));
        } catch (java.sql.SQLException ignored) {
            // columnas opcionales
        }
        c.setTallerNombre(rs.getString("taller_nombre"));
        // Cliente y detalle van embebidos en notas_usuario (formato del formulario web)
        String notas = rs.getString("notas_usuario");
        String cliente = "Cliente";
        String detalle = "";
        if (notas != null) {
            for (String linea : notas.split("\n")) {
                if (linea.startsWith("Cliente:")) {
                    cliente = linea.substring("Cliente:".length()).trim();
                } else if (linea.startsWith("Detalle:")) {
                    detalle = linea.substring("Detalle:".length()).trim();
                }
            }
            if (detalle.isEmpty() && !notas.isBlank()) {
                detalle = notas;
            }
        }
        c.setCliente(cliente);
        c.setDetalle(detalle);
        Timestamp fh = rs.getTimestamp("fecha_hora");
        if (fh != null) {
            var ldt = fh.toLocalDateTime();
            c.setFecha(ldt.toLocalDate().toString());
            c.setHora(ldt.toLocalTime().withSecond(0).withNano(0).toString());
        }
        return c;
    }

    // Inserto cita con estado solicitada; notas guardan nombre y motivo del cliente
    @Override
    public void agregarCita(Cita cita, long clienteUsuarioId) {
        if (cita == null) {
            return;
        }
        Long tid = cita.getTallerId();
        if (tid == null) {
            Taller p = obtenerPrincipal();
            tid = p != null ? p.getId() : null;
        }
        if (tid == null) {
            return;
        }
        String notas = "Cliente: " + (cita.getCliente() != null ? cita.getCliente() : "Cliente web")
                + "\nDetalle: " + (cita.getDetalle() != null ? cita.getDetalle() : "");
        String f = cita.getFecha() != null ? cita.getFecha() : "1970-01-01";
        String h = cita.getHora() != null ? cita.getHora() : "00:00";
        if (h.length() == 5) {
            h = h + ":00";
        }
        Timestamp tsCita = Timestamp.valueOf(f + " " + h);
        long citaId = siguienteIdEn("cita");
        jdbc.update(
                """
                        INSERT INTO cita (id, usuario_id, taller_id, servicio_id, fecha_hora, estado, notas_usuario, created_at, updated_at)
                        VALUES (?, ?, ?, NULL, ?, 'solicitada', ?, NOW(), NOW())
                        """,
                citaId,
                clienteUsuarioId,
                tid,
                tsCita,
                notas);
    }

    // Si la BD vieja no tiene columna origen, repito la consulta sin ese filtro
    @Override
    // Ultimas 30 busquedas; pruebo con columna origen y si falla sin ella
    public List<BusquedaHistorial> listarHistorialUsuario(long usuarioId) {
        try {
            return consultarHistorialBuscador(usuarioId, true);
        } catch (BadSqlGrammarException ex) {
            return consultarHistorialBuscador(usuarioId, false);
        }
    }

    // SELECT historial; conColumnaOrigen filtra origen=buscador si existe la columna
    private List<BusquedaHistorial> consultarHistorialBuscador(long usuarioId, boolean conColumnaOrigen) {
        String sql = conColumnaOrigen
                ? """
                SELECT id, usuario_id, termino, origen_lat, origen_lng, radio_m, resultados, origen, created_at
                FROM historial_busqueda
                WHERE usuario_id = ?
                  AND (origen IS NULL OR origen = ?)
                ORDER BY created_at DESC
                LIMIT 30
                """
                : """
                SELECT id, usuario_id, termino, origen_lat, origen_lng, radio_m, resultados, created_at
                FROM historial_busqueda
                WHERE usuario_id = ?
                ORDER BY created_at DESC
                LIMIT 30
                """;
        if (conColumnaOrigen) {
            return jdbc.query(sql, this::mapearFilaHistorial, usuarioId, ORIGEN_HISTORIAL_BUSCADOR);
        }
        return jdbc.query(sql, this::mapearFilaHistorial, usuarioId);
    }

    // Fila SQL -> BusquedaHistorial.desdeBd
    private BusquedaHistorial mapearFilaHistorial(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        String cuando = ts != null ? FMT_FECHA_HORA.format(ts.toLocalDateTime()) : "";
        int res = rs.getInt("resultados");
        Integer resultados = rs.wasNull() ? null : res;
        String origen = ORIGEN_HISTORIAL_BUSCADOR;
        try {
            origen = rs.getString("origen");
        } catch (java.sql.SQLException ignored) {
            // columna origen aun no migrada en BD vieja
        }
        return BusquedaHistorial.desdeBd(
                rs.getLong("id"),
                rs.getLong("usuario_id"),
                rs.getString("termino"),
                rs.getObject("origen_lat") != null ? rs.getDouble("origen_lat") : null,
                rs.getObject("origen_lng") != null ? rs.getDouble("origen_lng") : null,
                rs.getObject("radio_m") != null ? rs.getInt("radio_m") : null,
                resultados,
                origen,
                ts != null ? ts.toLocalDateTime() : null,
                cuando);
    }

    @Override
    // INSERT en historial_busqueda; ignoro duplicado reciente
    public void registrarHistorialBusqueda(
            long usuarioId,
            String termino,
            Double origenLat,
            Double origenLng,
            Integer radioM,
            Integer resultados) {
        if (termino == null || termino.isBlank()) {
            return;
        }
        String limpio = termino.trim();
        Integer repetido = jdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM historial_busqueda
                        WHERE usuario_id = ? AND termino = ?
                          AND created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE)
                        """,
                Integer.class,
                usuarioId,
                limpio);
        // Evito duplicar la misma busqueda en menos de 1 minuto
        if (repetido != null && repetido > 0) {
            return;
        }
        long id = siguienteIdEn("historial_busqueda");
        try {
            jdbc.update(
                    """
                            INSERT INTO historial_busqueda (id, usuario_id, termino, origen_lat, origen_lng, radio_m, resultados, origen, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                            """,
                    id,
                    usuarioId,
                    limpio,
                    origenLat,
                    origenLng,
                    radioM,
                    resultados,
                    ORIGEN_HISTORIAL_BUSCADOR);
        } catch (org.springframework.dao.DataAccessException ex) {
            // INSERT sin columna origen si el esquema aun no la tiene
            jdbc.update(
                    """
                            INSERT INTO historial_busqueda (id, usuario_id, termino, origen_lat, origen_lng, radio_m, resultados, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                            """,
                    id,
                    usuarioId,
                    limpio,
                    origenLat,
                    origenLng,
                    radioM,
                    resultados);
        }
    }

    @Override
    // Ultima fila valoracion del usuario; si no hay devuelvo placeholders
    public UltimaCalificacion obtenerUltimaCalificacion(long usuarioId) {
        String sql = """
                SELECT t.nombre AS taller_nom, v.estrellas, v.comentario
                FROM valoracion v
                JOIN taller t ON t.id = v.taller_id
                WHERE v.usuario_id = ?
                ORDER BY v.created_at DESC
                LIMIT 1
                """;
        List<UltimaCalificacion> lista = jdbc.query(sql, (rs, rowNum) -> {
            String com = rs.getString("comentario");
            String servicio = "Valoración";
            String comentario = com != null ? com : "";
            if (comentario.startsWith("Servicio: ")) {
                int sep = comentario.indexOf(" — ");
                if (sep > 0) {
                    servicio = comentario.substring("Servicio: ".length(), sep);
                    comentario = comentario.substring(sep + 3);
                } else {
                    servicio = comentario.substring("Servicio: ".length());
                    comentario = "";
                }
            }
            return new UltimaCalificacion(
                    rs.getString("taller_nom"),
                    servicio,
                    rs.getInt("estrellas"),
                    comentario);
        }, usuarioId);
        if (lista.isEmpty()) {
            return new UltimaCalificacion("—", "—", 0, "Sin valoraciones.");
        }
        return lista.get(0);
    }

    @Override
    // INSERT valoracion; busco taller por nombre del formulario
    public void registrarCalificacion(UltimaCalificacion calificacion, long usuarioId) {
        if (calificacion == null) {
            return;
        }
        Long tallerId = resolverTallerIdPorNombre(calificacion.getTallerNombre());
        if (tallerId == null) {
            Taller principal = obtenerPrincipal();
            if (principal == null) {
                return;
            }
            tallerId = principal.getId();
        }
        String comentario = calificacion.getComentario() != null ? calificacion.getComentario().trim() : "";
        if (calificacion.getServicio() != null && !calificacion.getServicio().isBlank()) {
            String serv = calificacion.getServicio().trim();
            comentario = comentario.isEmpty() ? "Servicio: " + serv : "Servicio: " + serv + " — " + comentario;
        }
        int estrellas = calificacion.getEstrellas();
        if (estrellas < 1) {
            estrellas = 1;
        }
        if (estrellas > 5) {
            estrellas = 5;
        }
        long valId = siguienteIdEn("valoracion");
        jdbc.update(
                """
                        INSERT INTO valoracion (id, usuario_id, taller_id, cita_id, estrellas, comentario, created_at)
                        VALUES (?, ?, ?, NULL, ?, ?, NOW())
                        """,
                valId,
                usuarioId,
                tallerId,
                estrellas,
                comentario);
    }

    // Al valorar escribo el nombre del taller; busco id exacto o parecido
    private Long resolverTallerIdPorNombre(String nombreTaller) {
        if (nombreTaller == null || nombreTaller.isBlank()) {
            return null;
        }
        String limpio = nombreTaller.trim();
        List<Long> exacto = jdbc.query(
                "SELECT id FROM taller WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                limpio);
        if (!exacto.isEmpty()) {
            return exacto.get(0);
        }
        List<Long> parecido = jdbc.query(
                "SELECT id FROM taller WHERE nombre LIKE ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                "%" + limpio + "%");
        return parecido.isEmpty() ? null : parecido.get(0);
    }
}
