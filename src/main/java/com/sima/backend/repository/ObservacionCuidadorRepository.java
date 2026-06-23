package com.sima.backend.repository;

import com.sima.backend.entity.ObservacionCuidador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ObservacionCuidadorRepository extends JpaRepository<ObservacionCuidador, Integer> {

    // Observaciones de un adulto ordenadas por fecha (más reciente primero)
    List<ObservacionCuidador> findByAdulto_IdAdultoOrderByFechaHoraDesc(Integer idAdulto);

    // Observaciones de un adulto en un rango de fechas
    @Query("""
            SELECT o FROM ObservacionCuidador o
            WHERE o.adulto.idAdulto = :idAdulto
              AND o.fechaHora BETWEEN :desde AND :hasta
            ORDER BY o.fechaHora DESC
            """)
    List<ObservacionCuidador> findByAdultoAndRango(@Param("idAdulto") Integer idAdulto,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Observaciones registradas por un cuidador específico
    List<ObservacionCuidador> findByCuidador_IdUsuarioOrderByFechaHoraDesc(Integer idCuidador);

    // Observaciones del día de un adulto
    @Query("""
            SELECT o FROM ObservacionCuidador o
            WHERE o.adulto.idAdulto = :idAdulto
              AND o.fechaHora >= :inicioDia
              AND o.fechaHora < :finDia
            ORDER BY o.fechaHora DESC
            """)
    List<ObservacionCuidador> findObservacionesDelDia(@Param("idAdulto") Integer idAdulto,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);
}