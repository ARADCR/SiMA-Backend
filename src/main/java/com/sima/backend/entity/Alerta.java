package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alertas")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    private AdultoMayor adulto;

    @Column(name = "tipo_alerta", nullable = false, length = 50)
    private String tipoAlerta;
    // Valores: "omision_medicacion" | "caida_detectada" | "emergencia" |
    // "bateria_baja"

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Origen: toma omitida (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_registro")
    private RegistroToma registro;

    // Origen: evento IoT (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento_iot")
    private EventoIot eventoIot;

    @Column(name = "resuelta", nullable = false)
    private Boolean resuelta = false;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Relaciones
    @OneToMany(mappedBy = "alerta", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<NotificacionWechat> notificaciones;
}