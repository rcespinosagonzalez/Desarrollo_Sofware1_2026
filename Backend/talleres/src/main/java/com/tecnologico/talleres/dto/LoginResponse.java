package com.tecnologico.talleres.dto;

/** Respuesta simple del login contra MySQL (práctica de curso). */
public record LoginResponse(boolean ok, String nombre, String rol, String mensaje) {
}
