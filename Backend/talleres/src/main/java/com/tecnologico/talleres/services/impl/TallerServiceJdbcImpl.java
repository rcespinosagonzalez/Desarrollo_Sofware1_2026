package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.services.TallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Conexión simple a MySQL con JdbcTemplate; IDs numéricos (1, 2, 3…).
 */
@Service
public class TallerServiceJdbcImpl implements TallerService {

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbc;
    private final Long propietarioUsuarioId;
    private final Long clienteUsuarioId;

    public TallerServiceJdbcImpl(
            JdbcTemplate jdbc,
            @Value("${app.propietario-usuario-id}") long propietarioUsuarioId,
            @Value("${app.cliente-usuario-id}") long clienteUsuarioId) {
        this.jdbc = jdbc;
        this.propietarioUsuarioId = propietarioUsuarioId;
        this.clienteUsuarioId = clienteUsuarioId;
    }

    private long siguienteIdEn(String tabla) {
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + tabla, Long.class);
        return max + 1;
    }

    @Override
    public List<Taller> listarTodos() {
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                LEFT JOIN ubicacion_taller ubi ON ubi.taller_id = t.id
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                LEFT JOIN (
                    SELECT taller_id, AVG(estrellas) AS prom, COUNT(*) AS cnt
                    FROM valoracion GROUP BY taller_id
                ) va ON va.taller_id = t.id
                ORDER BY t.id DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs));
    }

    /** Arma un Taller desde el ResultSet de las consultas con join ubicacion + usuario dueño. */
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
        t.setCalificacionPromedio(rs.getDouble("cal_prom"));
        t.setNumValoraciones(rs.getInt("num_val"));
        t.setEmailPropietario(rs.getString("email_propietario"));
        t.setNombrePropietario(rs.getString("nombre_propietario"));
        return t;
    }

    @Override
    public List<Taller> listarTalleresDelPropietario(long propietarioUsuarioId) {
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                LEFT JOIN ubicacion_taller ubi ON ubi.taller_id = t.id
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                LEFT JOIN (
                    SELECT taller_id, AVG(estrellas) AS prom, COUNT(*) AS cnt
                    FROM valoracion GROUP BY taller_id
                ) va ON va.taller_id = t.id
                WHERE t.propietario_usuario_id = ?
                ORDER BY t.id DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs), propietarioUsuarioId);
    }

    @Override
    public Taller obtenerPorId(Long id) {
        if (id == null) {
            return null;
        }
        String sql = """
                SELECT t.id, t.nombre, t.telefono, t.descripcion, t.horario, t.servicios_texto,
                       ubi.direccion_texto,
                       usr.email AS email_propietario,
                       usr.nombre AS nombre_propietario,
                       COALESCE(va.prom, 0) AS cal_prom, COALESCE(va.cnt, 0) AS num_val
                FROM taller t
                LEFT JOIN ubicacion_taller ubi ON ubi.taller_id = t.id
                LEFT JOIN usuario usr ON usr.id = t.propietario_usuario_id
                LEFT JOIN (
                    SELECT taller_id, AVG(estrellas) AS prom, COUNT(*) AS cnt
                    FROM valoracion GROUP BY taller_id
                ) va ON va.taller_id = t.id
                WHERE t.id = ?
                """;
        List<Taller> lista = jdbc.query(sql, (rs, rowNum) -> mapearFilaTaller(rs), id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public Taller obtenerPrincipal() {
        // El más reciente primero: así el usuario no queda pegado siempre al taller id 1
        String sql = "SELECT t.id FROM taller t ORDER BY t.id DESC LIMIT 1";
        List<Long> ids = jdbc.query(sql, (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            return null;
        }
        return obtenerPorId(ids.get(0));
    }

    @Override
    public void guardar(Taller taller) {
        if (taller == null) {
            return;
        }
        // Nuevo taller: el id NO debe venir en el POST (evita que Spring ponga 0 y reutilice la fila 1).
        Long id = taller.getId();
        if (id != null && id <= 0) {
            taller.setId(null);
            id = null;
        }
        Integer cuenta = id == null ? 0 : jdbc.queryForObject("SELECT COUNT(*) FROM taller WHERE id = ?", Integer.class, id);
        boolean existe = cuenta != null && cuenta > 0;

        if (existe) {
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
            int n = jdbc.update(
                    "UPDATE ubicacion_taller SET direccion_texto=? WHERE taller_id=?",
                    taller.getDireccion(),
                    id);
            if (n == 0) {
                long ubiId = siguienteIdEn("ubicacion_taller");
                jdbc.update(
                        """
                                INSERT INTO ubicacion_taller (id, taller_id, direccion_texto, ciudad, departamento, pais, lat, lng)
                                VALUES (?, ?, ?, NULL, NULL, 'CO', NULL, NULL)
                                """,
                        ubiId,
                        id,
                        taller.getDireccion());
            }
        } else {
            // Alta: si vienen correo+clave creamos usuario dueño; si no, el dueño es el admin del properties
            long propietarioTaller = propietarioUsuarioId;
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
                // En clase guardamos la clave tal cual (en producción iría hasheada)
                jdbc.update(
                        """
                                INSERT INTO usuario (id, email, password_hash, nombre, rol, activo, created_at, updated_at)
                                VALUES (?, ?, ?, ?, 'user', TRUE, NOW(), NOW())
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
            long ubiId = siguienteIdEn("ubicacion_taller");
            jdbc.update(
                    """
                            INSERT INTO ubicacion_taller (id, taller_id, direccion_texto, ciudad, departamento, pais, lat, lng)
                            VALUES (?, ?, ?, NULL, NULL, 'CO', NULL, NULL)
                            """,
                    ubiId,
                    nuevoId,
                    taller.getDireccion() != null ? taller.getDireccion() : "");
            taller.setCalificacionPromedio(0);
            taller.setNumValoraciones(0);
        }
    }

    @Override
    public List<Valoracion> listarValoraciones() {
        String sql = """
                SELECT u.nombre AS cliente, v.estrellas, v.comentario, v.created_at
                FROM valoracion v
                JOIN usuario u ON u.id = v.usuario_id
                ORDER BY v.created_at DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("created_at");
            String fecha = ts != null ? ts.toLocalDateTime().toLocalDate().toString() : "";
            return new Valoracion(rs.getString("cliente"), rs.getInt("estrellas"), rs.getString("comentario"), fecha);
        });
    }

    @Override
    public List<Cita> listarCitas() {
        String sql = """
                SELECT c.id, c.notas_usuario, c.fecha_hora, c.taller_id
                FROM cita c
                ORDER BY c.fecha_hora DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapearCita(rs));
    }

    private static Cita mapearCita(java.sql.ResultSet rs) throws java.sql.SQLException {
        Cita c = new Cita();
        c.setId(rs.getLong("id"));
        c.setTallerId(rs.getLong("taller_id"));
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

    @Override
    public void agregarCita(Cita cita) {
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

    @Override
    public List<BusquedaHistorial> listarHistorial() {
        String sql = "SELECT termino, created_at FROM historial_busqueda ORDER BY created_at DESC";
        return jdbc.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("created_at");
            String cuando = ts != null ? FMT_FECHA_HORA.format(ts.toLocalDateTime()) : "";
            return new BusquedaHistorial(rs.getString("termino"), cuando);
        });
    }

    @Override
    public UltimaCalificacion obtenerUltimaCalificacion() {
        String sql = """
                SELECT t.nombre AS taller_nom, v.estrellas, v.comentario
                FROM valoracion v
                JOIN taller t ON t.id = v.taller_id
                ORDER BY v.created_at DESC
                LIMIT 1
                """;
        List<UltimaCalificacion> lista = jdbc.query(sql, (rs, rowNum) -> new UltimaCalificacion(
                rs.getString("taller_nom"),
                "(servicio no guardado en BD)",
                rs.getInt("estrellas"),
                rs.getString("comentario")));
        if (lista.isEmpty()) {
            return new UltimaCalificacion("—", "—", 0, "Aún no hay valoraciones en la base de datos.");
        }
        return lista.get(0);
    }

    @Override
    public void registrarCalificacion(UltimaCalificacion calificacion) {
        if (calificacion == null) {
            return;
        }
        Taller principal = obtenerPrincipal();
        if (principal == null) {
            return;
        }
        long tallerId = principal.getId();
        if (calificacion.getTallerNombre() != null && !calificacion.getTallerNombre().isBlank()) {
            List<Long> ids = jdbc.query(
                    "SELECT id FROM taller WHERE nombre = ? LIMIT 1",
                    (rs, rowNum) -> rs.getLong("id"),
                    calificacion.getTallerNombre());
            if (!ids.isEmpty()) {
                tallerId = ids.get(0);
            }
        }
        long valId = siguienteIdEn("valoracion");
        jdbc.update(
                """
                        INSERT INTO valoracion (id, usuario_id, taller_id, cita_id, estrellas, comentario, created_at)
                        VALUES (?, ?, ?, NULL, ?, ?, NOW())
                        """,
                valId,
                clienteUsuarioId,
                tallerId,
                calificacion.getEstrellas(),
                calificacion.getComentario() != null ? calificacion.getComentario() : "");
    }
}
