-- Seed de datos de prueba para verificar HU-23 (IA en observaciones del cuidador)
-- Crea un Adulto Mayor demo vinculado a un Familiar y a un Cuidador, con observaciones
-- recientes (dentro de las últimas 72 horas) para probar:
--   1) GET /api/ai/observaciones/{idAdulto}/resumen  (Familiar o Cuidador)
--   2) POST /api/ai/observaciones/evaluar-urgencia   (Cuidador, cargando una obs NUEVA desde la UI)
--
-- USO:
-- 1) Reemplazá '@correo_familiar' y '@correo_cuidador' por correos de usuarios YA EXISTENTES
--    con esos roles (los que vas a usar para loguearte).
-- 2) Ejecutá el script COMPLETO de una sola vez (no statement por statement — las variables
--    de sesión @id_... se pierden si el cliente reconecta entre ejecuciones).
--    Con mysql CLI:  mysql -u root -p sima_db < seed_datos_prueba_hu23.sql
-- 3) Entrá como Familiar -> Observaciones del adulto "Roberto Demo HU23" -> deberías ver el
--    card "Resumen IA" destacando la observación urgente (TA 180/110) primero.
-- 4) Entrá como Cuidador -> Nueva observación -> cargá signos vitales (ej. TA 180/110,
--    frecuencia 98, temperatura 37.2) -> botón "Evaluar con IA" -> debería sugerir "urgente"
--    con justificación mencionando la hipertensión tratada con Losartán.

USE sima_db;

-- 0) Resolver los usuarios existentes por correo
SET @correo_familiar = 'tu-correo-familiar@ejemplo.com';
SET @correo_cuidador = 'tu-correo-cuidador@ejemplo.com';

SELECT id_usuario INTO @id_familiar FROM usuarios WHERE correo = @correo_familiar COLLATE utf8mb4_unicode_ci LIMIT 1;
SELECT id_usuario INTO @id_cuidador FROM usuarios WHERE correo = @correo_cuidador COLLATE utf8mb4_unicode_ci LIMIT 1;

-- Chequeo defensivo antes de seguir
SELECT
    IF(@id_familiar IS NULL, 'ERROR: no existe usuario Familiar con ese correo', CONCAT('OK familiar, id=', @id_familiar)) AS chequeo_familiar,
    IF(@id_cuidador IS NULL, 'ERROR: no existe usuario Cuidador con ese correo', CONCAT('OK cuidador, id=', @id_cuidador)) AS chequeo_cuidador;

-- 1) Adulto Mayor demo (hipertenso, en tratamiento con Losartán — para que la evaluación de
--    urgencia tenga contexto médico real que justifique "urgente" ante una TA alta)
INSERT INTO adultos_mayores (nombre, apellido, fecha_nacimiento, condiciones_medicas, contacto_medico, activo)
VALUES ('Roberto', 'Demo HU23', '1950-06-20',
        'Hipertensión arterial tratada con Losartán 50mg. Diabetes tipo 2 controlada con Metformina.',
        'Dra. Pérez - Clínica Santa Fe - 555-0200', TRUE);
SET @id_adulto = LAST_INSERT_ID();

-- 2) Vincular Familiar y Cuidador al adulto
INSERT INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia)
VALUES (@id_familiar, @id_adulto, 'familiar', TRUE);

INSERT INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia)
VALUES (@id_cuidador, @id_adulto, 'cuidador_asignado', FALSE);

-- 3) Medicamento activo (para que evaluar-urgencia tenga "medicamentos activos" en el contexto)
INSERT INTO medicamentos (id_adulto, nombre, dosis, frecuencia_horas, activo, principio_activo, prescrito_por)
VALUES (@id_adulto, 'Losartán', '50mg', 24, TRUE, 'Losartán potásico', 'Dra. Pérez');

-- 4) Observaciones de las últimas 72 horas (la IA analiza como máximo las últimas 10 o las de
--    las últimas 72h, lo que sea menor). Orden: de la más vieja a la más nueva.

-- Hace 60 horas: rutinaria, todo normal
INSERT INTO observaciones_cuidador (id_cuidador, id_adulto, texto, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora)
VALUES (@id_cuidador, @id_adulto,
        'Roberto amaneció de buen ánimo. Desayunó completo y caminó 15 minutos en el jardín.',
        'normal', '120/80', '72', '36.5',
        NOW() - INTERVAL 60 HOUR);

-- Hace 40 horas: rutinaria, todo normal
INSERT INTO observaciones_cuidador (id_cuidador, id_adulto, texto, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora)
VALUES (@id_cuidador, @id_adulto,
        'Almorzó bien, tomó su medicación de mediodía sin problemas. Estado de ánimo estable.',
        'normal', '118/78', '70', '36.6',
        NOW() - INTERVAL 40 HOUR);

-- Hace 22 horas: LA URGENTE — presión elevada, para que el resumen la destaque primero
INSERT INTO observaciones_cuidador (id_cuidador, id_adulto, texto, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora)
VALUES (@id_cuidador, @id_adulto,
        'Roberto se quejó de dolor de cabeza y mareo leve al levantarse de la siesta. Se le tomó la presión de inmediato.',
        'urgente', '180/110', '98', '37.8',
        NOW() - INTERVAL 22 HOUR);

-- Hace 10 horas: rutinaria, ya se normalizó (para mostrar seguimiento post-episodio urgente)
INSERT INTO observaciones_cuidador (id_cuidador, id_adulto, texto, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora)
VALUES (@id_cuidador, @id_adulto,
        'Se contactó al médico tratante tras el episodio de ayer. Roberto tomó su Losartán, se sintió mejor y cenó con normalidad.',
        'importante', '128/84', '76', '36.7',
        NOW() - INTERVAL 10 HOUR);

-- Hace 2 horas: rutinaria, todo normal (la más reciente)
INSERT INTO observaciones_cuidador (id_cuidador, id_adulto, texto, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora)
VALUES (@id_cuidador, @id_adulto,
        'Mañana tranquila, desayunó bien y participó de una llamada con la familia.',
        'normal', '122/80', '74', '36.5',
        NOW() - INTERVAL 2 HOUR);

-- 5) Verificación rápida
SELECT 'Adulto demo creado' AS paso, @id_adulto AS id_adulto;

SELECT id_observacion, urgencia, tension_arterial, frecuencia_cardiaca, temperatura, fecha_hora, LEFT(texto, 60) AS texto_preview
FROM observaciones_cuidador
WHERE id_adulto = @id_adulto
ORDER BY fecha_hora ASC;
