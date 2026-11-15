package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.security.SesionActual;
import com.tecnologico.talleres.services.AgendaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// El dueno del taller confirma, completa o cancela las citas de su taller (SecurityConfig: /panel-taller/** es rol TALLER)
@Controller
public class PanelTallerController {

    private final AgendaService agendaService;

    public PanelTallerController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping("/panel-taller/citas/{id}/estado")
    public String cambiarEstado(@PathVariable String id,
                                @RequestParam(name = "estado", required = false) String estado,
                                RedirectAttributes ra) {
        try {
            agendaService.cambiarEstadoCita(id, SesionActual.usuarioId(), estado);
            ra.addFlashAttribute("msgOk", "Cita actualizada.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/panel-taller#seccion-dashboard";
    }
}
