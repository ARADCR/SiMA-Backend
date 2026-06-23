package com.sima.backend.repository;

import com.sima.backend.entity.EventoIot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoIotRepository extends JpaRepository<EventoIot, Integer> {

    // Eventos sin procesar (para el scheduler de alertas)
    List<EventoIot> findByProcesadoFalse();

    // Eventos sin procesar por tipo (caídas pendientes de procesar)
    List<EventoIot> findByTipoEventoAndProcesadoFalse(String tipoEvento);

    // Historial de eventos de un dispositivo en un rango de fechas
    @Query("""
            SELECT e FROM EventoIot e
            WHERE e.dispositivo.idDispositivo = :idDispositivo
              AND e.fechaHora BETWEEN :desde AND :hasta
            ORDER BY e.fechaHora DESC
            """)
    List<EventoIot> findByDispositivoAndRango(@Param("idDispositivo") Integer idDispositivo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Historial de eventos de un adulto (a través del dispositivo)
    @Query("""
            SELECT e FROM EventoIot e
            WHERE e.dispositivo.adulto.idAdulto = :idAdulto
              AND e.fechaHora BETWEEN :desde AND :hasta
            ORDER BY e.fechaHora DESC
            """)
    List<EventoIot> findByAdultoAndRango(@Param("idAdulto") Integer idAdulto,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Marcar evento como procesado
    @Modifying
    @Query("UPDATE EventoIot e SET e.procesado = true WHERE e.idEvento = :id")
    void marcarComoProcesado(@Param("id") Integer idEvento);

    // Marcar múltiples eventos como procesados (batch)
    @Modifying
    @Query("UPDATE EventoIot e SET e.procesado = true WHERE e.idEvento IN :ids")
    void marcarVariosComoProcesados(@Param("ids") List<Integer> ids);
}