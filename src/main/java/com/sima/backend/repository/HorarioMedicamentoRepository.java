package com.sima.backend.repository;

import com.sima.backend.entity.HorarioMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioMedicamentoRepository extends JpaRepository<HorarioMedicamento, Integer> {

    // Listar horarios activos de un medicamento
    List<HorarioMedicamento> findByMedicamento_IdMedicamentoAndActivoTrue(Integer idMedicamento);

    // Listar todos los horarios de un medicamento (activos e inactivos)
    List<HorarioMedicamento> findByMedicamento_IdMedicamento(Integer idMedicamento);

    // Verificar si ya existe ese horario para el mismo medicamento (constraint uq)
    boolean existsByMedicamento_IdMedicamentoAndHoraProgramada(
            Integer idMedicamento, LocalTime horaProgramada);

    // Obtener horario específico por medicamento y hora
    Optional<HorarioMedicamento> findByMedicamento_IdMedicamentoAndHoraProgramada(
            Integer idMedicamento, LocalTime horaProgramada);

    // Listar TODOS los horarios activos de un adulto mayor (para el scheduler)
    @Query("""
            SELECT h FROM HorarioMedicamento h
            JOIN h.medicamento m
            WHERE m.adulto.idAdulto = :idAdulto
              AND h.activo = true
              AND m.activo = true
            ORDER BY h.horaProgramada
            """)
    List<HorarioMedicamento> findHorariosActivosByAdulto(@Param("idAdulto") Integer idAdulto);

    // Listar todos los horarios activos que coinciden con una hora específica (para el scheduler)
    @Query("""
            SELECT h FROM HorarioMedicamento h
            JOIN h.medicamento m
            WHERE h.horaProgramada = :hora
              AND h.activo = true
              AND m.activo = true
            """)
    List<HorarioMedicamento> findByHoraProgramada(@Param("hora") LocalTime hora);

    // Desactivar horario (sin eliminar registros previos)
    @Modifying
    @Query("UPDATE HorarioMedicamento h SET h.activo = false WHERE h.idHorario = :id")
    void desactivarHorario(@Param("id") Integer idHorario);

    // Desactivar todos los horarios de un medicamento al desactivarlo
    @Modifying
    @Query("""
            UPDATE HorarioMedicamento h SET h.activo = false
            WHERE h.medicamento.idMedicamento = :idMedicamento
            """)
    void desactivarHorariosByMedicamento(@Param("idMedicamento") Integer idMedicamento);
}