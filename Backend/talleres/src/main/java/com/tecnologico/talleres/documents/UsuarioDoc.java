package com.tecnologico.talleres.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// Coleccion usuarios. El correo es unico (indice uk_usuarios_email) y va siempre en minusculas
@Document("usuarios")
public class UsuarioDoc {

    @Id
    public ObjectId id;
    public String email;
    public String passwordHash;
    public String nombre;
    // admin, taller o user
    public String rol;
    public boolean activo;
    public Instant creadoEn;
    // Datos de perfil (opcionales: los usuarios antiguos no los tienen)
    public String telefono;
    public String ciudad;
    public List<VehiculoDoc> vehiculos;
    // Se llena al desactivar la cuenta desde el perfil (activo pasa a false)
    public Instant desactivadoEn;

    // Vehiculo del cliente, guardado dentro del propio usuario (maximo 5)
    public static class VehiculoDoc {
        @Id
        public ObjectId id;
        public String placa;
        // carro, camioneta, moto, camion u otro
        public String tipo;
        public String marca;
        public String modelo;
        public Integer anio;
        public String color;
    }
}
