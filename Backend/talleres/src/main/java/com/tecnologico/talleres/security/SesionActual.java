package com.tecnologico.talleres.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Atajo para saber quien esta logueado en esta peticion (reemplaza a session.getAttribute)
public final class SesionActual {

    private SesionActual() {}

    // Usuario puesto por JwtAuthenticationFilter; null si no hay login
    public static UsuarioAutenticado usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return u;
        }
        return null;
    }

    public static Long usuarioId() {
        UsuarioAutenticado u = usuario();
        return u == null ? null : u.id();
    }

    public static String rol() {
        UsuarioAutenticado u = usuario();
        return u == null ? null : u.rol();
    }

    public static String nombre() {
        UsuarioAutenticado u = usuario();
        return u == null ? null : u.nombre();
    }
}
