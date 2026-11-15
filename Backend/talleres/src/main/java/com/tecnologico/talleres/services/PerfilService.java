package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.Perfil;

// Perfil del cliente: datos personales, seguridad de la cuenta y vehiculos.
// Los metodos que no se pueden cumplir lanzan IllegalArgumentException con un mensaje para el usuario.
public interface PerfilService {

    // Perfil con vehiculos y resumen de actividad; null si el usuario no existe o esta desactivado
    Perfil obtener(String usuarioId);

    // Cambia nombre, telefono y ciudad (los dos ultimos son opcionales). Devuelve el nombre guardado.
    String actualizarDatos(String usuarioId, String nombre, String telefono, String ciudad);

    // Cambia la contrasena pidiendo la actual
    void cambiarClave(String usuarioId, String claveActual, String claveNueva, String confirmacion);

    // Cambia el correo de acceso pidiendo la contrasena
    void cambiarCorreo(String usuarioId, String nuevoCorreo, String clave);

    // Guarda un vehiculo nuevo (maximo 5, sin placas repetidas). El anio llega como texto del formulario.
    void agregarVehiculo(String usuarioId, String placa, String tipo, String marca, String modelo,
                         String anio, String color);

    void eliminarVehiculo(String usuarioId, String vehiculoId);

    // Desactiva la cuenta (no borra datos), cancela las citas futuras y libera sus turnos
    void desactivarCuenta(String usuarioId, String clave);
}
