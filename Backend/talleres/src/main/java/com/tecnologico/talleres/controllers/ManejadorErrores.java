package com.tecnologico.talleres.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Errores de los endpoints /api: respuesta JSON con el codigo correcto, sin mostrar detalles internos.
// Las paginas HTML usan templates/error.html (la pagina de error de Spring Boot).
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Datos invalidos que lanzo el servicio: el mensaje esta pensado para el usuario
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "mensaje", ex.getMessage()));
    }

    // Fallos de base de datos: al log el detalle, al cliente un mensaje generico
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> errorBd(DataAccessException ex) {
        log.error("Error de base de datos", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("ok", false, "mensaje", "Error interno. Intenta de nuevo más tarde."));
    }
}
