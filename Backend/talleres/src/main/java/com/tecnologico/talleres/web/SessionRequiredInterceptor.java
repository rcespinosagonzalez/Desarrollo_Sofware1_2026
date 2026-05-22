package com.tecnologico.talleres.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Bloqueo rutas sin login: si no hay usuarioId en sesion, mando al inicio
@Component
public class SessionRequiredInterceptor implements HandlerInterceptor {

    // Rutas que no piden sesion: login, assets, logout, error
    private static boolean isPublic(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
            if (uri.isEmpty()) {
                uri = "/";
            }
        }
        if (uri.equals("/")) {
            return true;
        }
        if (uri.startsWith("/css/") || uri.startsWith("/js/")) {
            return true;
        }
        if (uri.equals("/api/auth/login")) {
            return true;
        }
        if (uri.equals("/logout")) {
            return true;
        }
        if (uri.equals("/error")) {
            return true;
        }
        return uri.endsWith(".ico");
    }

    // Se ejecuta antes de cada controlador
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (isPublic(request)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        return true;
    }
}
