package com.tecnologico.talleres.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pruebas unitarias de JwtAuthenticationFilter con Mockito: simulo JwtService, la peticion, la respuesta y la cadena de filtros
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    // Mockito crea el filtro llamando a new JwtAuthenticationFilter(jwtService) con el @Mock de arriba
    @InjectMocks
    private JwtAuthenticationFilter filtro;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
    }

    // Dejo el contexto limpio para que una prueba no afecte a la siguiente
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Con token valido en la cookie autentica al usuario y sigue la cadena")
    void tokenValidoEnCookie_autenticaYContinua() throws Exception {
        UsuarioAutenticado usuario = new UsuarioAutenticado(1, "Admin Demo", "admin");
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(JwtService.COOKIE, "token-cookie")});
        when(jwtService.validar("token-cookie")).thenReturn(usuario);

        filtro.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(usuario, auth.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Con cabecera Authorization: Bearer usa ese token")
    void tokenValidoEnCabeceraBearer_autentica() throws Exception {
        UsuarioAutenticado usuario = new UsuarioAutenticado(2, "Cliente Web", "user");
        when(request.getHeader("Authorization")).thenReturn("Bearer token-bearer");
        when(jwtService.validar("token-bearer")).thenReturn(usuario);

        filtro.doFilterInternal(request, response, chain);

        assertEquals(usuario, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Si la cabecera no es Bearer, usa la cookie")
    void cabeceraQueNoEsBearer_usaLaCookie() throws Exception {
        UsuarioAutenticado usuario = new UsuarioAutenticado(3, "Dueno El Rapido", "taller");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(JwtService.COOKIE, "token-cookie")});
        when(jwtService.validar("token-cookie")).thenReturn(usuario);

        filtro.doFilterInternal(request, response, chain);

        assertEquals(usuario, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @ParameterizedTest(name = "rol {0} -> autoridad {1}")
    @CsvSource({"admin,ROLE_ADMIN", "taller,ROLE_TALLER", "user,ROLE_USER"})
    @DisplayName("La autoridad se arma como ROLE_ + rol en mayusculas")
    void autoridadPorRol(String rol, String autoridadEsperada) throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(JwtService.COOKIE, "t")});
        when(jwtService.validar("t")).thenReturn(new UsuarioAutenticado(9, "Alguien", rol));

        filtro.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(autoridadEsperada, auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Con token invalido no autentica, pero la cadena continua")
    void tokenInvalido_noAutenticaYContinua() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(JwtService.COOKIE, "token-malo")});
        when(jwtService.validar("token-malo")).thenReturn(null);

        filtro.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Sin token no consulta a JwtService y la cadena continua")
    void sinToken_noLlamaAlServicio() throws Exception {
        when(request.getCookies()).thenReturn(null);

        filtro.doFilterInternal(request, response, chain);

        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Una cookie con otro nombre se ignora")
    void cookieConOtroNombre_seIgnora() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("otra", "valor")});

        filtro.doFilterInternal(request, response, chain);

        verify(jwtService, org.mockito.Mockito.never()).validar(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
