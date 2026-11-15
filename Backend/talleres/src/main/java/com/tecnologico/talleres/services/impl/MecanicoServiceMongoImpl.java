package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.model.Mecanico;
import com.tecnologico.talleres.services.MecanicoService;
import com.tecnologico.talleres.util.Ids;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Los mecanicos viven dentro del documento del taller (talleres.mecanicos)
@Service
public class MecanicoServiceMongoImpl implements MecanicoService {

    private final MongoTemplate mongo;

    public MecanicoServiceMongoImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<Mecanico> listarActivos(String tallerId) {
        List<Mecanico> lista = new ArrayList<>();
        ObjectId oid = Ids.oid(tallerId);
        TallerDoc t = oid == null ? null : mongo.findById(oid, TallerDoc.class);
        if (t != null && t.mecanicos != null) {
            for (TallerDoc.MecanicoDoc m : t.mecanicos) {
                if (m.activo) {
                    lista.add(Mapeos.mecanico(t, m));
                }
            }
        }
        return lista;
    }

    @Override
    public void crear(String tallerId, String propietarioUsuarioId, String nombres, String apellidos, String telefono) {
        TallerDoc taller = exigirDueno(tallerId, propietarioUsuarioId);
        String nom = nombres == null ? "" : nombres.trim();
        String ape = apellidos == null ? "" : apellidos.trim();
        if (nom.isEmpty() || nom.length() > 120 || ape.isEmpty() || ape.length() > 120) {
            throw new IllegalArgumentException("Escribe nombres y apellidos del mecánico (máximo 120 caracteres cada uno).");
        }
        String tel = telefono == null || telefono.isBlank() ? null : telefono.trim();
        if (tel != null && tel.length() > 40) {
            throw new IllegalArgumentException("El teléfono es demasiado largo.");
        }
        TallerDoc.MecanicoDoc m = new TallerDoc.MecanicoDoc();
        m.id = new ObjectId();
        m.nombres = nom;
        m.apellidos = ape;
        m.telefono = tel;
        m.activo = true;
        // $push agrega al arreglo sin reescribir el resto del documento
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(taller.id)),
                new Update().push("mecanicos", m).set("actualizadoEn", Instant.now()),
                TallerDoc.class);
    }

    @Override
    public void desactivar(String mecanicoId, String propietarioUsuarioId) {
        ObjectId mid = Ids.oid(mecanicoId);
        TallerDoc taller = mid == null ? null
                : mongo.findOne(Query.query(Criteria.where("mecanicos._id").is(mid)), TallerDoc.class);
        TallerDoc.MecanicoDoc objetivo = null;
        if (taller != null) {
            for (TallerDoc.MecanicoDoc m : taller.mecanicos) {
                if (mid.equals(m.id) && m.activo) {
                    objetivo = m;
                }
            }
        }
        if (objetivo == null) {
            throw new IllegalArgumentException("Ese mecánico no existe o ya está dado de baja.");
        }
        exigirDueno(Ids.texto(taller.id), propietarioUsuarioId);

        long activos = taller.mecanicos.stream().filter(m -> m.activo).count();
        if (activos <= 1) {
            throw new IllegalArgumentException(
                    "El taller necesita al menos un mecánico activo para recibir citas. Agrega otro antes de dar de baja a este.");
        }
        long pendientes = mongo.count(
                Query.query(Criteria.where("mecanicoId").is(mid)
                        .and("fechaHora").gte(Instant.now())
                        .and("ocupaTurno").is(true)),
                CitaDoc.class);
        if (pendientes > 0) {
            throw new IllegalArgumentException(
                    "Ese mecánico tiene " + pendientes + " cita(s) pendiente(s). No se puede dar de baja hasta que pasen o se cancelen.");
        }
        // Una sola operacion atomica: baja al mecanico solo si despues queda otro activo.
        // Si dos mecanicos se dan de baja a la vez, el segundo falla en vez de dejar el taller sin nadie.
        Document masDeUno = Document.parse(
                "{ $gt: [ { $size: { $filter: { input: '$mecanicos', cond: '$$this.activo' } } }, 1 ] }");
        Query q = new BasicQuery(new Document("_id", taller.id)
                .append("mecanicos", new Document("$elemMatch", new Document("_id", mid).append("activo", true)))
                .append("$expr", masDeUno));
        var r = mongo.updateFirst(q,
                new Update().set("mecanicos.$.activo", false).set("actualizadoEn", Instant.now()),
                TallerDoc.class);
        if (r.getModifiedCount() == 0) {
            throw new IllegalArgumentException(
                    "No se pudo dar de baja: el taller necesita al menos un mecánico activo para recibir citas.");
        }
    }

    // Solo el propietario del taller puede tocar sus mecanicos
    private TallerDoc exigirDueno(String tallerId, String propietarioUsuarioId) {
        ObjectId tid = Ids.oid(tallerId);
        ObjectId pid = Ids.oid(propietarioUsuarioId);
        TallerDoc t = tid == null || pid == null ? null
                : mongo.findOne(Query.query(Criteria.where("_id").is(tid).and("propietarioId").is(pid)), TallerDoc.class);
        if (t == null) {
            throw new IllegalArgumentException("Ese taller no es tuyo.");
        }
        return t;
    }
}
