package com.tecnologico.talleres.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// En cada peticion busca el token (cookie o cabecera Bearer); si es valido, deja al usuario autenticado
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extraerToken(request);
        if (token != null) {
            UsuarioAutenticado usuario = jwtService.validar(token);
            if (usuario != null) {
                // ROLE_ADMIN, ROLE_TALLER o ROLE_USER: lo que usa hasRole(...) en SecurityConfig
                var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.rol().toUpperCase());
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of(autoridad));
                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(auth);
                SecurityContextHolder.setContext(contexto);
            }
        }
        chain.doFilter(request, response);
    }

    // Primero Authorization: Bearer xxx (Postman/curl); si no, la cookie del navegador
    private static String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            return cabecera.substring(7).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (JwtService.COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
