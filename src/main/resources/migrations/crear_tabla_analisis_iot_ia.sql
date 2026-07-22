-- ============================================================
-- Migración: Crear tabla analisis_iot_ia
-- Fecha: 2026-07-21
-- Motivo: HU-26 · Detección de anomalías IoT con IA.
--         Cachea los resultados del análisis IA (bajo demanda o batch)
--         para evitar invocar al LLM en cada request.
-- ============================================================

CREATE TABLE analisis_iot_ia (
  id_analisis INT PRIMARY KEY AUTO_INCREMENT,
  id_adulto INT NOT NULL,
  resumen_estado TEXT,
  anomalias_json TEXT,
  tendencias_json TEXT,
  fecha_analisis DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_adulto) REFERENCES adultos_mayores(id_adulto)
);
