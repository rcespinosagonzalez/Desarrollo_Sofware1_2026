package com.tecnologico.talleres.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Bloquea temporalmente un correo desde una IP tras varios intentos de login fallidos.
// Vive en memoria: se reinicia con la app y no se comparte entre varias instancias.
@Component
public class LoginAttemptService {

    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int LIMPIAR_DESDE = 10_000;

    private record Estado(int fallos, Instant primerFallo, Instant bloqueadoHasta) {}

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    // La clave mezcla IP y correo: asi un atacante no puede bloquear a un usuario desde otra IP
    public static String clave(String ip, String email) {
        return ip + "|" + (email == null ? "" : email.trim().toLowerCase());
    }

    // Segundos que faltan para poder reintentar; 0 si no esta bloqueado
    public long segundosBloqueado(String clave) {
        Estado e = estados.get(clave);
        if (e == null || e.bloqueadoHasta() == null) {
            return 0;
        }
        long resta = Duration.between(Instant.now(), e.bloqueadoHasta()).getSeconds();
        return Math.max(resta, 0);
    }

    public void registrarFallo(String clave) {
        Instant ahora = Instant.now();
        if (estados.size() > LIMPIAR_DESDE) {
            limpiar(ahora);
        }
        estados.compute(clave, (k, e) -> {
            if (e == null || ahora.isAfter(e.primerFallo().plus(VENTANA))) {
                return new Estado(1, ahora, null);
            }
            int fallos = e.fallos() + 1;
            Instant hasta = fallos >= MAX_FALLOS ? ahora.plus(VENTANA) : null;
            return new Estado(fallos, e.primerFallo(), hasta);
        });
    }

    public void registrarExito(String clave) {
        estados.remove(clave);
    }

    private void limpiar(Instant ahora) {
        estados.entrySet().removeIf(en -> ahora.isAfter(en.getValue().primerFallo().plus(VENTANA))
                && (en.getValue().bloqueadoHasta() == null || ahora.isAfter(en.getValue().bloqueadoHasta())));
    }
}
