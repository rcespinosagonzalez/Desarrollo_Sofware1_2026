package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Usuario;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.services.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VistaController {

    private final TallerService tallerService;
    private final UsuarioService usuarioService;

    public VistaController(TallerService tallerService, UsuarioService usuarioService) {
        this.tallerService = tallerService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/buscar-talleres")
    public String buscarTalleres() {
        return "buscar-talleres";
    }

    @GetMapping("/mi-taller")
    public String miTaller(Model model) {
        Taller taller = tallerService.obtenerPrincipal();
        model.addAttribute("taller", taller);
        model.addAttribute("estrellasLlenas", estrellasEnteras(taller));
        return "mi-taller";
    }

    @GetMapping("/registrar-taller")
    public String registrarTallerForm(Model model) {
        Taller taller = tallerService.obtenerPrincipal();
        if (taller == null) {
            taller = new Taller();
        } else {
            taller = copiarParaFormulario(taller);
        }
        model.addAttribute("taller", taller);
        return "registrar-taller";
    }

    @PostMapping("/registrar-taller")
    public String registrarTallerGuardar(@ModelAttribute Taller taller) {
        tallerService.guardar(taller);
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

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "Registro-usuario";
    }

    @PostMapping("/registro")
    public String registroGuardar(@ModelAttribute Usuario usuario, Model model) {
        try {
            usuarioService.save(usuario);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
            return "Registro-usuario";
        }
    }

    private static int estrellasEnteras(Taller taller) {
        if (taller == null) return 0;
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