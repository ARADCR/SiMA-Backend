package com.sima.backend.repository;

import com.sima.backend.entity.NotificacionWechat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionWechatRepository extends JpaRepository<NotificacionWechat, Integer> {

    // Notificaciones pendientes o fallidas (para reintentos)
    List<NotificacionWechat> findByEstadoEnvioIn(List<String> estados);

    // Notificaciones con menos del máximo de intentos (para scheduler de reintento)
    @Query("""
            SELECT n FROM NotificacionWechat n
            WHERE n.estadoEnvio IN ('pendiente', 'reintentando')
              AND n.intentos < :maxIntentos
            ORDER BY n.alerta.creadoEn ASC
            """)
    List<NotificacionWechat> findPendientesParaEnvio(@Param("maxIntentos") Integer maxIntentos);

    // Notificaciones de una alerta específica
    List<NotificacionWechat> findByAlerta_IdAlerta(Integer idAlerta);

    // Marcar como enviado exitosamente
    @Modifying
    @Query("""
            UPDATE NotificacionWechat n
            SET n.estadoEnvio = 'enviado',
                n.enviadoEn = :fecha,
                n.intentos = n.intentos + 1
            WHERE n.idNotificacion = :id
            """)
    void marcarComoEnviada(@Param("id") Integer idNotificacion,
            @Param("fecha") LocalDateTime fecha);

    // Marcar como fallido e incrementar intentos
    @Modifying
    @Query("""
            UPDATE NotificacionWechat n
            SET n.estadoEnvio = 'fallido',
                n.intentos = n.intentos + 1
            WHERE n.idNotificacion = :id
            """)
    void marcarComoFallida(@Param("id") Integer idNotificacion);

    // Poner en estado reintentando
    @Modifying
    @Query("""
            UPDATE NotificacionWechat n
            SET n.estadoEnvio = 'reintentando',
                n.intentos = n.intentos + 1
            WHERE n.idNotificacion = :id
            """)
    void marcarComoReintentando(@Param("id") Integer idNotificacion);
}