-- ============================================================
-- Migración: Crear tabla perfil_cuidador
-- Fecha: 2026-07-20
-- Motivo: HU-21 · Análisis de perfil y certificaciones de cuidadores.
--         Tabla separada de usuarios porque el perfil profesional
--         solo aplica a usuarios con rol Cuidador.
-- ============================================================

CREATE TABLE perfil_cuidador (
  id_usuario INT PRIMARY KEY,
  descripcion_perfil TEXT,
  especialidades VARCHAR(500),
  experiencia_anios INT,
  certificaciones TEXT,
  resumen_ia TEXT,
  tags VARCHAR(500),
  perfil_analizado BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
