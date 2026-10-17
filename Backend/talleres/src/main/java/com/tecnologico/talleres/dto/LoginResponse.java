package com.tecnologico.talleres.dto;

// JSON que devuelve POST /api/auth/login
public record LoginResponse(
        boolean ok,           // true si el correo y la clave son correctos
        String nombre,        // nombre para mostrar en menu
        String rol,           // admin, user o taller
        String mensaje,       // error si ok es false
        Long usuarioId,       // id para sessionStorage en el navegador
        String token          // JWT (tambien va en cookie HttpOnly); sirve para probar con Postman/curl
) {}
