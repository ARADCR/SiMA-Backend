package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dispositivos_iot", uniqueConstraints = {
        // Garantiza máximo un dispositivo del mismo tipo por adulto mayor
        @UniqueConstraint(name = "uq_adulto_tipo_dispositivo", columnNames = { "id_adulto", "tipo_dispositivo" })
})
@Getter
@Setter
@NoArgsConstructor
public class DispositivoIot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Integer idDispositivo;

    @Column(name = "identificador_fisico", nullable = false, unique = true, length = 80)
    private String identificadorFisico; // MAC address o código físico único

    @Column(name = "tipo_dispositivo", nullable = false, length = 50)
    private String tipoDispositivo; // "pastillero_esp32" | "pulsera_inteligente"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto") // NULL = sin asignar
    private AdultoMayor adulto;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    // Relaciones
    @OneToMany(mappedBy = "dispositivo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<EventoIot> eventos;
}