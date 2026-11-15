package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.Mecanico;

import java.util.List;

// Mecanicos de un taller: cada uno atiende una cita por turno
public interface MecanicoService {

    // Mecanicos activos del taller
    List<Mecanico> listarActivos(String tallerId);

    // El dueno agrega un mecanico a uno de sus talleres
    void crear(String tallerId, String propietarioUsuarioId, String nombres, String apellidos, String telefono);

    // El dueno da de baja un mecanico (no se borra: puede tener citas en el historial)
    void desactivar(String mecanicoId, String propietarioUsuarioId);
}
