package com.tecnologico.talleres.security;

// Datos del usuario que viajan dentro del token JWT (id, nombre y rol: admin, taller o user)
public record UsuarioAutenticado(long id, String nombre, String rol) {}
