-- ============================================================
-- Migración: Desactivar usuario de demo con rol "Adulto Mayor"
-- Fecha: 2026-07-04
-- Motivo: El adulto mayor ya no es un usuario del sistema.
--         El rol se conserva en la tabla roles por consistencia
--         del modelo de datos, pero ningún usuario activo debe
--         tener ese rol asignado.
-- ============================================================

-- Desactivar el usuario de demo adulto@sima.com
UPDATE usuarios
SET activo = false
WHERE correo = 'adulto@sima.com';

-- Verificación: no debe quedar ningún usuario activo con rol "Adulto Mayor"
-- SELECT u.correo, r.nombre_rol
-- FROM usuarios u
-- JOIN roles r ON u.id_rol = r.id_rol
-- WHERE r.nombre_rol = 'Adulto Mayor'
--   AND u.activo = true;
