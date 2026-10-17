package com.tecnologico.talleres.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

// Crea y valida los tokens JWT (firmados con HS256) y arma la cookie donde viaja el token
@Service
public class JwtService {

    // Nombre de la cookie HttpOnly que guarda el token en el navegador
    public static final String COOKIE = "TALLERES_JWT";

    private static final Set<String> ROLES = Set.of("admin", "taller", "user");

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration duracion;
    private final boolean cookieSegura;

    public JwtService(
            @Value("${jwt.secret}") String secreto,
            @Value("${jwt.expiration-minutes:60}") long minutos,
            @Value("${jwt.cookie-secure:false}") boolean cookieSegura) {
        // HS256 exige una clave de al menos 32 bytes; si es corta, aviso de una vez
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres");
        }
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        this.decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        this.duracion = Duration.ofMinutes(minutos);
        this.cookieSegura = cookieSegura;
    }

    // Token con id (subject), nombre y rol; vence segun jwt.expiration-minutes
    public String generarToken(long id, String nombre, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("talleres")
                .subject(String.valueOf(id))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .claim("nombre", nombre)
                .claim("rol", rol)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    // Verifica firma y vencimiento. Devuelve null si el token no sirve
    public UsuarioAutenticado validar(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            String rol = jwt.getClaimAsString("rol");
            if (rol == null || !ROLES.contains(rol)) {
                return null;
            }
            return new UsuarioAutenticado(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("nombre"), rol);
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    // Cookie del login: HttpOnly (JS no la lee), SameSite=Lax (no viaja en POST de otros sitios)
    public ResponseCookie crearCookie(String token) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .maxAge(duracion)
                .build();
    }

    // Cookie vacia con vida 0: el navegador la borra (logout)
    public ResponseCookie borrarCookie() {
        return ResponseCookie.from(COOKIE, "")
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
