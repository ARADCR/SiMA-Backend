package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Cache de resultados del análisis IA de anomalías IoT (HU-26).
 * Se persiste tanto en la ejecución bajo demanda como en la del job batch,
 * para que el endpoint pueda devolver el último análisis sin invocar al LLM
 * en cada request (ver IotAiService.obtenerUltimoAnalisis).
 */
@Entity
@Table(name = "analisis_iot_ia")
@Getter
@Setter
@NoArgsConstructor
public class AnalisisIotIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analisis")
    private Integer idAnalisis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    private AdultoMayor adulto;

    @Column(name = "resumen_estado", columnDefinition = "TEXT")
    private String resumenEstado;

    @Column(name = "anomalias_json", columnDefinition = "TEXT")
    private String anomaliasJson;

    @Column(name = "tendencias_json", columnDefinition = "TEXT")
    private String tendenciasJson;

    @CreationTimestamp
    @Column(name = "fecha_analisis", nullable = false, updatable = false)
    private LocalDateTime fechaAnalisis;
}
