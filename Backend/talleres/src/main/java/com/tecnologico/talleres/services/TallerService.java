package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;

import java.util.List;

// Contrato de acceso a datos: todo vive en MongoDB. Los ids son ObjectId en texto hexadecimal
public interface TallerService {

    // Todos los talleres con ubicacion y promedio (mapa y admin)
    List<Taller> listarTodos();

    // Talleres cuyo propietarioId es el dueno logueado
    List<Taller> listarTalleresDelPropietario(String propietarioUsuarioId);

    // Un taller por id o null (tambien null si el id no tiene forma de ObjectId)
    Taller obtenerPorId(String id);

    // Insert o update; admin edita cualquiera, dueno solo el suyo
    void guardar(Taller taller, String propietarioUsuarioId, boolean esAdministrador);

    // Valoraciones de los talleres de un dueno
    List<Valoracion> listarValoracionesPorPropietario(String propietarioUsuarioId);

    // Valoraciones de un taller concreto (ficha)
    List<Valoracion> listarValoracionesPorTaller(String tallerId);

    // Las ultimas 100 valoraciones para tabla admin
    List<Valoracion> listarTodasValoraciones();

    // Citas de los talleres de un dueno
    List<Cita> listarCitasPorPropietario(String propietarioUsuarioId);

    // Las ultimas 500 citas para tabla admin
    List<Cita> listarTodasCitas();

    // Citas que pidio un cliente
    List<Cita> listarCitasPorCliente(String clienteUsuarioId);

    // Ultimas busquedas del mapa de ese usuario
    List<BusquedaHistorial> listarHistorialUsuario(String usuarioId);

    // Guardo una busqueda (la llama el JS del mapa)
    void registrarHistorialBusqueda(
            String usuarioId,
            String termino,
            Double origenLat,
            Double origenLng,
            Integer radioM,
            Integer resultados);

    // Ultima valoracion del cliente para mostrar en el formulario
    UltimaCalificacion obtenerUltimaCalificacion(String usuarioId);

    // Guarda la valoracion y actualiza el resumen (suma y total) del taller
    void registrarCalificacion(UltimaCalificacion calificacion, String usuarioId);

    // Admin: activa o extiende el plan destacado del taller por esa cantidad de dias (1 a 365)
    void destacar(String tallerId, int dias);

    // Admin: quita el plan destacado
    void quitarDestacado(String tallerId);
}
