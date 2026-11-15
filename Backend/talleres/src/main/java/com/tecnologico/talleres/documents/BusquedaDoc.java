package com.tecnologico.talleres.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Coleccion busquedas: historial del buscador del mapa. Un indice TTL borra solas las antiguas
@Document("busquedas")
public class BusquedaDoc {

    @Id
    public ObjectId id;
    public ObjectId usuarioId;
    public String termino;
    public Double origenLat;
    public Double origenLng;
    public Integer radioM;
    public Integer resultados;
    public String origen;
    public Instant creadoEn;
}
