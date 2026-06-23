package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_log")
@Immutable // Hibernate no ejecutará UPDATE ni DELETE sobre esta entidad
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaLog {

    /*
     * Tabla append-only.
     * - @Immutable impide que Hibernate haga UPDATE en esta entidad.
     * - No se expone ningún endpoint de DELETE.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Integer idLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tabla_afectada", nullable = false, length = 80)
    private String tablaAfectada;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion; // "INSERT" | "UPDATE" | "DELETE"

    // JSON almacenado como String, mapeado al tipo JSON de MySQL 8
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "JSON")
    private String datosAnteriores; // NULL en INSERT

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "JSON")
    private String datosNuevos; // NULL en DELETE

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @CreationTimestamp
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;
}