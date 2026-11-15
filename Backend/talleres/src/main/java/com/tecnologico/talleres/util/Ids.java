package com.tecnologico.talleres.util;

import org.bson.types.ObjectId;

// Los ids de MongoDB son ObjectId (24 caracteres hexadecimales). Hacia la web viajan como texto
public final class Ids {

    private Ids() {}

    // true si el texto tiene forma de ObjectId
    public static boolean esValido(String id) {
        return id != null && ObjectId.isValid(id);
    }

    // Texto -> ObjectId; null si viene vacio o no es valido (asi un id inventado solo da "no existe")
    public static ObjectId oid(String id) {
        return esValido(id) ? new ObjectId(id) : null;
    }

    // ObjectId -> texto; null si no hay
    public static String texto(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}
