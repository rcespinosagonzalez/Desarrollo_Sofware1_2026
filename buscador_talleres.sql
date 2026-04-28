CREATE DATABASE IF NOT EXISTS buscador_talleres;
USE buscador_talleres;

CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE taller (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    propietario_usuario_id BIGINT NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    telefono VARCHAR(40),
    descripcion TEXT,
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_taller_propietario_usuario
        FOREIGN KEY (propietario_usuario_id) REFERENCES usuario(id)
);

CREATE TABLE ubicacion_taller (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    taller_id BIGINT NOT NULL,
    direccion_texto VARCHAR(300),
    ciudad VARCHAR(120),
    departamento VARCHAR(120),
    pais VARCHAR(80),
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    geocodificado_en TIMESTAMP NULL,
    CONSTRAINT fk_ubicacion_taller_taller
        FOREIGN KEY (taller_id) REFERENCES taller(id)
);

CREATE TABLE mecanico (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    taller_id BIGINT NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(40),
    activo BOOLEAN NOT NULL,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_mecanico_taller
        FOREIGN KEY (taller_id) REFERENCES taller(id)
);

CREATE TABLE servicio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(140) NOT NULL UNIQUE,
    descripcion TEXT
);

CREATE TABLE taller_servicio (
    taller_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    precio_desde DECIMAL(12,2),
    duracion_min_estimada INT,
    activo BOOLEAN NOT NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (taller_id, servicio_id),
    CONSTRAINT fk_taller_servicio_taller
        FOREIGN KEY (taller_id) REFERENCES taller(id),
    CONSTRAINT fk_taller_servicio_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio(id)
);

CREATE TABLE cita (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    servicio_id BIGINT,
    fecha_hora TIMESTAMP NULL,
    estado VARCHAR(20) NOT NULL,
    notas_usuario TEXT,
    notas_taller TEXT,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_cita_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_cita_taller
        FOREIGN KEY (taller_id) REFERENCES taller(id),
    CONSTRAINT fk_cita_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio(id)
);

CREATE TABLE historial_busqueda (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT,
    termino VARCHAR(200) NOT NULL,
    origen_lat DECIMAL(9,6),
    origen_lng DECIMAL(9,6),
    radio_m INT,
    resultados INT,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_historial_busqueda_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE resultado_busqueda (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    historial_busqueda_id BIGINT NOT NULL,
    fuente VARCHAR(20),
    fuente_id VARCHAR(80),
    nombre VARCHAR(200) NOT NULL,
    direccion VARCHAR(300),
    tipo VARCHAR(80),
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    distancia_km DECIMAL(10,3),
    CONSTRAINT fk_resultado_busqueda_historial
        FOREIGN KEY (historial_busqueda_id) REFERENCES historial_busqueda(id)
);

CREATE TABLE valoracion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    cita_id BIGINT,
    estrellas SMALLINT NOT NULL,
    comentario TEXT,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_valoracion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_valoracion_taller
        FOREIGN KEY (taller_id) REFERENCES taller(id),
    CONSTRAINT fk_valoracion_cita
        FOREIGN KEY (cita_id) REFERENCES cita(id)
);
