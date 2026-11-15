package com.tecnologico.talleres.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Coleccion valoraciones: estrellas y comentario de un cliente sobre un taller
@Document("valoraciones")
public class ValoracionDoc {

    @Id
    public ObjectId id;
    public ObjectId clienteId;
    public String clienteNombre;
    public ObjectId tallerId;
    public String tallerNombre;
    // Opcional: cita que se esta valorando
    public ObjectId citaId;
    public int estrellas;
    public String servicio;
    public String comentario;
    public Instant creadoEn;
}
