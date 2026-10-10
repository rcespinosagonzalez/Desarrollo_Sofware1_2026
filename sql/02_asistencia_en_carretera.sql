-- Asistencia en carretera - base de datos completa (esquema ER + datos demo).
-- Ejecutar como root (crea la BD). Antes o despues: sql/01_crear_usuario_mysql.sql
-- App Spring: usuario taller_app / taller123 (ver application.properties)

DROP DATABASE IF EXISTS asistencia_en_carretera;
CREATE DATABASE asistencia_en_carretera
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE asistencia_en_carretera;

-- ---------------------------------------------------------------------------
-- Tablas (orden por dependencias FK)
-- ---------------------------------------------------------------------------

-- Cuentas: admin plataforma, user cliente, taller dueno de negocio
CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuario_email (email),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('admin', 'user', 'taller'))
) ENGINE=InnoDB;

-- Negocio mecanico; propietario_usuario_id apunta al dueno
CREATE TABLE taller (
    id BIGINT NOT NULL AUTO_INCREMENT,
    propietario_usuario_id BIGINT NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    telefono VARCHAR(40) NULL,
    descripcion TEXT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'publicado',
    horario VARCHAR(200) NULL,
    servicios_texto VARCHAR(500) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_taller_propietario (propietario_usuario_id),
    CONSTRAINT fk_taller_propietario
        FOREIGN KEY (propietario_usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Coordenadas para el mapa Leaflet (una fila por taller)
CREATE TABLE ubicacion_taller (
    id BIGINT NOT NULL AUTO_INCREMENT,
    taller_id BIGINT NOT NULL,
    direccion_texto VARCHAR(300) NULL,
    ciudad VARCHAR(120) NULL,
    departamento VARCHAR(120) NULL,
    pais VARCHAR(80) NULL DEFAULT 'CO',
    lat DECIMAL(9, 6) NULL,
    lng DECIMAL(9, 6) NULL,
    geocodificado_en TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ubicacion_taller (taller_id),
    CONSTRAINT fk_ubicacion_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mecanico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    taller_id BIGINT NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(40) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_mecanico_taller (taller_id),
    CONSTRAINT fk_mecanico_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE servicio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(140) NOT NULL,
    descripcion TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_servicio_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE taller_servicio (
    taller_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    precio_desde DECIMAL(12, 2) NULL,
    duracion_min_estimada INT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (taller_id, servicio_id),
    CONSTRAINT fk_ts_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ts_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Pedido de cita del cliente; notas_usuario guarda nombre y detalle del formulario web
CREATE TABLE cita (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    servicio_id BIGINT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'solicitada',
    notas_usuario TEXT NULL,
    notas_taller TEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cita_usuario (usuario_id),
    KEY idx_cita_taller (taller_id),
    KEY idx_cita_fecha (fecha_hora),
    CONSTRAINT fk_cita_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_cita_estado CHECK (estado IN ('solicitada', 'confirmada', 'cancelada', 'completada'))
) ENGINE=InnoDB;

-- Busquedas del mapa (origen buscador); la escribe buscar-talleres.js via API
CREATE TABLE historial_busqueda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    termino VARCHAR(200) NOT NULL,
    origen_lat DECIMAL(9, 6) NULL,
    origen_lng DECIMAL(9, 6) NULL,
    radio_m INT NULL DEFAULT 0,
    resultados INT NULL,
    origen VARCHAR(20) NOT NULL DEFAULT 'buscador',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_historial_usuario_fecha (usuario_id, created_at),
    CONSTRAINT fk_historial_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_historial_origen CHECK (origen = 'buscador')
) ENGINE=InnoDB;

CREATE TABLE resultado_busqueda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    historial_busqueda_id BIGINT NOT NULL,
    fuente VARCHAR(20) NULL,
    fuente_id VARCHAR(80) NULL,
    nombre VARCHAR(200) NOT NULL,
    direccion VARCHAR(300) NULL,
    tipo VARCHAR(80) NULL,
    lat DECIMAL(9, 6) NULL,
    lng DECIMAL(9, 6) NULL,
    distancia_km DECIMAL(10, 3) NULL,
    PRIMARY KEY (id),
    KEY idx_resultado_historial (historial_busqueda_id),
    CONSTRAINT fk_resultado_historial
        FOREIGN KEY (historial_busqueda_id) REFERENCES historial_busqueda (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Estrellas 1-5 y comentario del cliente sobre un taller
CREATE TABLE valoracion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    estrellas SMALLINT NOT NULL,
    comentario TEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_valoracion_taller (taller_id),
    KEY idx_valoracion_usuario (usuario_id),
    CONSTRAINT fk_valoracion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_valoracion_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_valoracion_cita
        FOREIGN KEY (cita_id) REFERENCES cita (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_valoracion_estrellas CHECK (estrellas BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Datos demo (contrasena de todos los usuarios: demo)
-- ---------------------------------------------------------------------------

INSERT INTO usuario (id, email, password_hash, nombre, rol, activo) VALUES
(1,  'admin@demo.local',    'demo', 'Admin Demo',              'admin',  TRUE),
(2,  'cliente@demo.local',  'demo', 'Cliente Web',             'user',   TRUE),
(3,  'taller1@demo.local',  'demo', 'Dueno El Rapido',         'taller', TRUE),
(4,  'taller2@demo.local',  'demo', 'Dueno La Candelaria',     'taller', TRUE),
(5,  'taller3@demo.local',  'demo', 'Dueno Mecanica del Caribe','taller', TRUE),
(6,  'taller4@demo.local',  'demo', 'Dueno San Francisco',     'taller', TRUE),
(7,  'taller5@demo.local',  'demo', 'Dueno Bocagrande',        'taller', TRUE),
(8,  'taller6@demo.local',  'demo', 'Dueno Getsemani',         'taller', TRUE),
(9,  'taller7@demo.local',  'demo', 'Dueno Motos Manga',       'taller', TRUE),
(10, 'taller8@demo.local',  'demo', 'Dueno Turbaco',           'taller', TRUE),
(11, 'taller9@demo.local',  'demo', 'Dueno Bazurto',           'taller', TRUE),
(12, 'taller10@demo.local', 'demo', 'Dueno El Bosque',         'taller', TRUE);

INSERT INTO taller (id, propietario_usuario_id, nombre, telefono, descripcion, estado, horario, servicios_texto) VALUES
(1, 3, 'Taller Mecanico El Rapido', '300 555 1234',
 'Mecanica general, cambio de aceite, frenos y revision antes de viaje.',
 'publicado', 'Lun - Sab 8:00 a 18:00', 'Aceite - Frenos - Suspension - Diagnostico'),
(2, 4, 'AutoServicio La Candelaria', '300 555 2234',
 'Especialistas en motor y transmision para carros particulares.',
 'publicado', 'Lun - Vie 7:30 a 17:30', 'Motor - Transmision - Embrague'),
(3, 5, 'Mecanica del Caribe', '300 555 3234',
 'Taller familiar con mas de 15 anos atendiendo flota liviana.',
 'publicado', 'Lun - Sab 8:00 a 19:00', 'Mecanica general - Alineacion - Balanceo'),
(4, 6, 'Taller San Francisco', '300 555 4234',
 'Revision electrica, bateria y aire acondicionado automotriz.',
 'publicado', 'Mar - Dom 9:00 a 18:00', 'Electricidad - A/C - Baterias'),
(5, 7, 'Frenos y Repuestos Bocagrande', '300 555 5234',
 'Cambio de pastillas, discos y liquido de frenos en el dia.',
 'publicado', 'Lun - Sab 8:00 a 17:00', 'Frenos - Pastillas - Discos'),
(6, 8, 'Electroautos Getsemani', '300 555 6234',
 'Diagnostico con escaner y reparacion de luces y sensores.',
 'publicado', 'Lun - Vie 8:00 a 18:00', 'Scanner - Luces - Sensores'),
(7, 9, 'Motos Rapidas Manga', '300 555 7234',
 'Mantenimiento de motos: aceite, cadena, llantas y frenos.',
 'publicado', 'Lun - Sab 8:00 a 16:00', 'Motos - Aceite - Llantas'),
(8, 10, 'Lubricentro Turbaco', '300 555 8234',
 'Cambio de aceite y filtros sin cita; revision de niveles gratis.',
 'publicado', 'Todos los dias 7:00 a 19:00', 'Aceite - Filtros - Fluidos'),
(9, 11, 'Diesel y Transmision Bazurto', '300 555 9234',
 'Camionetas y camiones pequenos: inyectores y caja de cambios.',
 'publicado', 'Lun - Vie 7:00 a 17:00', 'Diesel - Inyectores - Caja'),
(10, 12, 'Carroceria y Pintura El Bosque', '300 555 0234',
 'Latoneria, pintura y pulido para dejar el carro como nuevo.',
 'publicado', 'Lun - Sab 8:00 a 18:00', 'Latoneria - Pintura - Pulido');

INSERT INTO ubicacion_taller (id, taller_id, direccion_texto, ciudad, departamento, pais, lat, lng) VALUES
(1,  1, 'Av. Pedro de Heredia 45-20', 'Cartagena', 'Bolivar', 'CO', 10.391049, -75.479426),
(2,  2, 'Cra 2 # 10-15, La Candelaria', 'Cartagena', 'Bolivar', 'CO', 10.425800, -75.551200),
(3,  3, 'Calle del Curato 8-45', 'Cartagena', 'Bolivar', 'CO', 10.421500, -75.550100),
(4,  4, 'Av. San Francisco 32-10', 'Cartagena', 'Bolivar', 'CO', 10.418900, -75.548800),
(5,  5, 'Av. San Martin 5-120, Bocagrande', 'Cartagena', 'Bolivar', 'CO', 10.401200, -75.556500),
(6,  6, 'Calle 29 # 8-12, Getsemani', 'Cartagena', 'Bolivar', 'CO', 10.422800, -75.545600),
(7,  7, 'Av. Republica del Libano 55-30, Manga', 'Cartagena', 'Bolivar', 'CO', 10.408500, -75.512300),
(8,  8, 'Km 2 via Turbaco, sector El Carmen', 'Turbaco', 'Bolivar', 'CO', 10.328400, -75.411200),
(9,  9, 'Mercado Bazurto, modulo 12', 'Cartagena', 'Bolivar', 'CO', 10.395600, -75.498700),
(10, 10, 'Cra 17 # 45-80, El Bosque', 'Cartagena', 'Bolivar', 'CO', 10.435200, -75.514800);

INSERT INTO cita (id, usuario_id, taller_id, servicio_id, fecha_hora, estado, notas_usuario) VALUES
(1, 2, 1, NULL, '2026-03-10 09:00:00', 'solicitada',
 'Cliente: Carlos Mendoza\nDetalle: Cambio de aceite y filtro');

INSERT INTO valoracion (id, usuario_id, taller_id, cita_id, estrellas, comentario, created_at) VALUES
(1,  2, 1, NULL, 5, 'Muy rapidos con el aceite.', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2,  2, 1, NULL, 4, 'Buen precio y trato cordial.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3,  2, 2, NULL, 4, 'Repararon la transmision bien.', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4,  2, 2, NULL, 3, 'Demoraron un poco mas de lo previsto.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5,  2, 3, NULL, 5, 'Excelente mecanica, super recomendados.', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(6,  2, 3, NULL, 5, 'Explican todo con paciencia.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(7,  2, 4, NULL, 4, 'Arreglaron el aire acondicionado.', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8,  2, 5, NULL, 5, 'Frenos quedaron perfectos.', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(9,  2, 5, NULL, 4, 'Buen trabajo en pastillas.', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10, 2, 6, NULL, 4, 'Scanner detecto la falla al toque.', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(11, 2, 7, NULL, 5, 'Buen servicio para la moto.', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(12, 2, 8, NULL, 4, 'Cambio de aceite sin cita, genial.', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(13, 2, 9, NULL, 3, 'Correcto para camioneta diesel.', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(14, 2, 10, NULL, 5, 'Quedo como nuevo el pintado.', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(15, 2, 10, NULL, 4, 'Buen pulido de carroceria.', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- historial_busqueda: vacio al inicio; la app lo llena al pulsar Buscar en el mapa.
