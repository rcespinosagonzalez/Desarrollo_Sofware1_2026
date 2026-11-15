package com.tecnologico.talleres.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

// Reglas de acceso: quien entra a que ruta. Sin sesion en el servidor (STATELESS), solo JWT
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                // CSRF apagado: la cookie del JWT es SameSite=Lax y no viaja en POST de otros sitios
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // El login lo hace AuthController (JSON/cookie), no el formulario de Spring
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                // Cabeceras de seguridad. La CSP permite solo lo que usa la app: Leaflet (unpkg),
                // tiles de OpenStreetMap y Nominatim. Los estilos inline (style="") siguen permitidos.
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(c -> c.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' https://unpkg.com; "
                                        + "style-src 'self' 'unsafe-inline' https://unpkg.com; "
                                        + "img-src 'self' data: https://unpkg.com https://*.tile.openstreetmap.org; "
                                        + "connect-src 'self' https://nominatim.openstreetmap.org; "
                                        + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")))
                .authorizeHttpRequests(auth -> auth
                        // Publico: login, estaticos y errores
                        .requestMatchers("/", "/css/**", "/js/**", "/favicon.ico", "/error",
                                "/logout", "/registro", "/api/auth/login", "/api/auth/registro").permitAll()
                        // Solo administrador
                        .requestMatchers("/mi-taller", "/valoraciones", "/citas-pendientes", "/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // Solo dueno de taller
                        .requestMatchers("/panel-taller", "/panel-taller/**", "/api/taller/**").hasRole("TALLER")
                        // Administrador y dueno de taller
                        .requestMatchers("/registrar-taller").hasAnyRole("ADMIN", "TALLER")
                        // Solo cliente
                        .requestMatchers("/solicitar-cita", "/taller/**", "/mis-citas", "/mis-citas/**", "/perfil", "/perfil/**", "/calificar-taller", "/historial-busquedas",
                                "/api/historial-busqueda").hasRole("USER")
                        // Lo demas (mapa, detalle, /api/talleres): cualquier usuario logueado
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // Sin login: API responde 401, paginas vuelven al inicio
                        .authenticationEntryPoint((request, response, ex) -> {
                            if (esApi(request)) {
                                response.sendError(401);
                            } else {
                                response.sendRedirect(request.getContextPath() + "/");
                            }
                        })
                        // Con login pero rol incorrecto: API responde 403, paginas van a su panel
                        .accessDeniedHandler((request, response, ex) -> {
                            if (esApi(request)) {
                                response.sendError(403);
                            } else {
                                response.sendRedirect(request.getContextPath() + inicioPorRol(SesionActual.rol()));
                            }
                        }))
                // Mi filtro JWT corre antes del filtro de usuario/clave de Spring
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // BCrypt para guardar y comparar contrasenas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Evita que Spring cree el usuario "user" con clave aleatoria en el log; aqui el login es por JWT
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("El acceso es por JWT en /api/auth/login");
        };
    }

    private static boolean esApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    // Pagina de inicio de cada rol (igual que script.js tras el login)
    private static String inicioPorRol(String rol) {
        if ("admin".equals(rol)) {
            return "/mi-taller";
        }
        if ("taller".equals(rol)) {
            return "/panel-taller";
        }
        if ("user".equals(rol)) {
            return "/buscar-talleres";
        }
        return "/";
    }
}
