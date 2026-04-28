-- Ejecutar en MySQL como root DESPUÉS de buscador_talleres.sql (esquema con id numérico).
-- Si ya tenías la BD con UUID, borra la base o las tablas y vuelve a crear con buscador_talleres.sql

CREATE USER IF NOT EXISTS 'taller_app'@'localhost' IDENTIFIED BY 'taller123';
GRANT ALL PRIVILEGES ON buscador_talleres.* TO 'taller_app'@'localhost';
FLUSH PRIVILEGES;

USE buscador_talleres;

-- Si las columnas ya existen, comenta estas dos líneas:
ALTER TABLE taller ADD COLUMN horario VARCHAR(200) NULL;
ALTER TABLE taller ADD COLUMN servicios_texto VARCHAR(500) NULL;

-- IDs fijos pequeños (1, 2, 3…) para practicar
INSERT INTO usuario (id, email, password_hash, nombre, rol, activo, created_at, updated_at) VALUES
(1, 'admin@demo.local', 'demo', 'Admin Demo', 'admin', TRUE, NOW(), NOW()),
(2, 'cliente@demo.local', 'demo', 'Cliente Web', 'user', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO taller (id, propietario_usuario_id, nombre, telefono, descripcion, estado, horario, servicios_texto, created_at, updated_at) VALUES
(1, 1,
 'Taller Mecánico El Rápido', '300 555 1234',
 'Taller especializado en mecánica general, cambio de aceite, frenos y revisión técnico-mecánica.',
 'publicado', 'Lun - Sáb 8:00 a 18:00', 'Mecánica general · Cambio de aceite · Frenos · Electricidad básica · Diagnóstico', NOW(), NOW())
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO ubicacion_taller (id, taller_id, direccion_texto, ciudad, departamento, pais, lat, lng) VALUES
(1, 1,
 'Av. Pedro de Heredia 45-20', 'Cartagena', 'Bolívar', 'CO', 10.391049, -75.479426)
ON DUPLICATE KEY UPDATE direccion_texto = VALUES(direccion_texto);

INSERT INTO valoracion (id, usuario_id, taller_id, cita_id, estrellas, comentario, created_at) VALUES
(1, 2, 1, NULL, 4, 'Excelente servicio en el cambio de aceite.', NOW())
ON DUPLICATE KEY UPDATE comentario = VALUES(comentario);

INSERT INTO cita (id, usuario_id, taller_id, servicio_id, fecha_hora, estado, notas_usuario, created_at, updated_at) VALUES
(1, 2, 1, NULL,
 TIMESTAMP('2026-03-10 09:00:00'), 'solicitada', 'Cliente: Carlos Mendoza\nDetalle: Cambio de aceite y filtro', NOW(), NOW())
ON DUPLICATE KEY UPDATE notas_usuario = VALUES(notas_usuario);

INSERT INTO historial_busqueda (id, usuario_id, termino, origen_lat, origen_lng, radio_m, resultados, created_at) VALUES
(1, 2, 'Mecánica general - Cartagena', 10.39, -75.47, 5000, 3, NOW())
ON DUPLICATE KEY UPDATE termino = VALUES(termino);
