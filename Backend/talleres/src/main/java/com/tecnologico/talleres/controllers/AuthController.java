package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.dto.LoginResponse;
import com.tecnologico.talleres.repositories.UsuarioRepositorio;
import com.tecnologico.talleres.util.Ids;
import com.tecnologico.talleres.security.JwtService;
import com.tecnologico.talleres.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// Login REST: busco el usuario en MongoDB, comparo la clave con BCrypt y entrego un JWT en cookie HttpOnly
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MSG_INCORRECTO = "Correo o contraseña incorrectos.";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int CLAVE_MIN = 8;
    // BCrypt solo usa los primeros 72 bytes de la clave
    private static final int CLAVE_MAX_BYTES = 72;

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService intentos;

    // Spring inyecta el repositorio de usuarios, BCrypt (clave), el servicio de tokens y el limitador de intentos
    public AuthController(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder, JwtService jwtService,
                          LoginAttemptService intentos) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.intentos = intentos;
    }

    // POST con formulario (email, password) como hace script.js en index
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<LoginResponse> login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Completa correo y contraseña.");
        }
        // Tras varios fallos seguidos se bloquea ese correo desde esa IP por un rato
        String claveIntentos = LoginAttemptService.clave(request.getRemoteAddr(), email);
        long espera = intentos.segundosBloqueado(claveIntentos);
        if (espera > 0) {
            long minutos = (espera + 59) / 60;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(espera))
                    .body(respuestaError("Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " min."));
        }
        // Traigo el usuario por correo (guardado en minusculas); la clave se compara con BCrypt
        UsuarioDoc u = usuarios.buscarActivoPorEmail(email.trim().toLowerCase());
        // Mismo mensaje si el correo no existe o la clave falla (no revelo cual de los dos)
        if (u == null || !passwordEncoder.matches(password, u.passwordHash)) {
            intentos.registrarFallo(claveIntentos);
            return error(HttpStatus.UNAUTHORIZED, MSG_INCORRECTO);
        }
        intentos.registrarExito(claveIntentos);
        String id = Ids.texto(u.id);
        // admin, taller o user: lo usan las rutas protegidas y el menu del front
        String rolFront = normalizarRol(u.rol);
        String token = jwtService.generarToken(id, u.nombre, rolFront);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.crearCookie(token).toString())
                .body(new LoginResponse(true, u.nombre, rolFront, null, id, token));
    }

    // Registro de clientes (rol user). Si sale bien, deja la sesion iniciada igual que el login
    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<LoginResponse> registro(
            @RequestParam("nombre") String nombre,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        String nom = nombre == null ? "" : nombre.trim();
        String correo = email == null ? "" : email.trim().toLowerCase();
        String clave = password == null ? "" : password;

        if (nom.length() < 2 || nom.length() > 120) {
            return error(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 2 y 120 caracteres.");
        }
        if (correo.length() > 320 || !EMAIL.matcher(correo).matches()) {
            return error(HttpStatus.BAD_REQUEST, "Escribe un correo válido.");
        }
        if (clave.length() < CLAVE_MIN) {
            return error(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos " + CLAVE_MIN + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAX_BYTES) {
            return error(HttpStatus.BAD_REQUEST, "La contraseña es demasiado larga.");
        }

        UsuarioDoc creado;
        try {
            // El rol siempre es user (nadie se registra como admin o taller)
            creado = usuarios.crear(correo, passwordEncoder.encode(clave), nom, "user");
        } catch (DuplicateKeyException ex) {
            // El indice unico uk_usuarios_email rechaza el correo repetido, incluso con dos registros simultaneos
            return error(HttpStatus.CONFLICT, "Ese correo ya está registrado.");
        }

        String id = Ids.texto(creado.id);
        String token = jwtService.generarToken(id, nom, "user");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.crearCookie(token).toString())
                .body(new LoginResponse(true, nom, "user", null, id, token));
    }

    // Respuesta de error con su codigo HTTP (400, 401, 409) y el mismo JSON que usa el front
    private static ResponseEntity<LoginResponse> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(respuestaError(mensaje));
    }

    private static LoginResponse respuestaError(String mensaje) {
        return new LoginResponse(false, null, null, mensaje, null, null);
    }

    // Mapeo de rol en BD a lo que entiende el front (sessionStorage + menus)
    private static String normalizarRol(String rolDb) {
        if (rolDb == null) {
            return "user";
        }
        String r = rolDb.trim().toLowerCase();
        if ("admin".equals(r)) {
            return "admin";
        }
        if ("taller".equals(r)) {
            return "taller";
        }
        return "user";
    }
}
