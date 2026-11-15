package com.tecnologico.talleres.repositories;

import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.util.Ids;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Acceso a la coleccion usuarios (login, registro y dueños de talleres)
@Repository
public class UsuarioRepositorio {

    private final MongoTemplate mongo;

    public UsuarioRepositorio(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // Usuario activo por correo (ya normalizado a minusculas) o null
    public UsuarioDoc buscarActivoPorEmail(String email) {
        return mongo.findOne(
                Query.query(Criteria.where("email").is(email).and("activo").is(true)), UsuarioDoc.class);
    }

    public UsuarioDoc buscarPorId(String id) {
        ObjectId oid = Ids.oid(id);
        return oid == null ? null : mongo.findById(oid, UsuarioDoc.class);
    }

    // Varios usuarios en una sola consulta, indexados por id (para no consultar uno por taller)
    public Map<ObjectId, UsuarioDoc> porIds(Collection<ObjectId> ids) {
        Map<ObjectId, UsuarioDoc> mapa = new HashMap<>();
        if (ids.isEmpty()) {
            return mapa;
        }
        for (UsuarioDoc u : mongo.find(Query.query(Criteria.where("_id").in(ids)), UsuarioDoc.class)) {
            mapa.put(u.id, u);
        }
        return mapa;
    }

    // Inserta un usuario nuevo. Si el correo ya existe, el indice unico lanza DuplicateKeyException
    public UsuarioDoc crear(String email, String passwordHash, String nombre, String rol) {
        UsuarioDoc u = new UsuarioDoc();
        u.id = new ObjectId();
        u.email = email;
        u.passwordHash = passwordHash;
        u.nombre = nombre;
        u.rol = rol;
        u.activo = true;
        u.creadoEn = Instant.now();
        return mongo.insert(u);
    }

    public void borrar(ObjectId id) {
        mongo.remove(Query.query(Criteria.where("_id").is(id)), UsuarioDoc.class);
    }
}
