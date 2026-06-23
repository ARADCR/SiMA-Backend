package com.sima.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Relaciones
    @OneToMany(mappedBy = "medicamento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioMedicamento> horarios;
}