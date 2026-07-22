package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_generados")
@Getter
@Setter
@NoArgsConstructor
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo; // Semanal, Mensual, Trimestral

    @Column(name = "estado", nullable = false, length = 50)
    private String estado; // Completado, En proceso, Error

    @Column(name = "generado_por", length = 100)
    private String generadoPor; // Nombre del admin o "Sistema (auto)"

    @Column(name = "tipo_reporte", length = 50)
    private String tipoReporte; // General, Medicacion, Alertas

    @Column(name = "adulto_mayor_id")
    private Integer adultoMayorId;

    @Column(name = "adulto_mayor_nombre", length = 150)
    private String adultoMayorNombre;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @CreationTimestamp
    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;
}
