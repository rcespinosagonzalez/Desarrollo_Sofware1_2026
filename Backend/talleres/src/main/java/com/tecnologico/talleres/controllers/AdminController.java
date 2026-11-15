package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.services.TallerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Acciones del administrador sobre los talleres (SecurityConfig limita /admin/** al rol ADMIN)
@Controller
public class AdminController {

    private final TallerService tallerService;

    public AdminController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    // Activa o extiende el plan destacado de un taller (aparece como patrocinado bajo el mapa)
    @PostMapping("/admin/talleres/{id}/destacar")
    public String destacar(@PathVariable String id,
                           @RequestParam(name = "dias", required = false, defaultValue = "30") int dias,
                           RedirectAttributes ra) {
        try {
            tallerService.destacar(id, dias);
            ra.addFlashAttribute("msgOk", "Plan destacado activado por " + dias + " días.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/mi-taller#seccion-talleres";
    }

    @PostMapping("/admin/talleres/{id}/quitar-destacado")
    public String quitarDestacado(@PathVariable String id, RedirectAttributes ra) {
        try {
            tallerService.quitarDestacado(id);
            ra.addFlashAttribute("msgOk", "Plan destacado quitado.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/mi-taller#seccion-talleres";
    }
}
