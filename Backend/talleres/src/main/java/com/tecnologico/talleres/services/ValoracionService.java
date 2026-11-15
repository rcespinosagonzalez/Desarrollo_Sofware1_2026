package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.Cita;

import java.util.List;

// Valoraciones verificadas: se valora una visita (cita completada) y cada visita se valora una sola vez.
// Los metodos que no se pueden cumplir lanzan IllegalArgumentException con un mensaje para el usuario.
public interface ValoracionService {

    // true si solo se puede valorar un taller tras una cita completada (app.valoraciones.exigir-cita-completada)
    boolean exigeCitaCompletada();

    // Citas completadas del cliente en ese taller que todavia no tienen valoracion
    List<Cita> citasPorValorar(String tallerId, String clienteUsuarioId);

    // Guarda la valoracion y actualiza el resumen (suma y total) del taller. citaId puede faltar si no se exige cita.
    void valorar(String tallerId, String clienteUsuarioId, String citaId, int estrellas, String comentario);
}
