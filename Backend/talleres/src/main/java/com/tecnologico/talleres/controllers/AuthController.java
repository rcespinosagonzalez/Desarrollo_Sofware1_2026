package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.dto.LoginResponse;
import com.tecnologico.talleres.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Login REST: busco el usuario en MySQL, comparo la clave con BCrypt y entrego un JWT en cookie HttpOnly
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MSG_INCORRECTO = "Correo o contraseña incorrectos.";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Spring inyecta JdbcTemplate (consulta), BCrypt (clave) y el servicio de tokens
    public AuthController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // POST con formulario (email, password) como hace script.js en index
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<LoginResponse> login(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return ResponseEntity.ok(respuestaError("Completa correo y contraseña."));
        }
        // Traigo el hash por correo; la clave NO va en el WHERE, se compara con BCrypt
        String sql = """
                SELECT id, nombre, rol, password_hash FROM usuario
                WHERE email = ? AND COALESCE(activo, 0) = 1
                LIMIT 1
                """;
        List<FilaUsuario> filas = jdbc.query(
                sql,
                (rs, rowNum) -> new FilaUsuario(
                        rs.getLong("id"), rs.getString("nombre"), rs.getString("rol"), rs.getString("password_hash")),
                email.trim());
        // Mismo mensaje si el correo no existe o la clave falla (no revelo cual de los dos)
        if (filas.isEmpty() || !passwordEncoder.matches(password, filas.get(0).passwordHash())) {
            return ResponseEntity.ok(respuestaError(MSG_INCORRECTO));
        }
        FilaUsuario u = filas.get(0);
        // admin, taller o user: lo usan las rutas protegidas y el menu del front
        String rolFront = normalizarRol(u.rolDb());
        String token = jwtService.generarToken(u.id(), u.nombre(), rolFront);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.crearCookie(token).toString())
                .body(new LoginResponse(true, u.nombre(), rolFront, null, u.id(), token));
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

    // Fila minima del SELECT de login
    private record FilaUsuario(long id, String nombre, String rolDb, String passwordHash) {}
}
