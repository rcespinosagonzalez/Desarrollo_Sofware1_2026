package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.services.AgendaService;
import com.tecnologico.talleres.services.TallerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// JSON de talleres para el mapa del cliente (Leaflet)
@RestController
@RequestMapping("/api/talleres")
public class TalleresController {

    private final TallerService tallerService;
    private final AgendaService agendaService;

    public TalleresController(TallerService tallerService, AgendaService agendaService) {
        this.tallerService = tallerService;
        this.agendaService = agendaService;
    }

    // GET /api/talleres -> JSON para buscar-talleres.js
    @GetMapping
    public List<Taller> listar() {
        return tallerService.listarTodos();
    }

    // GET /api/talleres/{id}/disponibilidad?fecha=2026-11-20 -> horas de inicio con mecanico libre ese dia
    @GetMapping("/{id}/disponibilidad")
    public Map<String, Object> disponibilidad(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "excluirCita", required = false) String excluirCita) {
        // excluirCita: al editar una cita, su propia hora no cuenta como ocupada
        return Map.of("fecha", fecha.toString(), "horas", agendaService.horasLibres(id, fecha, excluirCita));
    }
}
