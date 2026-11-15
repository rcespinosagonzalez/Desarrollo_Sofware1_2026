package com.tecnologico.talleres.security;

// Datos del usuario que viajan dentro del token JWT (id hexadecimal, nombre y rol: admin, taller o user)
public record UsuarioAutenticado(String id, String nombre, String rol) {}
