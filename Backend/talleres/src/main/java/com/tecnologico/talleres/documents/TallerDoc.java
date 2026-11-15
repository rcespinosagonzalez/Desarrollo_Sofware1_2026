package com.tecnologico.talleres.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Coleccion talleres. Ubicacion, horario, mecanicos y resumen de valoraciones van embebidos:
// el mapa y la ficha se arman leyendo un solo documento
@Document("talleres")
public class TallerDoc {

    @Id
    public ObjectId id;
    public ObjectId propietarioId;
    public String nombre;
    public String telefono;
    public String descripcion;
    // publicado u oculto
    public String estado;
    public String servicios;
    public UbicacionDoc ubicacion;
    public List<HorarioDoc> horarios = new ArrayList<>();
    public List<MecanicoDoc> mecanicos = new ArrayList<>();
    public ResumenDoc resumen = new ResumenDoc();
    public Instant creadoEn;
    public Instant actualizadoEn;
    // Si es futura, el taller es "destacado" (suscripcion activa): aparece como patrocinado bajo el mapa
    public Instant destacadoHasta;

    // Direccion y punto GeoJSON [longitud, latitud] con indice 2dsphere (busqueda por cercania)
    public static class UbicacionDoc {
        public String direccion;
        public String ciudad;
        public String departamento;
        public String pais;
        public GeoJsonPoint punto;
    }

    // Un dia de atencion: dia 1 = lunes ... 7 = domingo; horas "HH:mm"
    public static class HorarioDoc {
        public int dia;
        public String apertura;
        public String cierre;
    }

    // Mecanico embebido; su id se guarda en las citas que atiende
    public static class MecanicoDoc {
        @Id
        public ObjectId id;
        public String nombres;
        public String apellidos;
        public String telefono;
        public boolean activo;
    }

    // Suma de estrellas y cantidad de valoraciones: el promedio es suma / total (se actualiza con $inc)
    public static class ResumenDoc {
        public long suma;
        public int total;
    }
}
