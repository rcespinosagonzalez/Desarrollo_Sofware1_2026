package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;

import java.util.List;

// Contrato de acceso a datos: todo va a MySQL con JDBC (sin JPA)
public interface TallerService {

    // Todos los talleres con ubicacion y promedio (mapa y admin)
    List<Taller> listarTodos();

    // Talleres cuyo propietario_usuario_id es el dueno logueado
    List<Taller> listarTalleresDelPropietario(long propietarioUsuarioId);

    // Un taller por id o null
    Taller obtenerPorId(Long id);

    // El de mayor id (fallback en detalle sin parametro)
    Taller obtenerPrincipal();

    // Insert o update; admin edita cualquiera, dueno solo el suyo
    void guardar(Taller taller, long propietarioUsuarioId, boolean esAdministrador);

    // Valoraciones de los talleres de un dueno
    List<Valoracion> listarValoracionesPorPropietario(long propietarioUsuarioId);

    // Valoraciones de un taller concreto (ficha)
    List<Valoracion> listarValoracionesPorTaller(Long tallerId);

    // Todas las valoraciones para tabla admin
    List<Valoracion> listarTodasValoraciones();

    // Citas de los talleres de un dueno
    List<Cita> listarCitasPorPropietario(long propietarioUsuarioId);

    // Todas las citas para tabla admin
    List<Cita> listarTodasCitas();

    // Citas que pidio un cliente
    List<Cita> listarCitasPorCliente(long clienteUsuarioId);

    // Nueva cita solicitada desde el formulario web
    void agregarCita(Cita cita, long clienteUsuarioId);

    // Ultimas busquedas del mapa de ese usuario
    List<BusquedaHistorial> listarHistorialUsuario(long usuarioId);

    // Guardo una busqueda (la llama el JS del mapa)
    void registrarHistorialBusqueda(
            long usuarioId,
            String termino,
            Double origenLat,
            Double origenLng,
            Integer radioM,
            Integer resultados);

    // Ultima valoracion del cliente para mostrar en el formulario
    UltimaCalificacion obtenerUltimaCalificacion(long usuarioId);

    // Inserto fila en valoracion
    void registrarCalificacion(UltimaCalificacion calificacion, long usuarioId);
}
