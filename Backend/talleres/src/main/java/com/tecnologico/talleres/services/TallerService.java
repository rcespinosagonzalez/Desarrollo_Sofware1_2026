package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;

import java.util.List;

public interface TallerService {

    List<Taller> listarTodos();

    Taller obtenerPorId(Long id);

    Taller obtenerPrincipal();

    void guardar(Taller taller);

    List<Valoracion> listarValoraciones();

    List<Cita> listarCitas();

    void agregarCita(Cita cita);

    List<BusquedaHistorial> listarHistorial();

    UltimaCalificacion obtenerUltimaCalificacion();

    void registrarCalificacion(UltimaCalificacion calificacion);
}
