-- Actualiza una base YA INSTALADA (creada con una version anterior de 02) a la agenda con horarios y mecanicos.
-- Si instalas desde cero con 02_asistencia_en_carretera.sql NO necesitas este script.
-- Ejecutar UNA sola vez, como root:  mysql -u root -p < sql/03_agenda_horarios_y_mecanicos.sql
-- Haz una copia de seguridad antes (mysqldump).

USE asistencia_en_carretera;

-- 1) Horario estructurado: una fila por dia abierto
CREATE TABLE IF NOT EXISTS horario_taller (
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

-- 2) Convierte el texto de taller.horario ("Lun - Sab 8:00 a 18:00", "Todos los dias 7:00 a 19:00",
--    "Mar - Dom 9:00 a 18:00") a filas. Los textos que no siguen ese formato se listan en el paso 4.
INSERT IGNORE INTO horario_taller (taller_id, dia_semana, apertura, cierre)
SELECT x.id, d.n, x.apertura, x.cierre
FROM (
    SELECT a.id, a.apertura, a.cierre,
           CASE WHEN a.todos THEN 1 ELSE CASE LOWER(LEFT(a.primero, 3))
                WHEN 'lun' THEN 1 WHEN 'mar' THEN 2 WHEN 'mie' THEN 3 WHEN 'jue' THEN 4
                WHEN 'vie' THEN 5 WHEN 'sab' THEN 6 WHEN 'dom' THEN 7 END END AS ini,
           CASE WHEN a.todos THEN 7 ELSE CASE LOWER(LEFT(a.ultimo, 3))
                WHEN 'lun' THEN 1 WHEN 'mar' THEN 2 WHEN 'mie' THEN 3 WHEN 'jue' THEN 4
                WHEN 'vie' THEN 5 WHEN 'sab' THEN 6 WHEN 'dom' THEN 7 END END AS fin
    FROM (
        SELECT t.id,
               STR_TO_DATE(REGEXP_SUBSTR(t.horario, '[0-9]{1,2}:[0-9]{2}', 1, 1), '%H:%i') AS apertura,
               STR_TO_DATE(REGEXP_SUBSTR(t.horario, '[0-9]{1,2}:[0-9]{2}', 1, 2), '%H:%i') AS cierre,
               LOWER(t.horario) LIKE 'todos%' AS todos,
               SUBSTRING_INDEX(t.horario, ' ', 1) AS primero,
               SUBSTRING_INDEX(SUBSTRING_INDEX(t.horario, ' - ', -1), ' ', 1) AS ultimo
        FROM taller t
        WHERE t.horario REGEXP '[0-9]{1,2}:[0-9]{2}.*[0-9]{1,2}:[0-9]{2}'
    ) a
) x
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) d
  ON d.n BETWEEN x.ini AND x.fin
WHERE x.ini IS NOT NULL AND x.fin IS NOT NULL AND x.apertura < x.cierre;

-- 3) Cada taller necesita al menos un mecanico activo para recibir citas
INSERT INTO mecanico (taller_id, nombres, apellidos, activo)
SELECT t.id, 'Mecanico', 'Principal', 1
FROM taller t
WHERE NOT EXISTS (SELECT 1 FROM mecanico m WHERE m.taller_id = t.id AND m.activo = 1);

-- 4) La cita guarda el mecanico asignado (las citas anteriores quedan sin mecanico y ocupan un cupo del turno)
ALTER TABLE cita
    ADD COLUMN mecanico_id BIGINT NULL AFTER servicio_id,
    ADD KEY idx_cita_taller_fecha (taller_id, fecha_hora),
    ADD KEY idx_cita_mecanico_fecha (mecanico_id, fecha_hora),
    ADD CONSTRAINT fk_cita_mecanico
        FOREIGN KEY (mecanico_id) REFERENCES mecanico (id)
        ON DELETE SET NULL ON UPDATE CASCADE;

-- 5) REVISA: talleres cuyo horario no se pudo convertir (el dueno debe configurarlo en "Datos de mi taller").
--    Sin horario estructurado un taller no puede recibir citas.
SELECT t.id, t.nombre, t.horario AS horario_texto_original
FROM taller t
LEFT JOIN horario_taller h ON h.taller_id = t.id
WHERE h.taller_id IS NULL;

-- 6) Cuando todo este bien, la columna de texto ya no se usa (la app no la lee ni la escribe):
-- ALTER TABLE taller DROP COLUMN horario;
