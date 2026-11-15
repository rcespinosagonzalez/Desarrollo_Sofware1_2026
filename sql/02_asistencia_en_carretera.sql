-- Asistencia en carretera - base de datos completa (esquema ER + datos demo).
-- Ejecutar como root (crea la BD). Antes o despues: sql/01_crear_usuario_mysql.sql
-- App Spring: usuario taller_app; su clave va en la variable de entorno DB_PASSWORD

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

-- Dias y horas de atencion (una fila por dia abierto). Las citas solo se agendan dentro de este horario.
CREATE TABLE horario_taller (
    taller_id BIGINT NOT NULL,
    dia_semana TINYINT NOT NULL,
    apertura TIME NOT NULL,
    cierre TIME NOT NULL,
    PRIMARY KEY (taller_id, dia_semana),
    CONSTRAINT fk_horario_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_rango CHECK (apertura < cierre)
) ENGINE=InnoDB;
-- dia_semana: 1 = lunes ... 7 = domingo (como DayOfWeek de Java)

-- Cada mecanico atiende una cita por turno de 1 hora
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

-- Pedido de cita del cliente; notas_usuario guarda nombre y detalle del formulario web.
-- mecanico_id lo asigna la agenda (un mecanico por cita y turno; ver AgendaServiceJdbcImpl)
CREATE TABLE cita (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    servicio_id BIGINT NULL,
    mecanico_id BIGINT NULL,
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
    KEY idx_cita_taller_fecha (taller_id, fecha_hora),
    KEY idx_cita_mecanico_fecha (mecanico_id, fecha_hora),
    CONSTRAINT fk_cita_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_taller
        FOREIGN KEY (taller_id) REFERENCES taller (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_cita_mecanico
        FOREIGN KEY (mecanico_id) REFERENCES mecanico (id)
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

-- Contrasena de TODOS los usuarios demo: demo (guardada con BCrypt, ya no en texto plano)
INSERT INTO usuario (id, email, password_hash, nombre, rol, activo) VALUES
(1,  'admin@demo.local',    '$2a$10$0rP4WFSGgHa/nvpqZwVR8eUilSuo1N4gC2L6is9sufuUD0lqNur8O', 'Admin Demo',              'admin',  TRUE),
(2,  'cliente@demo.local',  '$2a$10$uIcZS22P.EtVtluhdghaYO4OO2nIzdeAZmDP4PA4CBZ4AIPFMooje', 'Cliente Web',             'user',   TRUE),
(3,  'taller1@demo.local',  '$2a$10$0EtYS6uRhUS19AIeqbXfKeDiR6BQ2MhcEdaZMuY6tgBS5uy45Ivoi', 'Dueno El Rapido',         'taller', TRUE),
(4,  'taller2@demo.local',  '$2a$10$EA4GrROng38MD2bVfTRZlOV0q3LCDg7w6vtgEMb8EiFkljOXvkMbi', 'Dueno La Candelaria',     'taller', TRUE),
(5,  'taller3@demo.local',  '$2a$10$EihzE8.B3m29xF3FuCLG7OgcYrVRqMZUBnK6MMC/EpO9OkGfSC8f2', 'Dueno Mecanica del Caribe','taller', TRUE),
(6,  'taller4@demo.local',  '$2a$10$Z9e/zx8rbtPmX8oJ.W.aZOlU.RGcTfhHH37H67hwQ3u1s5vFiCaYK', 'Dueno San Francisco',     'taller', TRUE),
(7,  'taller5@demo.local',  '$2a$10$0cq5R7qTf8S2rqd4UQvbe./K4eXH31yKCSRf99JzU4SRXHSCEx.vW', 'Dueno Bocagrande',        'taller', TRUE),
(8,  'taller6@demo.local',  '$2a$10$9IhQFSAphfD4TJ8iv2B2oeGlMPJPDDbTzR2qhgJPU9zxwLNQ/xot.', 'Dueno Getsemani',         'taller', TRUE),
(9,  'taller7@demo.local',  '$2a$10$50gpKtod8XlxaJwihJqYSu94Cnn4npsRsSFOcC3.uWGsW1vN6DNZe', 'Dueno Motos Manga',       'taller', TRUE),
(10, 'taller8@demo.local',  '$2a$10$vlDhcTWQOzkirNqO04/2BeSw//CBcIOvSVOSxVSV4dkmlaSg9LWVe', 'Dueno Turbaco',           'taller', TRUE),
(11, 'taller9@demo.local',  '$2a$10$UXrrkwpkL/IWz447YJatt.T9uu7VFJf.8a.ZVFt/0J47j/Nlw60Da', 'Dueno Bazurto',           'taller', TRUE),
(12, 'taller10@demo.local', '$2a$10$ioylmLnHXePQQUTXjy8oRu3M03gY588987IrJcjjL0LSpUmKDnId.', 'Dueno El Bosque',         'taller', TRUE);

INSERT INTO taller (id, propietario_usuario_id, nombre, telefono, descripcion, estado, servicios_texto) VALUES
(1, 3, 'Taller Mecanico El Rapido', '300 555 1234',
 'Mecanica general, cambio de aceite, frenos y revision antes de viaje.',
 'publicado', 'Aceite - Frenos - Suspension - Diagnostico'),
(2, 4, 'AutoServicio La Candelaria', '300 555 2234',
 'Especialistas en motor y transmision para carros particulares.',
 'publicado', 'Motor - Transmision - Embrague'),
(3, 5, 'Mecanica del Caribe', '300 555 3234',
 'Taller familiar con mas de 15 anos atendiendo flota liviana.',
 'publicado', 'Mecanica general - Alineacion - Balanceo'),
(4, 6, 'Taller San Francisco', '300 555 4234',
 'Revision electrica, bateria y aire acondicionado automotriz.',
 'publicado', 'Electricidad - A/C - Baterias'),
(5, 7, 'Frenos y Repuestos Bocagrande', '300 555 5234',
 'Cambio de pastillas, discos y liquido de frenos en el dia.',
 'publicado', 'Frenos - Pastillas - Discos'),
(6, 8, 'Electroautos Getsemani', '300 555 6234',
 'Diagnostico con escaner y reparacion de luces y sensores.',
 'publicado', 'Scanner - Luces - Sensores'),
(7, 9, 'Motos Rapidas Manga', '300 555 7234',
 'Mantenimiento de motos: aceite, cadena, llantas y frenos.',
 'publicado', 'Motos - Aceite - Llantas'),
(8, 10, 'Lubricentro Turbaco', '300 555 8234',
 'Cambio de aceite y filtros sin cita; revision de niveles gratis.',
 'publicado', 'Aceite - Filtros - Fluidos'),
(9, 11, 'Diesel y Transmision Bazurto', '300 555 9234',
 'Camionetas y camiones pequenos: inyectores y caja de cambios.',
 'publicado', 'Diesel - Inyectores - Caja'),
(10, 12, 'Carroceria y Pintura El Bosque', '300 555 0234',
 'Latoneria, pintura y pulido para dejar el carro como nuevo.',
 'publicado', 'Latoneria - Pintura - Pulido');

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

-- Horarios de atencion de los talleres demo
INSERT INTO horario_taller (taller_id, dia_semana, apertura, cierre) VALUES
(1, 1, '08:00:00', '18:00:00'),
(1, 2, '08:00:00', '18:00:00'),
(1, 3, '08:00:00', '18:00:00'),
(1, 4, '08:00:00', '18:00:00'),
(1, 5, '08:00:00', '18:00:00'),
(1, 6, '08:00:00', '18:00:00'),
(2, 1, '07:30:00', '17:30:00'),
(2, 2, '07:30:00', '17:30:00'),
(2, 3, '07:30:00', '17:30:00'),
(2, 4, '07:30:00', '17:30:00'),
(2, 5, '07:30:00', '17:30:00'),
(3, 1, '08:00:00', '19:00:00'),
(3, 2, '08:00:00', '19:00:00'),
(3, 3, '08:00:00', '19:00:00'),
(3, 4, '08:00:00', '19:00:00'),
(3, 5, '08:00:00', '19:00:00'),
(3, 6, '08:00:00', '19:00:00'),
(4, 2, '09:00:00', '18:00:00'),
(4, 3, '09:00:00', '18:00:00'),
(4, 4, '09:00:00', '18:00:00'),
(4, 5, '09:00:00', '18:00:00'),
(4, 6, '09:00:00', '18:00:00'),
(4, 7, '09:00:00', '18:00:00'),
(5, 1, '08:00:00', '17:00:00'),
(5, 2, '08:00:00', '17:00:00'),
(5, 3, '08:00:00', '17:00:00'),
(5, 4, '08:00:00', '17:00:00'),
(5, 5, '08:00:00', '17:00:00'),
(5, 6, '08:00:00', '17:00:00'),
(6, 1, '08:00:00', '18:00:00'),
(6, 2, '08:00:00', '18:00:00'),
(6, 3, '08:00:00', '18:00:00'),
(6, 4, '08:00:00', '18:00:00'),
(6, 5, '08:00:00', '18:00:00'),
(7, 1, '08:00:00', '16:00:00'),
(7, 2, '08:00:00', '16:00:00'),
(7, 3, '08:00:00', '16:00:00'),
(7, 4, '08:00:00', '16:00:00'),
(7, 5, '08:00:00', '16:00:00'),
(7, 6, '08:00:00', '16:00:00'),
(8, 1, '07:00:00', '19:00:00'),
(8, 2, '07:00:00', '19:00:00'),
(8, 3, '07:00:00', '19:00:00'),
(8, 4, '07:00:00', '19:00:00'),
(8, 5, '07:00:00', '19:00:00'),
(8, 6, '07:00:00', '19:00:00'),
(8, 7, '07:00:00', '19:00:00'),
(9, 1, '07:00:00', '17:00:00'),
(9, 2, '07:00:00', '17:00:00'),
(9, 3, '07:00:00', '17:00:00'),
(9, 4, '07:00:00', '17:00:00'),
(9, 5, '07:00:00', '17:00:00'),
(10, 1, '08:00:00', '18:00:00'),
(10, 2, '08:00:00', '18:00:00'),
(10, 3, '08:00:00', '18:00:00'),
(10, 4, '08:00:00', '18:00:00'),
(10, 5, '08:00:00', '18:00:00'),
(10, 6, '08:00:00', '18:00:00');

-- Mecanicos demo (el taller 1 tiene dos: puede atender 2 citas a la misma hora)
INSERT INTO mecanico (id, taller_id, nombres, apellidos) VALUES
(1, 1, 'Mecanico', 'Principal'),
(2, 2, 'Mecanico', 'Principal'),
(3, 3, 'Mecanico', 'Principal'),
(4, 4, 'Mecanico', 'Principal'),
(5, 5, 'Mecanico', 'Principal'),
(6, 6, 'Mecanico', 'Principal'),
(7, 7, 'Mecanico', 'Principal'),
(8, 8, 'Mecanico', 'Principal'),
(9, 9, 'Mecanico', 'Principal'),
(10, 10, 'Mecanico', 'Principal'),
(11, 1, 'Mecanico', 'Auxiliar');

INSERT INTO cita (id, usuario_id, taller_id, servicio_id, mecanico_id, fecha_hora, estado, notas_usuario) VALUES
(1, 2, 1, NULL, 1, '2026-03-10 09:00:00', 'solicitada',
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
