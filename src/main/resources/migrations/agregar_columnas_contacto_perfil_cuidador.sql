-- ============================================================
-- Migración: Agregar columnas de contacto y condiciones a perfil_cuidador
-- Fecha: 2026-07-20
-- Motivo: HU-27 (Edición de datos de contacto y condiciones del perfil
--         de Cuidador) agrega los campos telefono, ciudad, tarifa_hora
--         y disponibilidad a la entidad PerfilCuidador. Hibernate corre
--         en modo validate, así que el esquema debe actualizarse a mano
--         antes de levantar el backend.
-- ============================================================

ALTER TABLE perfil_cuidador
    ADD COLUMN telefono VARCHAR(30) NULL,
    ADD COLUMN ciudad VARCHAR(100) NULL,
    ADD COLUMN tarifa_hora DECIMAL(10,2) NULL,
    ADD COLUMN disponibilidad TEXT NULL;

-- Verificación:
-- DESCRIBE perfil_cuidador;
