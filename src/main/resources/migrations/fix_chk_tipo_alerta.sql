-- Corrige el CHECK constraint 'chk_tipo_alerta' de la tabla `alertas`.
--
-- Motivo: el constraint solo permitía 'omision_medicacion', 'caida_detectada', 'emergencia'
-- y 'bateria_baja'. Esto rompía dos flujos:
--   1) HU-23 (IA en observaciones del cuidador): al aceptar una sugerencia de urgencia "urgente",
--      ObservacionService intenta insertar una Alerta de tipo 'urgencia_signos_vitales', que no
--      estaba permitida — la violación del constraint revertía la transacción completa, así que
--      ni la alerta ni la observación se guardaban.
--   2) RecordatorioScheduler (bug preexistente, no relacionado con HU-23): inserta alertas con
--      tipo 'DOSIS_NO_TOMADA' y 'RECORDATORIO_MEDICAMENTO', que tampoco estaban permitidos —
--      quedaría latente hasta la primera vez que el scheduler dispare esa alerta.
--
-- USO: correr una sola vez contra `sima_db`.

USE sima_db;

ALTER TABLE alertas DROP CHECK chk_tipo_alerta;

ALTER TABLE alertas
    ADD CONSTRAINT chk_tipo_alerta CHECK (tipo_alerta IN (
        'omision_medicacion',
        'caida_detectada',
        'emergencia',
        'bateria_baja',
        'urgencia_signos_vitales',
        'DOSIS_NO_TOMADA',
        'RECORDATORIO_MEDICAMENTO'
    ));

-- Verificación
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'sima_db' AND CONSTRAINT_NAME = 'chk_tipo_alerta';
