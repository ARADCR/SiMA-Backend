package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_config")
    private Integer idConfig;

    @Column(name = "clave", nullable = false, unique = true, length = 100)
    private String clave;
    // Ej: "tolerancia_minutos", "umbral_caida", "max_intentos_notificacion"

    @Column(name = "valor", nullable = false, columnDefinition = "TEXT")
    private String valor;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Se actualiza automáticamente con @PreUpdate
    @Column(name = "modificado_en", nullable = false)
    private LocalDateTime modificadoEn;

    @PrePersist
    @PreUpdate
    public void actualizarFecha() {
        this.modificadoEn = LocalDateTime.now();
    }
}