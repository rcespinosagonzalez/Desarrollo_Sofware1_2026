package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.services.TallerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/talleres")
public class TalleresController {

    private final TallerService tallerService;

    public TalleresController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    @GetMapping
    public List<Taller> listar() {
        return tallerService.listarTodos();
    }
}
