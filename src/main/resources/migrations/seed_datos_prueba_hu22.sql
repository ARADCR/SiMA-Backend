-- Seed de datos de prueba para verificar HU-22 (Análisis inteligente de adherencia)
-- Crea un Adulto Mayor demo con 28 días de RegistroToma con patrones detectables:
--   - Metformina 08:00, método IoT  -> 100% de cumplimiento
--   - Aspirina   08:00, método manual -> adherencia más baja que el IoT (omitida cada 5 días)
--   - Losartán   20:00, método manual -> omitida sistemáticamente los fines de semana
--
-- USO:
-- 1) Reemplazá 'tu-correo@ejemplo.com' por el correo de tu usuario Familiar o Cuidador ya existente
--    (el que vas a usar para loguearte y ver el reporte).
-- 2) Ejecutá el script completo contra `sima_db`.
-- 3) Entrá al sistema con ese usuario, andá al adulto "Elena Demo" y mirá Reportes de Medicación.

USE sima_db;

-- 0) Resolver el id del usuario dueño de la relación (Familiar o Cuidador)
SET @correo_usuario = 'tu-correo@ejemplo.com';
SELECT id_usuario INTO @id_usuario FROM usuarios WHERE correo = @correo_usuario COLLATE utf8mb4_unicode_ci LIMIT 1;

-- Chequeo defensivo: si no se encontró el usuario, frená acá (ver mensaje de error)
SELECT IF(@id_usuario IS NULL,
    'ERROR: no existe ningún usuario con ese correo. Revisá @correo_usuario antes de seguir.',
    CONCAT('OK: usuario encontrado, id_usuario = ', @id_usuario)
) AS chequeo_usuario;

-- 1) Adulto Mayor demo
INSERT INTO adultos_mayores (nombre, apellido, fecha_nacimiento, condiciones_medicas, contacto_medico, activo)
VALUES ('Elena', 'Demo HU22', '1948-03-12',
        'Hipertensión arterial, diabetes tipo 2 controlada con Metformina',
        'Dr. Ramírez - Clínica San Rafael - 555-0100', TRUE);
SET @id_adulto = LAST_INSERT_ID();

-- 2) Vincular al usuario existente como Familiar (o Cuidador, según tu rol) con el adulto demo
INSERT INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia)
SELECT @id_usuario, @id_adulto,
       CASE WHEN r.nombre_rol = 'Cuidador' THEN 'cuidador_asignado' ELSE 'familiar' END,
       TRUE
FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
WHERE u.id_usuario = @id_usuario;

-- 3) Medicamentos
INSERT INTO medicamentos (id_adulto, nombre, dosis, frecuencia_horas, activo, principio_activo, prescrito_por)
VALUES (@id_adulto, 'Metformina', '850mg', 12, TRUE, 'Metformina', 'Dr. Ramírez');
SET @id_med_metformina = LAST_INSERT_ID();

INSERT INTO medicamentos (id_adulto, nombre, dosis, frecuencia_horas, activo, principio_activo, prescrito_por)
VALUES (@id_adulto, 'Aspirina', '100mg', 24, TRUE, 'Ácido acetilsalicílico', 'Dr. Ramírez');
SET @id_med_aspirina = LAST_INSERT_ID();

INSERT INTO medicamentos (id_adulto, nombre, dosis, frecuencia_horas, activo, principio_activo, prescrito_por)
VALUES (@id_adulto, 'Losartán', '50mg', 24, TRUE, 'Losartán potásico', 'Dr. Ramírez');
SET @id_med_losartan = LAST_INSERT_ID();

-- 4) Horarios (uno por medicamento)
INSERT INTO horarios_medicamento (id_medicamento, hora_programada, activo) VALUES (@id_med_metformina, '08:00:00', TRUE);
SET @id_hor_metformina = LAST_INSERT_ID();

INSERT INTO horarios_medicamento (id_medicamento, hora_programada, activo) VALUES (@id_med_aspirina, '08:00:00', TRUE);
SET @id_hor_aspirina = LAST_INSERT_ID();

INSERT INTO horarios_medicamento (id_medicamento, hora_programada, activo) VALUES (@id_med_losartan, '20:00:00', TRUE);
SET @id_hor_losartan = LAST_INSERT_ID();

-- 5) 28 días de RegistroToma (hoy y los 27 días anteriores)
-- Serie de fechas vía CTE recursivo (MySQL 8+)
INSERT INTO registros_toma (id_horario, id_adulto, id_usuario_confirmador, estado, metodo_confirmacion, fecha_hora_programada, fecha_hora_registro, observacion)
WITH RECURSIVE dias (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM dias WHERE n < 27
)
SELECT
    @id_hor_metformina,
    @id_adulto,
    NULL,
    'tomado',
    'iot_pastillero',
    TIMESTAMP(CURDATE() - INTERVAL n DAY, '08:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL n DAY, '08:03:00'),
    NULL
FROM dias;
-- Metformina: 100% de cumplimiento, siempre por IoT

INSERT INTO registros_toma (id_horario, id_adulto, id_usuario_confirmador, estado, metodo_confirmacion, fecha_hora_programada, fecha_hora_registro, observacion)
WITH RECURSIVE dias (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM dias WHERE n < 27
)
SELECT
    @id_hor_aspirina,
    @id_adulto,
    CASE WHEN n % 5 = 0 THEN NULL ELSE @id_usuario END,
    CASE WHEN n % 5 = 0 THEN 'omitido' ELSE 'tomado' END,
    CASE WHEN n % 5 = 0 THEN NULL ELSE 'manual_cuidador' END,
    TIMESTAMP(CURDATE() - INTERVAL n DAY, '08:00:00'),
    CASE WHEN n % 5 = 0 THEN NULL ELSE TIMESTAMP(CURDATE() - INTERVAL n DAY, '08:10:00') END,
    NULL
FROM dias;
-- Aspirina: confirmación manual, omitida 1 de cada 5 días (~80% de cumplimiento, peor que el IoT)

INSERT INTO registros_toma (id_horario, id_adulto, id_usuario_confirmador, estado, metodo_confirmacion, fecha_hora_programada, fecha_hora_registro, observacion)
WITH RECURSIVE dias (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM dias WHERE n < 27
)
SELECT
    @id_hor_losartan,
    @id_adulto,
    CASE WHEN DAYOFWEEK(CURDATE() - INTERVAL n DAY) IN (1, 7) THEN NULL ELSE @id_usuario END,
    CASE WHEN DAYOFWEEK(CURDATE() - INTERVAL n DAY) IN (1, 7) THEN 'omitido' ELSE 'tomado' END,
    CASE WHEN DAYOFWEEK(CURDATE() - INTERVAL n DAY) IN (1, 7) THEN NULL ELSE 'manual_cuidador' END,
    TIMESTAMP(CURDATE() - INTERVAL n DAY, '20:00:00'),
    CASE WHEN DAYOFWEEK(CURDATE() - INTERVAL n DAY) IN (1, 7) THEN NULL ELSE TIMESTAMP(CURDATE() - INTERVAL n DAY, '20:15:00') END,
    NULL
FROM dias;
-- Losartán 20:00: omitido sistemáticamente sábados y domingos (DAYOFWEEK 1=domingo, 7=sábado en MySQL)

-- 6) Verificación rápida
SELECT 'Adulto demo creado' AS paso, @id_adulto AS id_adulto;

SELECT m.nombre, h.hora_programada,
       SUM(rt.estado = 'tomado') AS tomados,
       SUM(rt.estado = 'omitido') AS omitidos,
       COUNT(*) AS total,
       ROUND(SUM(rt.estado = 'tomado') / COUNT(*) * 100, 1) AS pct_adherencia
FROM registros_toma rt
JOIN horarios_medicamento h ON h.id_horario = rt.id_horario
JOIN medicamentos m ON m.id_medicamento = h.id_medicamento
WHERE rt.id_adulto = @id_adulto
GROUP BY m.nombre, h.hora_programada;
