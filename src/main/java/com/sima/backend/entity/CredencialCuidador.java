package com.sima.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "credenciales_cuidador")
public class CredencialCuidador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credencial")
    private Integer idCredencial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuidador", nullable = false)
    private Usuario cuidador;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(nullable = false, length = 20)
    private String estado; // pendiente, verificado, rechazado

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;
}
