package com.sima.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecturas_pulsera")
@Getter
@Setter
@NoArgsConstructor
public class LecturaPulsera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura")
    private Long idLectura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    @JsonIgnore
    private DispositivoIot dispositivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    @JsonIgnore
    private AdultoMayor adulto;

    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "presion_sistolica")
    private Integer presionSistolica;

    @Column(name = "presion_diastolica")
    private Integer presionDiastolica;

    @Column(name = "pasos_diarios")
    private Integer pasosDiarios;

    @Column(name = "nivel_bateria")
    private Integer nivelBateria;

    @Column(name = "fecha_medicion", nullable = false)
    private LocalDateTime fechaMedicion;

    @CreationTimestamp
    @Column(name = "fecha_recepcion", nullable = false, updatable = false)
    private LocalDateTime fechaRecepcion;
}
