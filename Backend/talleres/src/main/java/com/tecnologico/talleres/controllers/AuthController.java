package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.dto.LoginResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Login REST: valido email y clave en MySQL y guardo id, rol y nombre en la sesion HTTP
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;

    // Spring inyecta JdbcTemplate para consultar usuario
    public AuthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // POST con formulario (email, password) como hace script.js en index
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public LoginResponse login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session) {
        if (email == null || email.isBlank() || password == null) {
            return new LoginResponse(false, null, null, "Completa correo y contraseña.", null);
        }
        String em = email.trim();
        // password_hash en demo es texto plano; en produccion iria un hash real
        String sql = """
                SELECT id, nombre, rol FROM usuario
                WHERE email = ? AND password_hash = ? AND COALESCE(activo, 0) = 1
                LIMIT 1
                """;
        List<SesionUsuario> filas = jdbc.query(
                sql,
                (rs, rowNum) -> new SesionUsuario(rs.getLong("id"), rs.getString("nombre"), rs.getString("rol")),
                em,
                password);
        if (filas.isEmpty()) {
            return new LoginResponse(false, null, null, "Correo o contraseña incorrectos.", null);
        }
        SesionUsuario su = filas.get(0);
        // admin, taller o user: lo usa el interceptor y las vistas
        String rolFront = normalizarRol(su.rolDb());
        session.setAttribute("usuarioId", su.id());
        session.setAttribute("rol", rolFront);
        session.setAttribute("nombre", su.nombre());
        return new LoginResponse(true, su.nombre(), rolFront, null, su.id());
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
    private record SesionUsuario(long id, String nombre, String rolDb) {}
}
