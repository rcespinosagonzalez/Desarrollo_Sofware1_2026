package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.model.Usuario;
import com.tecnologico.talleres.services.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Usuario usuario = usuarioService.findByEmail(email).orElse(null);

        if (usuario == null || !usuario.getPassword().equals(password)) {
            model.addAttribute("errorLogin", "Correo o contraseña incorrectos.");
            return "index";
        }

        session.setAttribute("role", usuario.getRol());
        session.setAttribute("nombre", usuario.getNombre());

        return usuario.getRol().equals("admin")
                ? "redirect:/mi-taller"
                : "redirect:/buscar-talleres";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}