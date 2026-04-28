package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.dto.LoginResponse;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Login leyendo la tabla usuario (misma clave que guardamos en password_hash, sin hash en esta práctica).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;

    public AuthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public LoginResponse login(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        if (email == null || email.isBlank() || password == null) {
            return new LoginResponse(false, null, null, "Completa correo y contraseña.");
        }
        String em = email.trim();
        // activo en MySQL suele ser TINYINT 0/1
        String sql = """
                SELECT nombre, rol FROM usuario
                WHERE email = ? AND password_hash = ? AND COALESCE(activo, 0) = 1
                LIMIT 1
                """;
        List<LoginResponse> filas = jdbc.query(sql, (rs, rowNum) -> new LoginResponse(
                true,
                rs.getString("nombre"),
                rs.getString("rol"),
                null), em, password);
        if (filas.isEmpty()) {
            return new LoginResponse(false, null, null, "Correo o contraseña incorrectos.");
        }
        LoginResponse fila = filas.get(0);
        return new LoginResponse(true, fila.nombre(), normalizarRol(fila.rol()), null);
    }

    /** El menú del front solo entiende admin vs user. */
    private static String normalizarRol(String rolDb) {
        if (rolDb == null) {
            return "user";
        }
        return "admin".equalsIgnoreCase(rolDb.trim()) ? "admin" : "user";
    }
}
