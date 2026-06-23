package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "eventos_iot")
@Getter
@Setter
@NoArgsConstructor
public class EventoIot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    private DispositivoIot dispositivo;

    @Column(name = "tipo_evento", nullable = false, length = 60)
    private String tipoEvento;
    // Valores: "apertura_pastillero" | "deteccion_caida" | "ritmo_cardiaco" |
    // "pasos_diarios"

    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor; // BPM, pasos, etc. (opcional según tipo)

    @CreationTimestamp
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "procesado", nullable = false)
    private Boolean procesado = false; // FALSE = aún no evaluado por el sistema de alertas

    // Relaciones
    @OneToMany(mappedBy = "eventoIot", fetch = FetchType.LAZY)
    private List<Alerta> alertas;
}