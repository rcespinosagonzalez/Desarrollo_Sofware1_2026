package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.services.ValoracionService;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Valoraciones sobre MongoDB. Cada valoracion guarda la cita que valora (citaId), asi nadie puede opinar de un
// taller donde no fue y cada visita se valora una vez. Con app.valoraciones.exigir-cita-completada=false
// se permite valorar sin cita (modo anterior).
@Service
public class ValoracionServiceMongoImpl implements ValoracionService {

    private final MongoTemplate mongo;
    private final ZoneId zona;
    private final boolean exigirCita;

    public ValoracionServiceMongoImpl(MongoTemplate mongo,
                                      @Value("${app.zona-horaria:America/Bogota}") String zona,
                                      @Value("${app.valoraciones.exigir-cita-completada:true}") boolean exigirCita) {
        this.mongo = mongo;
        this.zona = ZoneId.of(zona);
        this.exigirCita = exigirCita;
    }

    @Override
    public boolean exigeCitaCompletada() {
        return exigirCita;
    }

    @Override
    public List<Cita> citasPorValorar(String tallerId, String clienteUsuarioId) {
        ObjectId tid = Ids.oid(tallerId);
        ObjectId uid = Ids.oid(clienteUsuarioId);
        if (tid == null || uid == null) {
            return List.of();
        }
        Set<ObjectId> yaValoradas = new HashSet<>();
        for (ValoracionDoc v : mongo.find(
                Query.query(Criteria.where("clienteId").is(uid).and("tallerId").is(tid)), ValoracionDoc.class)) {
            if (v.citaId != null) {
                yaValoradas.add(v.citaId);
            }
        }
        List<Cita> porValorar = new ArrayList<>();
        for (CitaDoc c : mongo.find(
                Query.query(Criteria.where("clienteId").is(uid).and("tallerId").is(tid).and("estado").is("completada"))
                        .with(Sort.by(Sort.Direction.DESC, "fechaHora")),
                CitaDoc.class)) {
            if (!yaValoradas.contains(c.id)) {
                porValorar.add(Mapeos.cita(c, zona));
            }
        }
        return porValorar;
    }

    @Override
    public void valorar(String tallerId, String clienteUsuarioId, String citaId, int estrellas, String comentario) {
        if (estrellas < 1 || estrellas > 5) {
            throw new IllegalArgumentException("Elige de 1 a 5 estrellas.");
        }
        String texto = comentario == null ? "" : comentario.trim();
        if (texto.length() > 500) {
            throw new IllegalArgumentException("El comentario es demasiado largo (máximo 500 caracteres).");
        }
        ObjectId tid = Ids.oid(tallerId);
        ObjectId uid = Ids.oid(clienteUsuarioId);
        TallerDoc taller = tid == null ? null : mongo.findById(tid, TallerDoc.class);
        if (taller == null) {
            throw new IllegalArgumentException("El taller no existe.");
        }
        UsuarioDoc cliente = uid == null ? null : mongo.findOne(
                Query.query(Criteria.where("_id").is(uid).and("activo").is(true)), UsuarioDoc.class);
        if (cliente == null) {
            throw new IllegalArgumentException("Inicia sesión para valorar un taller.");
        }

        CitaDoc cita = null;
        ObjectId cid = Ids.oid(citaId);
        if (cid != null) {
            // La cita debe ser de este cliente y de este taller, estar completada y no tener valoracion
            cita = mongo.findOne(Query.query(Criteria.where("_id").is(cid)
                    .and("clienteId").is(uid)
                    .and("tallerId").is(tid)
                    .and("estado").is("completada")), CitaDoc.class);
            if (cita == null) {
                throw new IllegalArgumentException("Esa visita no se puede valorar.");
            }
            if (mongo.exists(Query.query(Criteria.where("citaId").is(cid)), ValoracionDoc.class)) {
                throw new IllegalArgumentException("Ya valoraste esta visita.");
            }
        } else if (exigirCita) {
            throw new IllegalArgumentException("Solo puedes valorar un taller después de una cita completada.");
        }

        ValoracionDoc v = new ValoracionDoc();
        v.id = new ObjectId();
        v.clienteId = cliente.id;
        v.clienteNombre = cliente.nombre;
        v.tallerId = taller.id;
        v.tallerNombre = taller.nombre;
        v.citaId = cita == null ? null : cita.id;
        v.estrellas = estrellas;
        v.servicio = cita == null ? null : cita.motivo;
        v.comentario = texto;
        v.creadoEn = Instant.now();
        mongo.insert(v);
        // $inc es atomico: el promedio del taller no se descuadra aunque varias personas valoren a la vez
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(taller.id)),
                new Update().inc("resumen.suma", estrellas).inc("resumen.total", 1),
                TallerDoc.class);
    }
}
