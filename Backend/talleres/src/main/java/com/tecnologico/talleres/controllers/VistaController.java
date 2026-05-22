package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.services.TallerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Paginas HTML (Thymeleaf). El interceptor ya valido sesion en la mayoria de rutas
@Controller
public class VistaController {

    private final TallerService tallerService;

    // Inyeccion del servicio que habla con MySQL
    public VistaController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    // Pantalla de login (publica)
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Borro la sesion y vuelvo al inicio
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // Cliente: mapa, historial, pedir cita y valorar. tallerCita = taller ya elegido para la cita
    @GetMapping("/buscar-talleres")
    public String buscarTalleres(
            @RequestParam(name = "tallerCita", required = false) Long tallerCita,
            Model model,
            HttpSession session) {
        model.addAttribute("talleresBd", tallerService.listarTodos());
        boolean esCliente = "user".equals(session.getAttribute("rol"));
        model.addAttribute("panelCliente", esCliente);
        if (esCliente) {
            long uid = (Long) session.getAttribute("usuarioId");
            model.addAttribute("ultima", tallerService.obtenerUltimaCalificacion(uid));
            model.addAttribute("nuevaCalificacion", new UltimaCalificacion());
            model.addAttribute("citasCliente", tallerService.listarCitasPorCliente(uid));
            model.addAttribute(
                    "tallerParaCita", tallerCita != null ? tallerService.obtenerPorId(tallerCita) : null);
        }
        return "buscar-talleres";
    }

    // Admin: tablas de todos los talleres, citas y valoraciones
    @GetMapping("/mi-taller")
    public String miTaller(Model model, HttpSession session) {
        String redir = redirectSiNoEsAdmin(session);
        if (redir != null) {
            return redir;
        }
        var talleres = tallerService.listarTodos();
        model.addAttribute("talleres", talleres);
        model.addAttribute("citas", tallerService.listarTodasCitas());
        model.addAttribute("valoraciones", tallerService.listarTodasValoraciones());
        model.addAttribute("totalTalleres", talleres.size());
        model.addAttribute("nombreAdmin", session.getAttribute("nombre"));
        return "mi-taller";
    }

    // Alta o edicion de taller. El dueno solo puede editar el suyo (id en URL)
    @GetMapping("/registrar-taller")
    public String registrarTallerForm(@RequestParam(name = "id", required = false) Long id, Model model, HttpSession session) {
        String redir = redirectSiNoPuedeGestionarTaller(session);
        if (redir != null) {
            return redir;
        }
        long uid = (Long) session.getAttribute("usuarioId");
        boolean esAdmin = "admin".equals(session.getAttribute("rol"));
        boolean esPropietario = "taller".equals(session.getAttribute("rol"));
        if (esPropietario && id == null) {
            var propios = tallerService.listarTalleresDelPropietario(uid);
            if (!propios.isEmpty()) {
                return "redirect:/registrar-taller?id=" + propios.get(0).getId();
            }
            return "redirect:/panel-taller";
        }
        Taller taller;
        if (id != null) {
            Taller existente = tallerService.obtenerPorId(id);
            if (existente != null && esAdmin) {
                taller = copiarParaFormulario(existente);
            } else if (existente != null) {
                Taller permitido = null;
                for (Taller t : tallerService.listarTalleresDelPropietario(uid)) {
                    if (t.getId().equals(id)) {
                        permitido = t;
                        break;
                    }
                }
                taller = permitido != null ? copiarParaFormulario(permitido) : new Taller();
            } else {
                taller = new Taller();
            }
        } else {
            taller = new Taller();
        }
        model.addAttribute("taller", taller);
        model.addAttribute("esPropietario", esPropietario);
        model.addAttribute("urlVolver", esPropietario ? "/panel-taller" : "/mi-taller");
        return "registrar-taller";
    }

    // Guardo en MySQL (taller + ubicacion + usuario dueno si es nuevo)
    @PostMapping("/registrar-taller")
    public String registrarTallerGuardar(
            @ModelAttribute Taller taller, RedirectAttributes ra, HttpSession session) {
        String redir = redirectSiNoPuedeGestionarTaller(session);
        if (redir != null) {
            return redir;
        }
        long uid = (Long) session.getAttribute("usuarioId");
        boolean esAdmin = "admin".equals(session.getAttribute("rol"));
        boolean esPropietario = "taller".equals(session.getAttribute("rol"));
        if (esPropietario && taller.getId() == null) {
            ra.addFlashAttribute("msgError", "Solo puedes editar tu taller registrado.");
            return "redirect:/panel-taller";
        }
        try {
            tallerService.guardar(taller, uid, esAdmin);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
            if (taller.getId() != null) {
                return "redirect:/registrar-taller?id=" + taller.getId();
            }
            return esPropietario ? "redirect:/panel-taller" : "redirect:/registrar-taller";
        }
        ra.addFlashAttribute("msgOk", taller.getId() != null ? "Taller actualizado." : "Taller registrado. Ya aparece en el mapa.");
        if (esPropietario) {
            return "redirect:/panel-taller#seccion-mi-taller";
        }
        return "redirect:/mi-taller#seccion-talleres";
    }

    // Atajo del menu admin hacia la seccion valoraciones en mi-taller
    @GetMapping("/valoraciones")
    public String valoraciones(HttpSession session) {
        String redir = redirectSiNoEsAdmin(session);
        if (redir != null) {
            return redir;
        }
        return "redirect:/mi-taller#seccion-valoraciones";
    }

    @GetMapping("/citas-pendientes")
    public String citasPendientes(HttpSession session) {
        String redir = redirectSiNoEsAdmin(session);
        if (redir != null) {
            return redir;
        }
        return "redirect:/mi-taller#seccion-citas";
    }

    // Dueno del taller: datos del negocio, citas recibidas y valoraciones
    @GetMapping("/panel-taller")
    public String panelTaller(Model model, HttpSession session) {
        String redir = redirectSiNoEsTaller(session);
        if (redir != null) {
            return redir;
        }
        long uid = (Long) session.getAttribute("usuarioId");
        var misTalleres = tallerService.listarTalleresDelPropietario(uid);
        var citas = tallerService.listarCitasPorPropietario(uid);
        var valoraciones = tallerService.listarValoracionesPorPropietario(uid);
        Taller miTaller = misTalleres.isEmpty() ? null : misTalleres.get(0);
        model.addAttribute("miTaller", miTaller);
        model.addAttribute("misTalleres", misTalleres);
        model.addAttribute("citas", citas);
        model.addAttribute("valoraciones", valoraciones);
        model.addAttribute("totalCitas", citas.size());
        model.addAttribute("totalValoraciones", valoraciones.size());
        model.addAttribute("nombreUsuario", session.getAttribute("nombre"));
        model.addAttribute(
                "nombreTaller",
                miTaller != null ? miTaller.getNombre() : "Mi taller");
        return "panel-taller";
    }

    // Ficha publica de un taller (sin id uso el ultimo de la BD)
    @GetMapping("/detalle-taller")
    public String detalleTaller(@RequestParam(name = "id", required = false) Long id, Model model) {
        Taller taller = id != null ? tallerService.obtenerPorId(id) : null;
        if (taller == null) {
            taller = tallerService.obtenerPrincipal();
        }
        model.addAttribute("taller", taller);
        model.addAttribute("estrellasLlenas", estrellasEnteras(taller));
        if (taller != null && taller.getId() != null) {
            model.addAttribute("valoracionesTaller", tallerService.listarValoracionesPorTaller(taller.getId()));
        }
        return "detalle-taller";
    }

    // Redirige al panel cliente en seccion calificar
    @GetMapping("/calificar-taller")
    public String calificarTaller(HttpSession session) {
        String redir = redirectSiNoEsCliente(session);
        if (redir != null) {
            return redir;
        }
        return "redirect:/buscar-talleres?ver=calificar";
    }

    // Guardo estrellas y comentario en tabla valoracion
    @PostMapping("/calificar-taller")
    public String calificarTallerEnviar(
            @ModelAttribute UltimaCalificacion calificacion,
            HttpSession session,
            RedirectAttributes ra) {
        String redir = redirectSiNoEsCliente(session);
        if (redir != null) {
            return redir;
        }
        if (calificacion == null
                || calificacion.getTallerNombre() == null
                || calificacion.getTallerNombre().isBlank()) {
            ra.addFlashAttribute("msgError", "Falta nombre del taller.");
            return "redirect:/buscar-talleres?ver=calificar";
        }
        long uid = (Long) session.getAttribute("usuarioId");
        tallerService.registrarCalificacion(calificacion, uid);
        ra.addFlashAttribute("msgOk", "Valoración guardada.");
        return "redirect:/buscar-talleres?ver=calificar";
    }

    // Abre formulario de cita con tallerId opcional en query
    @GetMapping("/solicitar-cita")
    public String solicitarCita(@RequestParam(name = "tallerId", required = false) Long tallerId, HttpSession session) {
        String redir = redirectSiNoEsCliente(session);
        if (redir != null) {
            return redir;
        }
        if (tallerId != null) {
            return "redirect:/buscar-talleres?tallerCita=" + tallerId + "&ver=solicitar";
        }
        return "redirect:/buscar-talleres?ver=solicitar";
    }

    // Cliente pide cita: fecha, hora, motivo y taller opcional
    @PostMapping("/solicitar-cita")
    public String solicitarCitaConfirmar(
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam String motivo,
            @RequestParam(name = "tallerId", required = false) Long tallerId,
            @RequestParam(name = "cliente", required = false) String cliente,
            HttpSession session) {
        String redir = redirectSiNoEsCliente(session);
        if (redir != null) {
            return redir;
        }
        long uid = (Long) session.getAttribute("usuarioId");
        String nombreSesion = (String) session.getAttribute("nombre");
        String nombreCliente = cliente != null && !cliente.isBlank() ? cliente : nombreSesion;
        if (nombreCliente == null || nombreCliente.isBlank()) {
            nombreCliente = "Cliente web";
        }
        Cita cita = new Cita();
        cita.setTallerId(tallerId);
        cita.setCliente(nombreCliente);
        cita.setDetalle(motivo);
        cita.setFecha(fecha);
        cita.setHora(hora);
        tallerService.agregarCita(cita, uid);
        return "redirect:/buscar-talleres?ver=citas";
    }

    // Atajo de menu al historial en buscar-talleres
    @GetMapping("/historial-busquedas")
    public String historialBusquedas(HttpSession session) {
        String redir = redirectSiNoEsCliente(session);
        if (redir != null) {
            return redir;
        }
        return "redirect:/buscar-talleres?ver=historial";
    }

    // Si no soy admin, mando al panel que corresponda a mi rol
    private static String redirectSiNoEsAdmin(HttpSession session) {
        Object rol = session.getAttribute("rol");
        if ("admin".equals(rol)) {
            return null;
        }
        if ("taller".equals(rol)) {
            return "redirect:/panel-taller";
        }
        return "redirect:/buscar-talleres";
    }

    // Solo admin y rol taller entran a registrar-taller
    private static String redirectSiNoPuedeGestionarTaller(HttpSession session) {
        Object rol = session.getAttribute("rol");
        if ("admin".equals(rol) || "taller".equals(rol)) {
            return null;
        }
        if ("user".equals(rol)) {
            return "redirect:/buscar-talleres";
        }
        return "redirect:/";
    }

    // Citas y valoraciones solo para rol user (cliente)
    private static String redirectSiNoEsCliente(HttpSession session) {
        Object rol = session.getAttribute("rol");
        if ("taller".equals(rol)) {
            return "redirect:/panel-taller";
        }
        if (!"user".equals(rol)) {
            return "redirect:/mi-taller";
        }
        return null;
    }

    // panel-taller solo para rol taller
    private static String redirectSiNoEsTaller(HttpSession session) {
        if (!"taller".equals(session.getAttribute("rol"))) {
            if ("admin".equals(session.getAttribute("rol"))) {
                return "redirect:/mi-taller";
            }
            if ("user".equals(session.getAttribute("rol"))) {
                return "redirect:/buscar-talleres";
            }
            return "redirect:/";
        }
        return null;
    }

    // Cuantas estrellas llenas pintar en detalle-taller
    private static int estrellasEnteras(Taller taller) {
        if (taller == null) {
            return 0;
        }
        return (int) Math.floor(taller.getCalificacionPromedio());
    }

    // Copio a un objeto nuevo para no mezclar datos del listado con el formulario
    private static Taller copiarParaFormulario(Taller origen) {
        Taller t = new Taller();
        t.setId(origen.getId());
        t.setNombre(origen.getNombre());
        t.setDireccion(origen.getDireccion());
        t.setTelefono(origen.getTelefono());
        t.setDescripcion(origen.getDescripcion());
        t.setHorario(origen.getHorario());
        t.setServicios(origen.getServicios());
        t.setCiudad(origen.getCiudad());
        t.setLat(origen.getLat());
        t.setLng(origen.getLng());
        t.setCalificacionPromedio(origen.getCalificacionPromedio());
        t.setNumValoraciones(origen.getNumValoraciones());
        return t;
    }
}
