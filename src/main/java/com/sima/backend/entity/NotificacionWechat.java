package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_wechat")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionWechat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_destino", nullable = false)
    private Usuario usuarioDestino;

    @Column(name = "wechat_openid", nullable = false, length = 100)
    private String wechatOpenid; // Snapshot del openid al momento del envío (auditoría)

    @Column(name = "estado_envio", nullable = false, length = 30)
    private String estadoEnvio = "pendiente";
    // Valores: "pendiente" | "enviado" | "fallido" | "reintentando"

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn; // Timestamp del envío exitoso
}