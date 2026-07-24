package com.sima.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perfil_cuidador")
@Getter
@Setter
@NoArgsConstructor
public class PerfilCuidador {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "descripcion_perfil", columnDefinition = "TEXT")
    private String descripcionPerfil;

    @Column(name = "especialidades", length = 500)
    private String especialidades;

    @Column(name = "experiencia_anios")
    private Integer experienciaAnios;

    @Column(name = "certificaciones", columnDefinition = "TEXT")
    private String certificaciones;

    @Column(name = "resumen_ia", columnDefinition = "TEXT")
    private String resumenIa;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "perfil_analizado", nullable = false)
    private Boolean perfilAnalizado = false;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "tarifa_hora", precision = 10, scale = 2)
    private java.math.BigDecimal tarifaHora;

    @Column(name = "disponibilidad", columnDefinition = "TEXT")
    private String disponibilidad;

    public PerfilCuidador(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }
}
