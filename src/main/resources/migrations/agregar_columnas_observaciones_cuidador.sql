-- ============================================================
-- Migración: Agregar columnas de signos vitales a observaciones_cuidador
-- Fecha: 2026-07-13
-- Motivo: HU-15 (Notas de observación) agrega los campos urgencia,
--         tension_arterial, frecuencia_cardiaca y temperatura a la
--         entidad ObservacionCuidador. Hibernate corre en modo
--         validate, así que el esquema debe actualizarse a mano
--         antes de levantar el backend.
-- ============================================================

ALTER TABLE observaciones_cuidador
    ADD COLUMN urgencia VARCHAR(20) NOT NULL DEFAULT 'BAJA',
    ADD COLUMN tension_arterial VARCHAR(20) NULL,
    ADD COLUMN frecuencia_cardiaca VARCHAR(20) NULL,
    ADD COLUMN temperatura VARCHAR(20) NULL;

-- El DEFAULT 'BAJA' en urgencia es solo para no romper filas existentes;
-- la entidad la exige en cada insert nuevo (nullable = false, sin default).
ALTER TABLE observaciones_cuidador
    ALTER COLUMN urgencia DROP DEFAULT;

-- Verificación:
-- DESCRIBE observaciones_cuidador;
