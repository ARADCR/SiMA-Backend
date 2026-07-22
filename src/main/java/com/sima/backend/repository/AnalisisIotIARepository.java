package com.sima.backend.repository;

import com.sima.backend.entity.AnalisisIotIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalisisIotIARepository extends JpaRepository<AnalisisIotIA, Integer> {

    // Último análisis persistido de un adulto (para caché de 24h en obtenerUltimoAnalisis)
    Optional<AnalisisIotIA> findTopByAdulto_IdAdultoOrderByFechaAnalisisDesc(Integer idAdulto);
}
