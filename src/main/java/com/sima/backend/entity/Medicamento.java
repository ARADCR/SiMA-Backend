package com.sima.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "medicamentos")
@Getter
@Setter
@NoArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicamento")
    private Integer idMedicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    private AdultoMayor adulto;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "dosis", nullable = false, length = 80)
    private String dosis; // Ej: "1 tableta", "5ml"

    @Min(value = 1, message = "La frecuencia debe ser mayor a 0")
    @Column(name = "frecuencia_horas", nullable = false)
    private Integer frecuenciaHoras; // Solo informativo, la lógica real está en horarios

    @Column(name = "compartimento", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer compartimento = 1;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "principio_activo", length = 150)
    private String principioActivo;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "stock_actual")
    private Integer stockActual;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "prescrito_por", length = 150)
    private String prescritoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Relaciones
    @OneToMany(mappedBy = "medicamento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioMedicamento> horarios;
}