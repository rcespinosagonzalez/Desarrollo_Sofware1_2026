package com.tecnologico.talleres.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Al arrancar: si una base vieja tiene contrasenas en texto plano, las convierte a BCrypt.
// Con el SQL nuevo (ya trae hashes) no hace nada.
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public PasswordMigrationRunner(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        try {
            List<Map<String, Object>> filas = jdbc.queryForList(
                    "SELECT id, password_hash FROM usuario WHERE password_hash NOT LIKE '$2%'");
            for (Map<String, Object> fila : filas) {
                String plano = String.valueOf(fila.get("password_hash"));
                jdbc.update("UPDATE usuario SET password_hash = ? WHERE id = ?",
                        encoder.encode(plano), fila.get("id"));
            }
            if (!filas.isEmpty()) {
                log.info("Contrasenas convertidas a BCrypt: {}", filas.size());
            }
        } catch (Exception e) {
            // Si la tabla aun no existe no debe tumbar la aplicacion
            log.warn("No se pudo revisar las contrasenas: {}", e.getMessage());
        }
    }
}
