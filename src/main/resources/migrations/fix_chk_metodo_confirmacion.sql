-- Corrige el CHECK constraint 'chk_metodo_confirmacion' de la tabla `registros_toma`.
--
-- Motivo: el constraint solo permitía 'app', 'chatbot', 'iot_pastillero' y 'manual_cuidador'.
-- HU-13 requiere que el cuidador pueda omitir una toma manualmente desde su perfil, lo que
-- necesita un nuevo método de confirmación: 'omision_cuidador'.
-- Sin este fix, el INSERT/UPDATE en registros_toma lanza un 500 con:
--   "constraint 'chk_metodo_confirmacion' is violated"
--
-- USO: correr una sola vez contra `sima_db`.

USE sima_db;

ALTER TABLE registros_toma DROP CHECK chk_metodo_confirmacion;

ALTER TABLE registros_toma
    ADD CONSTRAINT chk_metodo_confirmacion CHECK (metodo_confirmacion IN (
        'app',
        'chatbot',
        'iot_pastillero',
        'manual_cuidador',
        'omision_cuidador'
    ));

-- Verificación
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'sima_db' AND CONSTRAINT_NAME = 'chk_metodo_confirmacion';
