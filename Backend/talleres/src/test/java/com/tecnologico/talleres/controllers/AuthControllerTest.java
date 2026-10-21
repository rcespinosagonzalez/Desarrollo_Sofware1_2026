package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.dto.LoginResponse;
import com.tecnologico.talleres.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pruebas unitarias de AuthController.login con Mockito: simulo la base de datos (JdbcTemplate), BCrypt y JwtService
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AuthControllerTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    // Mockito crea el controlador llamando a new AuthController(jdbc, passwordEncoder, jwtService)
    @InjectMocks
    private AuthController controller;

    // Simula que MySQL encuentra a este usuario: el RowMapper real del controlador lee un ResultSet falso
    private void dadoUsuarioEnBd(String email, long id, String nombre, String rol, String hash) {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(email))).thenAnswer(invocacion -> {
            RowMapper<?> mapper = invocacion.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(id);
            when(rs.getString("nombre")).thenReturn(nombre);
            when(rs.getString("rol")).thenReturn(rol);
            when(rs.getString("password_hash")).thenReturn(hash);
            return List.of(mapper.mapRow(rs, 0));
        });
    }

    @Test
    @DisplayName("Con correo vacio pide completar los campos y no toca la BD")
    void correoVacio_pideCompletarCampos() {
        ResponseEntity<LoginResponse> resp = controller.login("  ", "demo");

        assertFalse(resp.getBody().ok());
        assertTrue(resp.getBody().mensaje().contains("Completa correo"));
        verifyNoInteractions(jdbc, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Con clave vacia pide completar los campos y no toca la BD")
    void claveVacia_pideCompletarCampos() {
        ResponseEntity<LoginResponse> resp = controller.login("admin@demo.local", "");

        assertFalse(resp.getBody().ok());
        verifyNoInteractions(jdbc, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Si el correo no existe responde incorrecto y no compara claves ni crea token")
    void correoQueNoExiste_respondeIncorrecto() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("nadie@demo.local"))).thenReturn(List.of());

        ResponseEntity<LoginResponse> resp = controller.login("nadie@demo.local", "demo");

        assertFalse(resp.getBody().ok());
        assertTrue(resp.getBody().mensaje().contains("incorrectos"));
        assertNull(resp.getBody().token());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Con clave incorrecta responde incorrecto y NO genera token")
    void claveIncorrecta_noGeneraToken() {
        dadoUsuarioEnBd("admin@demo.local", 1, "Admin Demo", "admin", "$2a$hash");
        when(passwordEncoder.matches("mala", "$2a$hash")).thenReturn(false);

        ResponseEntity<LoginResponse> resp = controller.login("admin@demo.local", "mala");

        assertFalse(resp.getBody().ok());
        assertTrue(resp.getBody().mensaje().contains("incorrectos"));
        verify(jwtService, never()).generarToken(anyLong(), anyString(), anyString());
        assertNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("Con datos correctos devuelve los datos del usuario, el token y la cookie")
    void datosCorrectos_devuelveTokenYCookie() {
        dadoUsuarioEnBd("admin@demo.local", 1, "Admin Demo", "admin", "$2a$hash");
        when(passwordEncoder.matches("demo", "$2a$hash")).thenReturn(true);
        when(jwtService.generarToken(1L, "Admin Demo", "admin")).thenReturn("token-falso");
        when(jwtService.crearCookie("token-falso"))
                .thenReturn(ResponseCookie.from(JwtService.COOKIE, "token-falso").build());

        ResponseEntity<LoginResponse> resp = controller.login("admin@demo.local", "demo");

        LoginResponse body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.ok());
        assertEquals("Admin Demo", body.nombre());
        assertEquals("admin", body.rol());
        assertEquals(Long.valueOf(1), body.usuarioId());
        assertEquals("token-falso", body.token());
        assertTrue(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("TALLERES_JWT=token-falso"));
    }

    @Test
    @DisplayName("Quita los espacios del correo antes de consultar la BD")
    void correoConEspacios_seRecorta() {
        dadoUsuarioEnBd("cliente@demo.local", 2, "Cliente Web", "user", "$2a$hash");
        when(passwordEncoder.matches("demo", "$2a$hash")).thenReturn(true);
        when(jwtService.generarToken(2L, "Cliente Web", "user")).thenReturn("t");
        when(jwtService.crearCookie("t")).thenReturn(ResponseCookie.from(JwtService.COOKIE, "t").build());

        ResponseEntity<LoginResponse> resp = controller.login("  cliente@demo.local ", "demo");

        assertTrue(resp.getBody().ok());
    }

    @ParameterizedTest(name = "rol en BD {0} -> rol del token {1}")
    @CsvSource({"ADMIN,admin", "Taller,taller", "user,user", "cliente,user", "desconocido,user"})
    @DisplayName("El rol de la BD se normaliza a admin, taller o user")
    void rolSeNormaliza(String rolEnBd, String rolEsperado) {
        dadoUsuarioEnBd("x@demo.local", 5, "Alguien", rolEnBd, "$2a$hash");
        when(passwordEncoder.matches("demo", "$2a$hash")).thenReturn(true);
        when(jwtService.generarToken(5L, "Alguien", rolEsperado)).thenReturn("t");
        when(jwtService.crearCookie("t")).thenReturn(ResponseCookie.from(JwtService.COOKIE, "t").build());

        ResponseEntity<LoginResponse> resp = controller.login("x@demo.local", "demo");

        assertEquals(rolEsperado, resp.getBody().rol());
        verify(jwtService).generarToken(5L, "Alguien", rolEsperado);
    }
}
