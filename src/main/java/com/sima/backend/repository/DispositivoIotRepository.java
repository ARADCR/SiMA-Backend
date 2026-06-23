package com.sima.backend.repository;

import com.sima.backend.entity.DispositivoIot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoIotRepository extends JpaRepository<DispositivoIot, Integer> {

    // Buscar por MAC/identificador físico (registro de nuevo dispositivo)
    Optional<DispositivoIot> findByIdentificadorFisico(String identificadorFisico);

    // Verificar si ya existe el identificador físico
    boolean existsByIdentificadorFisico(String identificadorFisico);

    // Listar dispositivos de un adulto mayor
    List<DispositivoIot> findByAdulto_IdAdultoAndActivoTrue(Integer idAdulto);

    // Buscar dispositivo de un tipo específico para un adulto
    // (garantiza máximo uno de cada tipo por adulto)
    Optional<DispositivoIot> findByAdulto_IdAdultoAndTipoDispositivo(
            Integer idAdulto, String tipoDispositivo);

    // Listar dispositivos sin asignar
    List<DispositivoIot> findByAdultoIsNullAndActivoTrue();

    // Listar todos los dispositivos activos
    List<DispositivoIot> findByActivoTrue();

    // Desasignar dispositivo de un adulto (poner id_adulto a NULL)
    @Modifying
    @Query("UPDATE DispositivoIot d SET d.adulto = null WHERE d.idDispositivo = :id")
    void desasignarDispositivo(@Param("id") Integer idDispositivo);
}