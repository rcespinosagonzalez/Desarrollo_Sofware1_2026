package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.services.TallerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas Thymeleaf. El rol admin/user lo marca el JS en el navegador (sessionStorage).
 */
@Controller
public class VistaController {

    private final TallerService tallerService;

    public VistaController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Usuario: mapa + lista de talleres que vienen de la BD (para elegir cuál ver). */
    @GetMapping("/buscar-talleres")
    public String buscarTalleres(Model model) {
        model.addAttribute("talleresBd", tallerService.listarTodos());
        return "buscar-talleres";
    }

    /** Admin: todos los talleres = “clientes” registrados; muestra correo del dueño en BD. */
    @GetMapping("/mi-taller")
    public String miTaller(Model model) {
        model.addAttribute("talleres", tallerService.listarTodos());
        return "mi-taller";
    }

    /**
     * Sin ?id → formulario vacío (alta). Con ?id=3 → edición de ese taller.
     * Importante: en alta NO se envía input hidden id, si no el servidor cree que es edición.
     */
    @GetMapping("/registrar-taller")
    public String registrarTallerForm(@RequestParam(name = "id", required = false) Long id, Model model) {
        Taller taller;
        if (id != null) {
            Taller existente = tallerService.obtenerPorId(id);
            taller = existente != null ? copiarParaFormulario(existente) : new Taller();
        } else {
            taller = new Taller();
        }
        model.addAttribute("taller", taller);
        return "registrar-taller";
    }

    @PostMapping("/registrar-taller")
    public String registrarTallerGuardar(@ModelAttribute Taller taller, RedirectAttributes ra) {
        try {
            tallerService.guardar(taller);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            if (taller.getId() != null) {
                return "redirect:/registrar-taller?id=" + taller.getId();
            }
            return "redirect:/registrar-taller";
        }
        return "redirect:/mi-taller";
    }

    @GetMapping("/valoraciones")
    public String valoraciones(Model model) {
        model.addAttribute("valoraciones", tallerService.listarValoraciones());
        return "valoraciones";
    }

    @GetMapping("/citas-pendientes")
    public String citasPendientes(Model model) {
        model.addAttribute("citas", tallerService.listarCitas());
        return "citas-pendientes";
    }

    @GetMapping("/detalle-taller")
    public String detalleTaller(@RequestParam(name = "id", required = false) Long id, Model model) {
        Taller taller = id != null ? tallerService.obtenerPorId(id) : null;
        if (taller == null) {
            taller = tallerService.obtenerPrincipal();
        }
        model.addAttribute("taller", taller);
        model.addAttribute("estrellasLlenas", estrellasEnteras(taller));
        return "detalle-taller";
    }

    @GetMapping("/calificar-taller")
    public String calificarTaller(Model model) {
        model.addAttribute("ultima", tallerService.obtenerUltimaCalificacion());
        model.addAttribute("nuevaCalificacion", new UltimaCalificacion());
        return "calificar-taller";
    }

    @PostMapping("/calificar-taller")
    public String calificarTallerEnviar(@ModelAttribute UltimaCalificacion calificacion) {
        tallerService.registrarCalificacion(calificacion);
        return "redirect:/calificar-taller";
    }

    @GetMapping("/solicitar-cita")
    public String solicitarCita(@RequestParam(name = "tallerId", required = false) Long tallerId, Model model) {
        Taller taller = tallerId != null ? tallerService.obtenerPorId(tallerId) : tallerService.obtenerPrincipal();
        model.addAttribute("taller", taller);
        return "solicitar-cita";
    }

    @PostMapping("/solicitar-cita")
    public String solicitarCitaConfirmar(
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam String motivo,
            @RequestParam(name = "tallerId", required = false) Long tallerId,
            @RequestParam(name = "cliente", defaultValue = "Cliente web") String cliente) {
        Cita cita = new Cita();
        cita.setTallerId(tallerId);
        cita.setCliente(cliente);
        cita.setDetalle(motivo);
        cita.setFecha(fecha);
        cita.setHora(hora);
        tallerService.agregarCita(cita);
        return "redirect:/buscar-talleres";
    }

    @GetMapping("/historial-busquedas")
    public String historialBusquedas(Model model) {
        model.addAttribute("busquedas", tallerService.listarHistorial());
        return "historial-busquedas";
    }

    private static int estrellasEnteras(Taller taller) {
        if (taller == null) {
            return 0;
        }
        return (int) Math.floor(taller.getCalificacionPromedio());
    }

    private static Taller copiarParaFormulario(Taller origen) {
        Taller t = new Taller();
        t.setId(origen.getId());
        t.setNombre(origen.getNombre());
        t.setDireccion(origen.getDireccion());
        t.setTelefono(origen.getTelefono());
        t.setDescripcion(origen.getDescripcion());
        t.setHorario(origen.getHorario());
        t.setServicios(origen.getServicios());
        t.setCalificacionPromedio(origen.getCalificacionPromedio());
        t.setNumValoraciones(origen.getNumValoraciones());
        return t;
    }
}
