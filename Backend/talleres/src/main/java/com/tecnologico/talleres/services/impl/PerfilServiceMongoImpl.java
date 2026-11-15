package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import com.tecnologico.talleres.model.Perfil;
import com.tecnologico.talleres.model.Vehiculo;
import com.tecnologico.talleres.security.LoginAttemptService;
import com.tecnologico.talleres.services.PerfilService;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

// Perfil del cliente sobre MongoDB. Las acciones sensibles (cambiar clave o correo, desactivar la cuenta)
// piden la contrasena y comparten un limitador de intentos para que una sesion robada no pueda adivinarla.
@Service
public class PerfilServiceMongoImpl implements PerfilService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9+()\\s-]{7,20}$");
    private static final Pattern PLACA = Pattern.compile("^[A-Z0-9]{5,7}$");
    private static final int CLAVE_MIN = 8;
    // BCrypt solo usa los primeros 72 bytes de la clave
    private static final int CLAVE_MAX_BYTES = 72;
    private static final int MAX_VEHICULOS = 5;
    private static final String MSG_SESION = "Tu sesión ya no es válida. Vuelve a iniciar sesión.";
    private static final DateTimeFormatter MES_ANIO =
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    private static final Map<String, String> TIPOS = Map.of(
            "carro", "Carro", "camioneta", "Camioneta", "moto", "Moto", "camion", "Camión", "otro", "Otro");

    private final MongoTemplate mongo;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService intentos;
    private final ZoneId zona;

    public PerfilServiceMongoImpl(MongoTemplate mongo, PasswordEncoder passwordEncoder, LoginAttemptService intentos,
                                  @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.mongo = mongo;
        this.passwordEncoder = passwordEncoder;
        this.intentos = intentos;
        this.zona = ZoneId.of(zona);
    }

    @Override
    public Perfil obtener(String usuarioId) {
        UsuarioDoc u = usuarioActivo(usuarioId);
        if (u == null) {
            return null;
        }
        Perfil p = new Perfil();
        p.setId(Ids.texto(u.id));
        p.setNombre(u.nombre);
        p.setEmail(u.email);
        p.setTelefono(u.telefono != null ? u.telefono : "");
        p.setCiudad(u.ciudad != null ? u.ciudad : "");
        p.setIniciales(iniciales(u.nombre));
        if (u.creadoEn != null) {
            String mes = MES_ANIO.format(u.creadoEn.atZone(zona));
            p.setMiembroDesde(mes.substring(0, 1).toUpperCase(Locale.ROOT) + mes.substring(1));
        }
        List<Vehiculo> vehiculos = new ArrayList<>();
        if (u.vehiculos != null) {
            for (UsuarioDoc.VehiculoDoc d : u.vehiculos) {
                vehiculos.add(aVehiculo(d));
            }
        }
        p.setVehiculos(vehiculos);

        Criteria mias = Criteria.where("clienteId").is(u.id);
        p.setCitasTotales(mongo.count(Query.query(mias), CitaDoc.class));
        p.setCitasProximas(mongo.count(Query.query(Criteria.where("clienteId").is(u.id)
                .and("ocupaTurno").is(true)
                .and("estado").in("solicitada", "confirmada")
                .and("fechaHora").gt(Instant.now())), CitaDoc.class));
        p.setCitasCompletadas(mongo.count(Query.query(Criteria.where("clienteId").is(u.id)
                .and("estado").is("completada")), CitaDoc.class));
        p.setValoraciones(mongo.count(Query.query(Criteria.where("clienteId").is(u.id)), ValoracionDoc.class));
        return p;
    }

    @Override
    public String actualizarDatos(String usuarioId, String nombre, String telefono, String ciudad) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        String nom = limpio(nombre);
        if (nom.length() < 2 || nom.length() > 120) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 120 caracteres.");
        }
        String tel = telefono == null ? "" : telefono.trim();
        if (!tel.isEmpty() && !TELEFONO.matcher(tel).matches()) {
            throw new IllegalArgumentException("Escribe un teléfono válido (solo números, espacios, + y -).");
        }
        String ciu = limpio(ciudad);
        if (ciu.length() > 80) {
            throw new IllegalArgumentException("La ciudad es demasiado larga (máximo 80 caracteres).");
        }
        Update cambios = new Update().set("nombre", nom);
        if (tel.isEmpty()) {
            cambios.unset("telefono");
        } else {
            cambios.set("telefono", tel);
        }
        if (ciu.isEmpty()) {
            cambios.unset("ciudad");
        } else {
            cambios.set("ciudad", ciu);
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(u.id)), cambios, UsuarioDoc.class);
        return nom;
    }

    @Override
    public void cambiarClave(String usuarioId, String claveActual, String claveNueva, String confirmacion) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        String nueva = claveNueva == null ? "" : claveNueva;
        if (nueva.length() < CLAVE_MIN) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos " + CLAVE_MIN + " caracteres.");
        }
        if (nueva.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAX_BYTES) {
            throw new IllegalArgumentException("La nueva contraseña es demasiado larga.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }
        verificarClave(u, claveActual, "La contraseña actual no es correcta.");
        if (passwordEncoder.matches(nueva, u.passwordHash)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual.");
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(u.id)),
                new Update().set("passwordHash", passwordEncoder.encode(nueva)), UsuarioDoc.class);
    }

    @Override
    public void cambiarCorreo(String usuarioId, String nuevoCorreo, String clave) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        String correo = nuevoCorreo == null ? "" : nuevoCorreo.trim().toLowerCase(Locale.ROOT);
        if (correo.length() > 320 || !EMAIL.matcher(correo).matches()) {
            throw new IllegalArgumentException("Escribe un correo válido.");
        }
        if (correo.equals(u.email)) {
            throw new IllegalArgumentException("Ese ya es tu correo actual.");
        }
        verificarClave(u, clave, "La contraseña no es correcta.");
        try {
            mongo.updateFirst(Query.query(Criteria.where("_id").is(u.id)),
                    new Update().set("email", correo), UsuarioDoc.class);
        } catch (DuplicateKeyException ex) {
            // El indice unico uk_usuarios_email rechaza el correo repetido
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }
    }

    @Override
    public void agregarVehiculo(String usuarioId, String placa, String tipo, String marca, String modelo,
                                String anio, String color) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        String pl = placa == null ? "" : placa.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (!PLACA.matcher(pl).matches()) {
            throw new IllegalArgumentException("La placa debe tener entre 5 y 7 letras o números (ej. ABC123).");
        }
        if (tipo == null || !TIPOS.containsKey(tipo)) {
            throw new IllegalArgumentException("Elige un tipo de vehículo válido.");
        }
        String mar = limpio(marca);
        if (mar.isEmpty()) {
            throw new IllegalArgumentException("Escribe la marca del vehículo.");
        }
        String mod = limpio(modelo);
        String col = limpio(color);
        if (mar.length() > 40 || mod.length() > 40 || col.length() > 30) {
            throw new IllegalArgumentException("Marca, modelo o color son demasiado largos.");
        }
        Integer an = null;
        if (anio != null && !anio.isBlank()) {
            int maximo = LocalDate.now(zona).getYear() + 1;
            try {
                an = Integer.valueOf(anio.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El año no es válido.");
            }
            if (an < 1950 || an > maximo) {
                throw new IllegalArgumentException("El año debe estar entre 1950 y " + maximo + ".");
            }
        }
        if (u.vehiculos != null && u.vehiculos.size() >= MAX_VEHICULOS) {
            throw new IllegalArgumentException("Puedes guardar hasta " + MAX_VEHICULOS
                    + " vehículos. Elimina uno para agregar otro.");
        }
        UsuarioDoc.VehiculoDoc v = new UsuarioDoc.VehiculoDoc();
        v.id = new ObjectId();
        v.placa = pl;
        v.tipo = tipo;
        v.marca = mar;
        v.modelo = mod.isEmpty() ? null : mod;
        v.anio = an;
        v.color = col.isEmpty() ? null : col;
        // La condicion sobre la placa evita duplicados aunque se envie dos veces seguidas
        var r = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(u.id).and("vehiculos.placa").ne(pl)),
                new Update().push("vehiculos", v), UsuarioDoc.class);
        if (r.getMatchedCount() == 0) {
            throw new IllegalArgumentException("Ya tienes un vehículo con esa placa.");
        }
    }

    @Override
    public void eliminarVehiculo(String usuarioId, String vehiculoId) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        final ObjectId vid = Ids.oid(vehiculoId);
        if (vid == null || u.vehiculos == null) {
            throw new IllegalArgumentException("El vehículo no existe.");
        }
        List<UsuarioDoc.VehiculoDoc> resto = new ArrayList<>(u.vehiculos);
        if (!resto.removeIf(v -> vid.equals(v.id))) {
            throw new IllegalArgumentException("El vehículo no existe.");
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(u.id)),
                new Update().set("vehiculos", resto), UsuarioDoc.class);
    }

    @Override
    public void desactivarCuenta(String usuarioId, String clave) {
        UsuarioDoc u = requerirUsuario(usuarioId);
        verificarClave(u, clave, "La contraseña no es correcta.");
        // Las citas futuras se cancelan y sus turnos quedan libres para otros clientes (ocupaTurno en false)
        mongo.updateMulti(
                Query.query(Criteria.where("clienteId").is(u.id)
                        .and("ocupaTurno").is(true)
                        .and("estado").in("solicitada", "confirmada")
                        .and("fechaHora").gt(Instant.now())),
                new Update().set("estado", "cancelada").set("ocupaTurno", false),
                CitaDoc.class);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(u.id)),
                new Update().set("activo", false).set("desactivadoEn", Instant.now()), UsuarioDoc.class);
    }

    // Usuario activo por id; null si no existe, esta desactivado o el id no es valido
    private UsuarioDoc usuarioActivo(String usuarioId) {
        ObjectId oid = Ids.oid(usuarioId);
        if (oid == null) {
            return null;
        }
        return mongo.findOne(Query.query(Criteria.where("_id").is(oid).and("activo").is(true)), UsuarioDoc.class);
    }

    private UsuarioDoc requerirUsuario(String usuarioId) {
        UsuarioDoc u = usuarioActivo(usuarioId);
        if (u == null) {
            throw new IllegalArgumentException(MSG_SESION);
        }
        return u;
    }

    // Compara la clave con BCrypt. Tras varios fallos seguidos bloquea un rato (mismo limitador del login)
    private void verificarClave(UsuarioDoc u, String clave, String mensajeIncorrecta) {
        String llave = "perfil|" + Ids.texto(u.id);
        long espera = intentos.segundosBloqueado(llave);
        if (espera > 0) {
            throw new IllegalArgumentException(
                    "Demasiados intentos fallidos. Intenta de nuevo en " + ((espera + 59) / 60) + " min.");
        }
        if (clave == null || clave.isEmpty() || !passwordEncoder.matches(clave, u.passwordHash)) {
            intentos.registrarFallo(llave);
            throw new IllegalArgumentException(mensajeIncorrecta);
        }
        intentos.registrarExito(llave);
    }

    // Quita espacios de los bordes y junta los repetidos
    private static String limpio(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    // "Daniel Blanco Rodriguez" -> "DB"
    private static String iniciales(String nombre) {
        String[] partes = limpio(nombre).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isEmpty() && sb.length() < 2) {
                sb.append(parte.substring(0, 1).toUpperCase(Locale.ROOT));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static Vehiculo aVehiculo(UsuarioDoc.VehiculoDoc d) {
        Vehiculo v = new Vehiculo();
        v.setId(Ids.texto(d.id));
        v.setPlaca(d.placa);
        v.setTipo(d.tipo);
        v.setTipoTexto(TIPOS.getOrDefault(d.tipo, "Otro"));
        v.setMarca(d.marca);
        v.setModelo(d.modelo);
        v.setAnio(d.anio);
        v.setColor(d.color);
        return v;
    }
}
