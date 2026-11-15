package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.security.SesionActual;
import com.tecnologico.talleres.services.AgendaService;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.util.Ids;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Citas del cliente: ver las proximas y el historial, editar una cita y cancelarla.
// SecurityConfig ya limita /mis-citas/** al rol USER; aqui ademas cada cita se busca por id Y por dueno.
@Controller
public class MisCitasController {

    private final TallerService tallerService;
    private final AgendaService agendaService;

    public MisCitasController(TallerService tallerService, AgendaService agendaService) {
        this.tallerService = tallerService;
        this.agendaService = agendaService;
    }

    // Proximas (la mas cercana primero) e historial (la mas reciente primero)
    @GetMapping("/mis-citas")
    public String listar(Model model) {
        // El servicio las devuelve ordenadas de la mas nueva a la mas vieja
        List<Cita> todas = tallerService.listarCitasPorCliente(SesionActual.usuarioId());
        List<Cita> proximas = new ArrayList<>();
        List<Cita> historial = new ArrayList<>();
        for (Cita c : todas) {
            if (c.isEditable()) {
                proximas.add(c);
            } else {
                historial.add(c);
            }
        }
        Collections.reverse(proximas);
        model.addAttribute("proximas", proximas);
        model.addAttribute("historial", historial);
        return "mis-citas";
    }

    // Formulario para cambiar fecha, hora, motivo o nombre de contacto
    @GetMapping("/mis-citas/{id}/editar")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        if (!Ids.esValido(id)) {
            return "redirect:/mis-citas";
        }
        Cita cita = agendaService.citaDelCliente(id, SesionActual.usuarioId());
        if (cita == null) {
            ra.addFlashAttribute("msgError", "La cita no existe.");
            return "redirect:/mis-citas";
        }
        if (!cita.isEditable()) {
            ra.addFlashAttribute("msgError", "Esta cita ya no se puede modificar.");
            return "redirect:/mis-citas";
        }
        model.addAttribute("cita", cita);
        return "editar-cita";
    }

    @PostMapping("/mis-citas/{id}/editar")
    public String guardar(
            @PathVariable String id,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam String motivo,
            @RequestParam(name = "cliente", required = false) String cliente,
            RedirectAttributes ra) {
        if (!Ids.esValido(id)) {
            return "redirect:/mis-citas";
        }
        try {
            agendaService.editarCita(id, SesionActual.usuarioId(), cliente, motivo, fecha, hora);
            ra.addFlashAttribute("msgOk", "Cita actualizada.");
            return "redirect:/mis-citas";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            return "redirect:/mis-citas/" + id + "/editar";
        }
    }

    @PostMapping("/mis-citas/{id}/cancelar")
    public String cancelar(@PathVariable String id, RedirectAttributes ra) {
        if (!Ids.esValido(id)) {
            return "redirect:/mis-citas";
        }
        try {
            agendaService.cancelarCita(id, SesionActual.usuarioId());
            ra.addFlashAttribute("msgOk", "Cita cancelada.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/mis-citas";
    }
}
