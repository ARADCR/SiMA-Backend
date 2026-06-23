package com.sima.backend.repository;

import com.sima.backend.entity.RegistroToma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroTomaRepository extends JpaRepository<RegistroToma, Integer> {

    // Registros del día actual para un adulto (para chatbot y dashboard)
    @Query("""
            SELECT rt FROM RegistroToma rt
            WHERE rt.adulto.idAdulto = :idAdulto
              AND rt.fechaHoraProgramada >= :inicioDia
              AND rt.fechaHoraProgramada < :finDia
            ORDER BY rt.fechaHoraProgramada
            """)
    List<RegistroToma> findTomasDelDia(@Param("idAdulto") Integer idAdulto,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    // Próxima toma pendiente de un adulto
    @Query("""
            SELECT rt FROM RegistroToma rt
            WHERE rt.adulto.idAdulto = :idAdulto
              AND rt.estado = 'pendiente'
              AND rt.fechaHoraProgramada > :ahora
            ORDER BY rt.fechaHoraProgramada ASC
            """)
    Optional<RegistroToma> findProximaToma(@Param("idAdulto") Integer idAdulto,
            @Param("ahora") LocalDateTime ahora);

    // Tomas pendientes vencidas (para el scheduler de alertas por omisión)
    @Query("""
            SELECT rt FROM RegistroToma rt
            WHERE rt.estado = 'pendiente'
              AND rt.fechaHoraProgramada <= :limite
            """)
    List<RegistroToma> findTomasPendientesVencidas(@Param("limite") LocalDateTime limite);

    // Historial de tomas por adulto en un rango de fechas
    @Query("""
            SELECT rt FROM RegistroToma rt
            WHERE rt.adulto.idAdulto = :idAdulto
              AND rt.fechaHoraProgramada BETWEEN :desde AND :hasta
            ORDER BY rt.fechaHoraProgramada DESC
            """)
    List<RegistroToma> findHistorialByAdultoAndRango(@Param("idAdulto") Integer idAdulto,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Cumplimiento semanal: total programadas y tomadas por día (últimos 7 días)
    @Query("""
            SELECT
                CAST(rt.fechaHoraProgramada AS LocalDate) AS fecha,
                COUNT(rt) AS totalProgramadas,
                SUM(CASE WHEN rt.estado IN ('tomado', 'confirmado_manual') THEN 1 ELSE 0 END) AS totalTomadas
            FROM RegistroToma rt
            WHERE rt.adulto.idAdulto = :idAdulto
              AND rt.fechaHoraProgramada >= :desde
            GROUP BY CAST(rt.fechaHoraProgramada AS LocalDate)
            ORDER BY fecha
            """)
    List<Object[]> findCumplimientoSemanal(@Param("idAdulto") Integer idAdulto,
            @Param("desde") LocalDateTime desde);

    // Actualizar estado de una toma (permitido según reglas de inmutabilidad)
    @Modifying
    @Query("""
            UPDATE RegistroToma rt
            SET rt.estado = :estado,
                rt.metodoConfirmacion = :metodo,
                rt.fechaHoraRegistro = :fechaRegistro,
                rt.usuarioConfirmador.idUsuario = :idUsuario
            WHERE rt.idRegistro = :idRegistro
              AND rt.estado = 'pendiente'
            """)
    int confirmarToma(@Param("idRegistro") Integer idRegistro,
            @Param("estado") String estado,
            @Param("metodo") String metodo,
            @Param("fechaRegistro") LocalDateTime fechaRegistro,
            @Param("idUsuario") Integer idUsuario);
}