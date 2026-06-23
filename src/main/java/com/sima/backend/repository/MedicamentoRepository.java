package com.sima.backend.repository;

import com.sima.backend.entity.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Integer> {

    // Listar medicamentos activos de un adulto
    List<Medicamento> findByAdulto_IdAdultoAndActivoTrue(Integer idAdulto);

    // Listar todos los medicamentos de un adulto (activos e inactivos)
    List<Medicamento> findByAdulto_IdAdulto(Integer idAdulto);

    // Buscar medicamento activo por ID (para validar antes de operar)
    Optional<Medicamento> findByIdMedicamentoAndActivoTrue(Integer idMedicamento);

    // Verificar que el medicamento pertenece al adulto (seguridad RBAC)
    boolean existsByIdMedicamentoAndAdulto_IdAdulto(Integer idMedicamento, Integer idAdulto);

    // Desactivar medicamento (soft delete, conserva historial)
    @Modifying
    @Query("UPDATE Medicamento m SET m.activo = false WHERE m.idMedicamento = :id")
    void desactivarMedicamento(@Param("id") Integer idMedicamento);

    // Contar medicamentos activos de un adulto
    long countByAdulto_IdAdultoAndActivoTrue(Integer idAdulto);
}