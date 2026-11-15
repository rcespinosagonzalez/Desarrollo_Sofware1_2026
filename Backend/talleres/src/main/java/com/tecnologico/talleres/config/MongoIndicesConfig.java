package com.tecnologico.talleres.config;

import com.tecnologico.talleres.documents.BusquedaDoc;
import com.tecnologico.talleres.documents.CitaDoc;
import com.tecnologico.talleres.documents.TallerDoc;
import com.tecnologico.talleres.documents.UsuarioDoc;
import com.tecnologico.talleres.documents.ValoracionDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

// Crea los indices al arrancar (si ya existen con la misma definicion no hace nada).
// Los nombres son fijos y los mismos que usa mongo/seed.js: asi ninguno de los dos choca con el otro.
// Lo importante: uk_citas_mecanico_turno es lo que impide dos citas del mismo mecanico a la misma hora,
// aunque dos personas lo pidan en el mismo instante.
@Configuration
public class MongoIndicesConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MongoIndicesConfig.class);

    private static final long DIAS_HISTORIAL = 180;

    @Bean
    ApplicationRunner crearIndices(MongoTemplate mongo) {
        return args -> {
            var usuarios = mongo.indexOps(UsuarioDoc.class);
            usuarios.createIndex(new Index().on("email", Direction.ASC).unique().named("uk_usuarios_email"));

            var talleres = mongo.indexOps(TallerDoc.class);
            talleres.createIndex(new GeospatialIndex("ubicacion.punto")
                    .typed(GeoSpatialIndexType.GEO_2DSPHERE).named("idx_talleres_ubicacion"));
            talleres.createIndex(new Index().on("propietarioId", Direction.ASC).named("idx_talleres_propietario"));
            talleres.createIndex(new Index().on("mecanicos._id", Direction.ASC).named("idx_talleres_mecanico"));

            var citas = mongo.indexOps(CitaDoc.class);
            citas.createIndex(new Index()
                    .on("mecanicoId", Direction.ASC).on("fechaHora", Direction.ASC)
                    .unique()
                    .partial(PartialIndexFilter.of(Criteria.where("ocupaTurno").is(true)))
                    .named("uk_citas_mecanico_turno"));
            citas.createIndex(new Index()
                    .on("tallerId", Direction.ASC).on("fechaHora", Direction.ASC).named("idx_citas_taller_fecha"));
            citas.createIndex(new Index()
                    .on("clienteId", Direction.ASC).on("fechaHora", Direction.DESC).named("idx_citas_cliente"));

            var valoraciones = mongo.indexOps(ValoracionDoc.class);
            valoraciones.createIndex(new Index()
                    .on("tallerId", Direction.ASC).on("creadoEn", Direction.DESC).named("idx_valoraciones_taller"));
            valoraciones.createIndex(new Index()
                    .on("clienteId", Direction.ASC).on("creadoEn", Direction.DESC).named("idx_valoraciones_cliente"));

            var busquedas = mongo.indexOps(BusquedaDoc.class);
            busquedas.createIndex(new Index()
                    .on("usuarioId", Direction.ASC).on("creadoEn", Direction.DESC).named("idx_busquedas_usuario"));
            busquedas.createIndex(new Index()
                    .on("creadoEn", Direction.ASC).expire(DIAS_HISTORIAL * 24 * 3600).named("ttl_busquedas"));

            LOG.info("Indices de MongoDB verificados");
        };
    }
}
