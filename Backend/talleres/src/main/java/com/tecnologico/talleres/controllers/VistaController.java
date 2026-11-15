package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.FilaHorario;
import com.tecnologico.talleres.model.HorarioDia;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.services.AgendaService;
import com.tecnologico.talleres.services.MecanicoService;
import com.tecnologico.talleres.services.TallerService;
import com.tecnologico.talleres.services.ValoracionService;
import com.tecnologico.talleres.util.HorarioAtencion;
import com.tecnologico.talleres.security.JwtService;
import com.tecnologico.talleres.security.SesionActual;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Paginas HTML (Thymeleaf). Spring Security + JWT ya valido login y rol en SecurityConfig
@Controller
public class VistaController {

    private final TallerService tallerService;
    private final JwtService jwtService;
    private final AgendaService agendaService;
    private final MecanicoService mecanicoService;
    private final ValoracionService valoracionService;

    // Inyeccion de los servicios (talleres, agenda de citas, mecanicos, valoraciones) y del que maneja la cookie del JWT
    public VistaController(TallerService tallerService, JwtService jwtService,
                           AgendaService agendaService, MecanicoService mecanicoService,
                           ValoracionService valoracionService) {
        this.tallerService = tallerService;
        this.jwtService = jwtService;
        this.agendaService = agendaService;
        this.mecanicoService = mecanicoService;
        this.valoracionService = valoracionService;
    }

    // Pantalla de login (publica)
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Pantalla de registro de clientes (publica)
    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    // Borro la cookie del JWT y vuelvo al inicio
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.borrarCookie().toString());
        return "redirect:/";
    }

    // Cliente: mapa con filtros y talleres destacados. Pedir cita y valorar viven en la ficha de cada taller
    @GetMapping("/buscar-talleres")
    public String buscarTalleres(Model model) {
        List<Taller> destacados = new ArrayList<>();
        for (Taller t : tallerService.listarTodos()) {
            if (t.isDestacado()) {
                destacados.add(t);
            }
        }
        // El orden rota en cada visita: ningun taller patrocinado queda siempre primero
        Collections.shuffle(destacados);
        model.addAttribute("destacados", destacados.subList(0, Math.min(6, destacados.size())));
        model.addAttribute("panelCliente", "user".equals(SesionActual.rol()));
        return "buscar-talleres";
    }

    // Admin: tablas de todos los talleres, citas y valoraciones
    @GetMapping("/mi-taller")
    public String miTaller(Model model) {
        String redir = redirectSiNoEsAdmin();
        if (redir != null) {
            return redir;
        }
        var talleres = tallerService.listarTodos();
        model.addAttribute("talleres", talleres);
        model.addAttribute("citas", tallerService.listarTodasCitas());
        model.addAttribute("valoraciones", tallerService.listarTodasValoraciones());
        model.addAttribute("totalTalleres", talleres.size());
        model.addAttribute("nombreAdmin", SesionActual.nombre());
        return "mi-taller";
    }

    // Alta o edicion de taller. El dueno solo puede editar el suyo (id en URL)
    @GetMapping("/registrar-taller")
    public String registrarTallerForm(@RequestParam(name = "id", required = false) String id, Model model) {
        String redir = redirectSiNoPuedeGestionarTaller();
        if (redir != null) {
            return redir;
        }
        String uid = SesionActual.usuarioId();
        boolean esAdmin = "admin".equals(SesionActual.rol());
        boolean esPropietario = "taller".equals(SesionActual.rol());
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
                taller = permitido != null ? copiarParaFormulario(permitido) : tallerNuevo();
            } else {
                taller = tallerNuevo();
            }
        } else {
            taller = tallerNuevo();
        }
        model.addAttribute("taller", taller);
        model.addAttribute("esPropietario", esPropietario);
        model.addAttribute("urlVolver", esPropietario ? "/panel-taller" : "/mi-taller");
        return "registrar-taller";
    }

    // Guardo en MongoDB (taller con su ubicacion y horario, y usuario dueno si es nuevo)
    @PostMapping("/registrar-taller")
    public String registrarTallerGuardar(
            @ModelAttribute Taller taller, RedirectAttributes ra) {
        String redir = redirectSiNoPuedeGestionarTaller();
        if (redir != null) {
            return redir;
        }
        String uid = SesionActual.usuarioId();
        boolean esAdmin = "admin".equals(SesionActual.rol());
        boolean esPropietario = "taller".equals(SesionActual.rol());
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
    public String valoraciones() {
        String redir = redirectSiNoEsAdmin();
        if (redir != null) {
            return redir;
        }
        return "redirect:/mi-taller#seccion-valoraciones";
    }

    @GetMapping("/citas-pendientes")
    public String citasPendientes() {
        String redir = redirectSiNoEsAdmin();
        if (redir != null) {
            return redir;
        }
        return "redirect:/mi-taller#seccion-citas";
    }

    // Dueno del taller: datos del negocio, citas recibidas y valoraciones
    @GetMapping("/panel-taller")
    public String panelTaller(Model model) {
        String redir = redirectSiNoEsTaller();
        if (redir != null) {
            return redir;
        }
        String uid = SesionActual.usuarioId();
        var misTalleres = tallerService.listarTalleresDelPropietario(uid);
        var citas = tallerService.listarCitasPorPropietario(uid);
        var valoraciones = tallerService.listarValoracionesPorPropietario(uid);
        Taller miTaller = misTalleres.isEmpty() ? null : misTalleres.get(0);
        model.addAttribute("miTaller", miTaller);
        model.addAttribute("misTalleres", misTalleres);
        model.addAttribute("mecanicos",
                miTaller != null ? mecanicoService.listarActivos(miTaller.getId()) : java.util.List.of());
        model.addAttribute("citas", citas);
        // Para el dashboard: lo que viene (la mas cercana primero) y lo confirmado cuya hora ya paso (falta completarlo)
        List<Cita> proximas = new ArrayList<>();
        List<Cita> porCompletar = new ArrayList<>();
        for (Cita c : citas) {
            if (c.isEditable()) {
                proximas.add(c);
            } else if ("confirmada".equals(c.getEstado()) && c.isPasada()) {
                porCompletar.add(c);
            }
        }
        Collections.reverse(proximas);
        model.addAttribute("proximasCitas", proximas.subList(0, Math.min(8, proximas.size())));
        model.addAttribute("citasPorCompletar", porCompletar.subList(0, Math.min(8, porCompletar.size())));
        model.addAttribute("valoraciones", valoraciones);
        model.addAttribute("totalCitas", citas.size());
        model.addAttribute("totalValoraciones", valoraciones.size());
        model.addAttribute("nombreUsuario", SesionActual.nombre());
        model.addAttribute(
                "nombreTaller",
                miTaller != null ? miTaller.getNombre() : "Mi taller");
        return "panel-taller";
    }

    // El dueno agrega un mecanico a su taller (cada mecanico atiende una cita por turno)
    @PostMapping("/panel-taller/mecanicos")
    public String crearMecanico(
            @RequestParam String tallerId,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam(required = false) String telefono,
            RedirectAttributes ra) {
        String redir = redirectSiNoEsTaller();
        if (redir != null) {
            return redir;
        }
        try {
            mecanicoService.crear(tallerId, SesionActual.usuarioId(), nombres, apellidos, telefono);
            ra.addFlashAttribute("msgOk", "Mecánico agregado.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/panel-taller#seccion-mecanicos";
    }

    // El dueno da de baja un mecanico (si no tiene citas pendientes y queda al menos uno)
    @PostMapping("/panel-taller/mecanicos/{id}/desactivar")
    public String desactivarMecanico(@PathVariable String id, RedirectAttributes ra) {
        String redir = redirectSiNoEsTaller();
        if (redir != null) {
            return redir;
        }
        try {
            mecanicoService.desactivar(id, SesionActual.usuarioId());
            ra.addFlashAttribute("msgOk", "Mecánico dado de baja.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("msgError", ex.getMessage());
        }
        return "redirect:/panel-taller#seccion-mecanicos";
    }

    // Ficha de un taller (cualquier rol con sesion): datos, horario, valoraciones y, para clientes, pedir cita y valorar.
    // Si el id no existe vuelve al inicio de su rol con un aviso.
    @GetMapping("/detalle-taller")
    public String detalleTaller(@RequestParam(name = "id", required = false) String id, Model model,
                                RedirectAttributes ra) {
        String rol = SesionActual.rol();
        Taller taller = id != null ? tallerService.obtenerPorId(id) : null;
        if (taller == null) {
            ra.addFlashAttribute("msgError", "Ese taller no existe.");
            return "redirect:" + inicioPorRol(rol);
        }
        boolean esCliente = "user".equals(rol);
        List<Valoracion> valoraciones = tallerService.listarValoracionesPorTaller(taller.getId());

        // Cuantas valoraciones hay de 5 a 1 estrellas: {estrellas, cantidad, porcentaje}
        int[] conteo = new int[6];
        for (Valoracion v : valoraciones) {
            if (v.getEstrellas() >= 1 && v.getEstrellas() <= 5) {
                conteo[v.getEstrellas()]++;
            }
        }
        List<int[]> distribucion = new ArrayList<>();
        for (int e = 5; e >= 1; e--) {
            int pct = valoraciones.isEmpty() ? 0 : (int) Math.round(100.0 * conteo[e] / valoraciones.size());
            distribucion.add(new int[] {e, conteo[e], pct});
        }

        List<String> servicios = new ArrayList<>();
        if (taller.getServicios() != null) {
            for (String sv : taller.getServicios().split("\\s+-\\s+|\\s*[,;|]\\s*")) {
                if (!sv.isBlank()) {
                    servicios.add(sv.trim());
                }
            }
        }

        model.addAttribute("taller", taller);
        model.addAttribute("estrellasLlenas", estrellasEnteras(taller));
        model.addAttribute("valoracionesTaller", valoraciones);
        model.addAttribute("distribucion", distribucion);
        model.addAttribute("serviciosLista", servicios);
        model.addAttribute("filasHorario", filasHorario(taller));
        model.addAttribute("abiertoAhora", abiertoAhora(taller));
        model.addAttribute("esCliente", esCliente);
        model.addAttribute("urlVolver", inicioPorRol(rol));
        model.addAttribute("textoVolver", esCliente ? "Mapa" : "Panel");
        if (esCliente) {
            model.addAttribute("citasPorValorar", valoracionService.citasPorValorar(taller.getId(), SesionActual.usuarioId()));
            model.addAttribute("exigeCita", valoracionService.exigeCitaCompletada());
        }
        return "detalle-taller";
    }

    // Antes el cliente valoraba desde la pantalla principal; ahora se valora en la ficha de cada taller
    @GetMapping("/calificar-taller")
    public String calificarTaller() {
        return "redirect:/buscar-talleres";
    }

    // Atajo de menu al historial en buscar-talleres
    @GetMapping("/historial-busquedas")
    public String historialBusquedas() {
        String redir = redirectSiNoEsCliente();
        if (redir != null) {
            return redir;
        }
        return "redirect:/buscar-talleres";
    }

    // Pantalla de inicio de cada rol
    private static String inicioPorRol(Object rol) {
        if ("admin".equals(rol)) {
            return "/mi-taller";
        }
        if ("taller".equals(rol)) {
            return "/panel-taller";
        }
        return "/buscar-talleres";
    }

    // Una fila por dia (lunes a domingo) para la tabla de horarios de la ficha
    private static List<FilaHorario> filasHorario(Taller taller) {
        String[] nombres = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        int hoy = LocalDate.now().getDayOfWeek().getValue();
        List<FilaHorario> filas = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            HorarioDia h = horarioDelDia(taller, d);
            filas.add(new FilaHorario(nombres[d - 1], h == null ? "Cerrado" : h.apertura() + " – " + h.cierre(),
                    d == hoy, h == null));
        }
        return filas;
    }

    private static HorarioDia horarioDelDia(Taller taller, int dia) {
        for (HorarioDia h : taller.getHorarios()) {
            if (h.dia() == dia) {
                return h;
            }
        }
        return null;
    }

    // true si ahora mismo cae dentro del horario de hoy
    private static boolean abiertoAhora(Taller taller) {
        LocalDateTime ahora = LocalDateTime.now();
        HorarioDia h = horarioDelDia(taller, ahora.getDayOfWeek().getValue());
        if (h == null) {
            return false;
        }
        int ap = minutos(h.apertura());
        int ci = minutos(h.cierre());
        int actual = ahora.getHour() * 60 + ahora.getMinute();
        return ap >= 0 && ci >= 0 && actual >= ap && actual < ci;
    }

    // "08:30" -> 510; -1 si no se entiende
    private static int minutos(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) {
            return -1;
        }
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    // Si no soy admin, mando al panel que corresponda a mi rol
    private static String redirectSiNoEsAdmin() {
        Object rol = SesionActual.rol();
        if ("admin".equals(rol)) {
            return null;
        }
        if ("taller".equals(rol)) {
            return "redirect:/panel-taller";
        }
        return "redirect:/buscar-talleres";
    }

    // Solo admin y rol taller entran a registrar-taller
    private static String redirectSiNoPuedeGestionarTaller() {
        Object rol = SesionActual.rol();
        if ("admin".equals(rol) || "taller".equals(rol)) {
            return null;
        }
        if ("user".equals(rol)) {
            return "redirect:/buscar-talleres";
        }
        return "redirect:/";
    }

    // Citas y valoraciones solo para rol user (cliente)
    private static String redirectSiNoEsCliente() {
        Object rol = SesionActual.rol();
        if ("taller".equals(rol)) {
            return "redirect:/panel-taller";
        }
        if (!"user".equals(rol)) {
            return "redirect:/mi-taller";
        }
        return null;
    }

    // panel-taller solo para rol taller
    private static String redirectSiNoEsTaller() {
        if (!"taller".equals(SesionActual.rol())) {
            if ("admin".equals(SesionActual.rol())) {
                return "redirect:/mi-taller";
            }
            if ("user".equals(SesionActual.rol())) {
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

    // Taller en blanco para el formulario de alta, con un horario sugerido (Lun - Sab 8:00 a 18:00)
    private static Taller tallerNuevo() {
        Taller t = new Taller();
        t.setHorarios(HorarioAtencion.desdeFormulario(java.util.List.of(1, 2, 3, 4, 5, 6), "08:00", "18:00"));
        return t;
    }

    // Copio a un objeto nuevo para no mezclar datos del listado con el formulario
    private static Taller copiarParaFormulario(Taller origen) {
        Taller t = new Taller();
        t.setId(origen.getId());
        t.setNombre(origen.getNombre());
        t.setDireccion(origen.getDireccion());
        t.setTelefono(origen.getTelefono());
        t.setDescripcion(origen.getDescripcion());
        t.setHorarios(origen.getHorarios());
        t.setServicios(origen.getServicios());
        t.setCiudad(origen.getCiudad());
        t.setLat(origen.getLat());
        t.setLng(origen.getLng());
        t.setCalificacionPromedio(origen.getCalificacionPromedio());
        t.setNumValoraciones(origen.getNumValoraciones());
        return t;
    }
}
