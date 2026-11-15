package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.Cita;

import java.time.LocalDate;
import java.util.List;

// Agenda de citas: turnos de 1 hora dentro del horario del taller y un mecanico libre por cita
public interface AgendaService {

    // Horas de inicio ("HH:mm") que aun tienen un mecanico libre ese dia; vacia si el taller no atiende
    List<String> horasLibres(String tallerId, LocalDate fecha);

    // Igual, pero sin contar la cita excluirCitaId: al editar una cita su propia hora debe verse como libre
    List<String> horasLibres(String tallerId, LocalDate fecha, String excluirCitaId);

    // Crea la cita en un turno libre y le asigna un mecanico.
    // Lanza IllegalArgumentException con un mensaje para el usuario si no se puede agendar.
    // vehiculo es opcional (texto con placa y descripcion).
    void solicitarCita(String tallerId, String clienteUsuarioId, String cliente, String motivo, String fecha, String hora,
                       String vehiculo);

    // El dueno del taller cambia el estado de una cita de su taller:
    // solicitada -> confirmada o cancelada; confirmada -> completada (cuando ya llego la hora) o cancelada.
    void cambiarEstadoCita(String citaId, String propietarioUsuarioId, String nuevoEstado);

    // La cita solo si pertenece a ese cliente; null si no existe o es de otra persona
    Cita citaDelCliente(String citaId, String clienteUsuarioId);

    // Cambia motivo, nombre de contacto y/o turno de una cita del cliente. Si cambia el turno se busca un
    // mecanico libre y la cita vuelve a "solicitada" para que el taller la revise de nuevo.
    // Lanza IllegalArgumentException con un mensaje para el usuario si no se puede.
    void editarCita(String citaId, String clienteUsuarioId, String cliente, String motivo, String fecha, String hora);

    // Cancela una cita futura del cliente y libera el turno del mecanico
    void cancelarCita(String citaId, String clienteUsuarioId);
}
