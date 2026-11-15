-- Ejecutar como root en MySQL (usuario de conexion de la app).
-- ANTES: cambia CAMBIA_ESTA_CLAVE por una clave propia (la misma que pondras en DB_PASSWORD).
-- Despues ejecutar: sql/02_asistencia_en_carretera.sql
--
-- Spring conecta por 127.0.0.1; MySQL distingue localhost de 127.0.0.1.
-- Por eso el usuario debe existir en ambos hosts.

CREATE USER IF NOT EXISTS 'taller_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
CREATE USER IF NOT EXISTS 'taller_app'@'127.0.0.1' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';

GRANT ALL PRIVILEGES ON asistencia_en_carretera.* TO 'taller_app'@'localhost';
GRANT ALL PRIVILEGES ON asistencia_en_carretera.* TO 'taller_app'@'127.0.0.1';

FLUSH PRIVILEGES;
