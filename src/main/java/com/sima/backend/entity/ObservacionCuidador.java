package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones_cuidador")
@Getter
@Setter
@NoArgsConstructor
public class ObservacionCuidador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observacion")
    private Integer idObservacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuidador", nullable = false)
    private Usuario cuidador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    private AdultoMayor adulto;

    @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "urgencia", nullable = false, length = 20)
    private String urgencia;

    @Column(name = "tension_arterial", length = 20)
    private String tensionArterial;

    @Column(name = "frecuencia_cardiaca", length = 20)
    private String frecuenciaCardiaca;

    @Column(name = "temperatura", length = 20)
    private String temperatura;

    @CreationTimestamp
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;
}