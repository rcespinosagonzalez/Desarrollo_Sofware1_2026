package com.tecnologico.talleres.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Coleccion citas. fechaHora se guarda en UTC; la zona horaria de la app solo se usa al mostrar.
// ocupaTurno es true mientras la cita no este cancelada: el indice unico parcial
// (mecanicoId, fechaHora) solo mira esas citas, asi un mecanico no puede tener dos a la misma hora
@Document("citas")
public class CitaDoc {

    @Id
    public ObjectId id;
    public ObjectId clienteId;
    public ObjectId tallerId;
    public ObjectId mecanicoId;
    // Copias de nombres para listar sin consultar otras colecciones
    public String tallerNombre;
    public String mecanicoNombre;
    public Instant fechaHora;
    // solicitada, confirmada, cancelada o completada
    public String estado;
    public boolean ocupaTurno;
    public String cliente;
    public String motivo;
    // Vehiculo elegido al pedir la cita, en texto ("ABC123 · Mazda 3 · 2019"); opcional
    public String vehiculo;
    public Instant creadoEn;
}
