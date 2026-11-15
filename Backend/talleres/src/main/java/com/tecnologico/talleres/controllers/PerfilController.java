package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Perfil;
import com.tecnologico.talleres.security.JwtService;
import com.tecnologico.talleres.security.SesionActual;
import com.tecnologico.talleres.services.PerfilService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Year;

// Perfil del cliente: datos personales, seguridad (clave y correo), vehiculos y baja de la cuenta.
// SecurityConfig ya limita /perfil/** al rol USER. Cada accion vuelve a /perfil#seccion con su mensaje,
// y la pagina lo muestra dentro de la tarjeta que corresponde.
@Controller
public class PerfilController {

    private final PerfilService perfilService;
    private final JwtService jwtService;

    public PerfilController(PerfilService perfilService, JwtService jwtService) {
        this.perfilService = perfilService;
        this.jwtService = jwtService;
    }

    @GetMapping("/perfil")
    public String ver(Model model, HttpServletResponse response) {
        Perfil perfil = perfilService.obtener(SesionActual.usuarioId());
        if (perfil == null) {
            // La cuenta ya no existe o esta desactivada: cierro la sesion
            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.borrarCookie().toString());
            return "redirect:/";
        }
        model.addAttribute("perfil", perfil);
        model.addAttribute("anioMaximo", Year.now().getValue() + 1);
        return "perfil";
    }

    @PostMapping("/perfil/datos")
    public String guardarDatos(
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "telefono", required = false) String telefono,
            @RequestParam(name = "ciudad", required = false) String ciudad,
            HttpServletResponse response,
            RedirectAttributes ra) {
        try {
            String guardado = perfilService.actualizarDatos(SesionActual.usuarioId(), nombre, telefono, ciudad);
            // El nombre viaja dentro del JWT: lo emito de nuevo para que la sesion use el nombre nuevo
            String token = jwtService.generarToken(SesionActual.usuarioId(), guardado, SesionActual.rol());
            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.crearCookie(token).toString());
            return ok(ra, "datos", "Datos actualizados.");
        } catch (IllegalArgumentException ex) {
            return error(ra, "datos", ex.getMessage());
        }
    }

    @PostMapping("/perfil/clave")
    public String cambiarClave(
            @RequestParam(name = "actual", required = false) String actual,
            @RequestParam(name = "nueva", required = false) String nueva,
            @RequestParam(name = "confirmacion", required = false) String confirmacion,
            RedirectAttributes ra) {
        try {
            perfilService.cambiarClave(SesionActual.usuarioId(), actual, nueva, confirmacion);
            return ok(ra, "seguridad", "Contraseña actualizada.");
        } catch (IllegalArgumentException ex) {
            return error(ra, "seguridad", ex.getMessage());
        }
    }

    @PostMapping("/perfil/correo")
    public String cambiarCorreo(
            @RequestParam(name = "correo", required = false) String correo,
            @RequestParam(name = "clave", required = false) String clave,
            RedirectAttributes ra) {
        try {
            perfilService.cambiarCorreo(SesionActual.usuarioId(), correo, clave);
            return ok(ra, "seguridad", "Correo actualizado. Úsalo la próxima vez que inicies sesión.");
        } catch (IllegalArgumentException ex) {
            return error(ra, "seguridad", ex.getMessage());
        }
    }

    @PostMapping("/perfil/vehiculos")
    public String agregarVehiculo(
            @RequestParam(name = "placa", required = false) String placa,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "marca", required = false) String marca,
            @RequestParam(name = "modelo", required = false) String modelo,
            @RequestParam(name = "anio", required = false) String anio,
            @RequestParam(name = "color", required = false) String color,
            RedirectAttributes ra) {
        try {
            perfilService.agregarVehiculo(SesionActual.usuarioId(), placa, tipo, marca, modelo, anio, color);
            return ok(ra, "vehiculos", "Vehículo guardado.");
        } catch (IllegalArgumentException ex) {
            return error(ra, "vehiculos", ex.getMessage());
        }
    }

    @PostMapping("/perfil/vehiculos/{id}/eliminar")
    public String eliminarVehiculo(@PathVariable String id, RedirectAttributes ra) {
        try {
            perfilService.eliminarVehiculo(SesionActual.usuarioId(), id);
            return ok(ra, "vehiculos", "Vehículo eliminado.");
        } catch (IllegalArgumentException ex) {
            return error(ra, "vehiculos", ex.getMessage());
        }
    }

    @PostMapping("/perfil/cuenta/eliminar")
    public String eliminarCuenta(
            @RequestParam(name = "clave", required = false) String clave,
            HttpServletResponse response,
            RedirectAttributes ra) {
        try {
            perfilService.desactivarCuenta(SesionActual.usuarioId(), clave);
        } catch (IllegalArgumentException ex) {
            return error(ra, "cuenta", ex.getMessage());
        }
        // Cuenta desactivada: borro la cookie y vuelvo al inicio
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.borrarCookie().toString());
        return "redirect:/";
    }

    private static String ok(RedirectAttributes ra, String seccion, String mensaje) {
        ra.addFlashAttribute("msgOk", mensaje);
        ra.addFlashAttribute("seccion", seccion);
        return "redirect:/perfil#" + seccion;
    }

    private static String error(RedirectAttributes ra, String seccion, String mensaje) {
        ra.addFlashAttribute("msgError", mensaje);
        ra.addFlashAttribute("seccion", seccion);
        return "redirect:/perfil#" + seccion;
    }
}
