-- Seed de datos de prueba para verificar HU-24 (Alertas inteligentes)
-- Crea un Adulto Mayor demo vinculado a un Familiar, con alertas de los últimos 7 días
-- que incluyen un patrón de omisión repetida del mismo medicamento (para que la IA detecte
-- una "escalada") y una mezcla de críticas/informativas/resueltas para el día de hoy.
--
-- USO:
-- 1) Reemplazá '@correo_familiar' por el correo de un usuario Familiar YA EXISTENTE.
-- 2) Ejecutá el script COMPLETO de una sola vez (no statement por statement):
--    mysql -u root -p sima_db < seed_datos_prueba_hu24.sql
-- 3) Entrá como ese Familiar -> Dashboard -> deberías ver el card "Resumen IA" con:
--    - Un resumen ejecutivo mencionando la omisión repetida de Losartán como crítica.
--    - Un banner de escalada: "3ra omisión de Losartán esta semana" (o similar).
--    - Contadores de informativas/resueltas.

USE sima_db;

-- 0) Resolver el usuario Familiar existente
SET @correo_familiar = 'tu-correo-familiar@ejemplo.com';
SELECT id_usuario INTO @id_familiar FROM usuarios WHERE correo = @correo_familiar COLLATE utf8mb4_unicode_ci LIMIT 1;

SELECT IF(@id_familiar IS NULL, 'ERROR: no existe usuario Familiar con ese correo', CONCAT('OK familiar, id=', @id_familiar)) AS chequeo_familiar;

-- 1) Adulto Mayor demo (hipertenso, en tratamiento con Losartán)
INSERT INTO adultos_mayores (nombre, apellido, fecha_nacimiento, condiciones_medicas, contacto_medico, activo)
VALUES ('Marta', 'Demo HU24', '1945-09-10',
        'Hipertensión arterial tratada con Losartán 50mg. Requiere seguimiento estricto de la medicación.',
        'Dr. Herrera - Clínica Vida - 555-0300', TRUE);
SET @id_adulto = LAST_INSERT_ID();

-- 2) Vincular el Familiar al adulto
INSERT INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia)
VALUES (@id_familiar, @id_adulto, 'familiar', TRUE);

-- 3) Alertas de los últimos 7 días — patrón de omisión repetida de Losartán (para escalada)

-- Hace 6 días: 1ra omisión de Losartán
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'DOSIS_NO_TOMADA', 'Marta no confirmó la toma de Losartán 50mg de las 20:00.', TRUE, NOW() - INTERVAL 6 DAY);

-- Hace 5 días: recordatorio normal, resuelto (para contar como informativa/resuelta)
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'RECORDATORIO_MEDICAMENTO', 'Recordatorio: Metformina 850mg programada para las 08:00.', TRUE, NOW() - INTERVAL 5 DAY);

-- Hace 3 días: 2da omisión de Losartán
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'DOSIS_NO_TOMADA', 'Marta no confirmó la toma de Losartán 50mg de las 20:00.', TRUE, NOW() - INTERVAL 3 DAY);

-- Hace 1 día: recordatorio normal, resuelto
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'RECORDATORIO_MEDICAMENTO', 'Recordatorio: Metformina 850mg programada para las 08:00.', TRUE, NOW() - INTERVAL 1 DAY);

-- 4) Alertas de HOY — mezcla de crítica / informativa / resuelta

-- HOY: 3ra omisión de Losartán esta semana (la crítica que debería disparar la escalada)
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'DOSIS_NO_TOMADA', 'Marta no confirmó la toma de Losartán 50mg de las 20:00.', FALSE, NOW() - INTERVAL 2 HOUR);

-- HOY: recordatorio de rutina, ya resuelto (informativa)
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'RECORDATORIO_MEDICAMENTO', 'Recordatorio: Metformina 850mg programada para las 08:00.', TRUE, NOW() - INTERVAL 8 HOUR);

-- HOY: recordatorio de rutina, aún sin marcar resuelto (informativa)
INSERT INTO alertas (id_adulto, tipo_alerta, mensaje, resuelta, creado_en)
VALUES (@id_adulto, 'RECORDATORIO_MEDICAMENTO', 'Recordatorio: Losartán 50mg programada para las 20:00 de mañana.', FALSE, NOW() - INTERVAL 30 MINUTE);

-- 5) Verificación rápida
SELECT 'Adulto demo creado' AS paso, @id_adulto AS id_adulto;

SELECT id_alerta, tipo_alerta, resuelta, creado_en, LEFT(mensaje, 60) AS mensaje_preview
FROM alertas
WHERE id_adulto = @id_adulto
ORDER BY creado_en ASC;
