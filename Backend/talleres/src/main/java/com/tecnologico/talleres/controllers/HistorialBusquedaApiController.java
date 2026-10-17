package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.dto.HistorialBusquedaRequest;
import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.security.SesionActual;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Historial de busquedas del mapa: leo y guardo por usuario en sesion
@RestController
@RequestMapping("/api/historial-busqueda")
public class HistorialBusquedaApiController {

    private final TallerService tallerService;

    public HistorialBusquedaApiController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    // Ultimas busquedas del cliente logueado
    @GetMapping
    public ResponseEntity<List<BusquedaHistorial>> listar() {
        Long uid = SesionActual.usuarioId();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(tallerService.listarHistorialUsuario(uid));
    }

    // Lo llama buscar-talleres.js al buscar un lugar en el mapa
    @PostMapping
    public ResponseEntity<Void> registrar(@RequestBody HistorialBusquedaRequest body) {
        Long uid = SesionActual.usuarioId();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null || body.termino() == null || body.termino().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        tallerService.registrarHistorialBusqueda(
                uid,
                body.termino().trim(),
                body.origenLat(),
                body.origenLng(),
                body.radioM(),
                body.resultados());
        return ResponseEntity.ok().build();
    }
}
