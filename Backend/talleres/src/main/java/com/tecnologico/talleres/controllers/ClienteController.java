package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Perfil;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.Vehiculo;
import com.tecnologico.talleres.security.SesionActual;
import com.tecnologico.talleres.services.AgendaService;
import com.tecnologico.talleres.services.PerfilService;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.services.ValoracionService;
import com.tecnologico.talleres.util.Ids;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Acciones del cliente sobre un taller concreto: pedir cita (pantalla propia) y valorarlo (desde su ficha).
// SecurityConfig ya limita estas rutas al rol USER.
@Controller
public class ClienteController {

    private final TallerService tallerService;
    private final AgendaService agendaService;
    private final PerfilService perfilService;
    private final ValoracionService valoracionService;

    public ClienteController(TallerService tallerService, AgendaService agendaService,
                             PerfilService perfilService, ValoracionService valoracionService) {
        this.tallerService = tallerService;
        this.agendaService = agendaService;
        this.perfilService = perfilService;
        this.valoracionService = valoracionService;
    }

    // Pantalla "Pedir cita" de un taller (tallerId obligatorio: se llega desde el mapa o desde la ficha)
    @GetMapping("/solicitar-cita")
    public String pedirCita(@RequestParam(name = "tallerId", required = false) String tallerId,
                            Model model, RedirectAttributes ra) {
        Taller taller = tallerId == null ? null : tallerService.obtenerPorId(tallerId);
        if (taller == null) {
            ra.addFlashAttribute("msgError", "Elige primero un taller para pedir tu cita.");
            return "redirect:/buscar-talleres";
        }
        Perfil perfil = perfilService.obtener(SesionActual.usuarioId());
        model.addAttribute("taller", taller);
        model.addAttribute("vehiculos", perfil == null ? List.of() : perfil.getVehiculos());
        model.addAttribute("nombreContacto", perfil == null ? SesionActual.nombre() : perfil.getNombre());
        return "pedir-cita";
    }

    // Cliente pide cita: fecha, hora, motivo y vehiculo opcional. La agenda valida horario y turno libre
    @PostMapping("/solicitar-cita")
    public String pedirCitaConfirmar(
            @RequestParam(name = "tallerId", required = false) String tallerId,
            @RequestParam(name = "fecha", required = false) String fecha,
            @RequestParam(name = "hora", required = false) String hora,
            @RequestParam(name = "motivo", required = false) String motivo,
            @RequestParam(name = "cliente", required = false) String cliente,
            @RequestParam(name = "vehiculoId", required = false) String vehiculoId,
            RedirectAttributes ra) {
        if (!Ids.esValido(tallerId)) {
            ra.addFlashAttribute("msgError", "Elige primero un taller para pedir tu cita.");
            return "redirect:/buscar-talleres";
        }
        String uid = SesionActual.usuarioId();
        String nombre = cliente != null && !cliente.isBlank() ? cliente : SesionActual.nombre();
        try {
            agendaService.solicitarCita(tallerId, uid, nombre, motivo, fecha, hora, textoVehiculo(uid, vehiculoId));
            ra.addFlashAttribute("msgOk", "Cita solicitada. El taller la revisará.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            return "redirect:/solicitar-cita?tallerId=" + tallerId;
        }
        return "redirect:/mis-citas";
    }

    // Valorar un taller desde su ficha (una valoracion por cita completada)
    @PostMapping("/taller/{id}/valorar")
    public String valorar(@PathVariable String id,
                          @RequestParam(name = "citaId", required = false) String citaId,
                          @RequestParam(name = "estrellas", required = false, defaultValue = "0") int estrellas,
                          @RequestParam(name = "comentario", required = false) String comentario,
                          RedirectAttributes ra) {
        if (!Ids.esValido(id)) {
            return "redirect:/buscar-talleres";
        }
        try {
            valoracionService.valorar(id, SesionActual.usuarioId(), citaId, estrellas, comentario);
            ra.addFlashAttribute("msgOk", "Gracias por tu valoración.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/detalle-taller?id=" + id + "#valoraciones";
    }

    // "ABC123 · Mazda 3 · 2019" del vehiculo elegido; null si no eligio ninguno o no es suyo
    private String textoVehiculo(String usuarioId, String vehiculoId) {
        if (vehiculoId == null || vehiculoId.isBlank()) {
            return null;
        }
        Perfil perfil = perfilService.obtener(usuarioId);
        if (perfil == null) {
            return null;
        }
        for (Vehiculo v : perfil.getVehiculos()) {
            if (vehiculoId.equals(v.getId())) {
                return v.getPlaca() + " · " + v.getDescripcion();
            }
        }
        return null;
    }
}
