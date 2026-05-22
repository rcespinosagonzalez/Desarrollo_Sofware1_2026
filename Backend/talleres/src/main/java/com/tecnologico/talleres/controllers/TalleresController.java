package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.services.TallerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// JSON de talleres para el mapa del cliente (Leaflet)
@RestController
@RequestMapping("/api/talleres")
public class TalleresController {

    private final TallerService tallerService;

    public TalleresController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    // GET /api/talleres -> JSON para buscar-talleres.js
    @GetMapping
    public List<Taller> listar() {
        return tallerService.listarTodos();
    }
}
