-- Ejecutar como root en MySQL (usuario de conexion de la app).
-- Despues ejecutar: sql/02_asistencia_en_carretera.sql
--
-- Spring conecta por 127.0.0.1; MySQL distingue localhost de 127.0.0.1.
-- Por eso el usuario debe existir en ambos hosts.

CREATE USER IF NOT EXISTS 'taller_app'@'localhost' IDENTIFIED BY 'taller123';
CREATE USER IF NOT EXISTS 'taller_app'@'127.0.0.1' IDENTIFIED BY 'taller123';

GRANT ALL PRIVILEGES ON asistencia_en_carretera.* TO 'taller_app'@'localhost';
GRANT ALL PRIVILEGES ON asistencia_en_carretera.* TO 'taller_app'@'127.0.0.1';

FLUSH PRIVILEGES;
