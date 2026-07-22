package com.sima.backend.repository;

import com.sima.backend.entity.LecturaPulsera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturaPulseraRepository extends JpaRepository<LecturaPulsera, Long> {

    // Lecturas de un adulto ordenadas de la más reciente a la más antigua
    List<LecturaPulsera> findByAdulto_IdAdultoOrderByFechaMedicionDesc(Integer idAdulto);

    // Lectura más reciente de un adulto
    Optional<LecturaPulsera> findFirstByAdulto_IdAdultoOrderByFechaMedicionDesc(Integer idAdulto);

    // Lecturas de un dispositivo ordenadas de la más reciente a la más antigua
    List<LecturaPulsera> findByDispositivo_IdDispositivoOrderByFechaMedicionDesc(Integer idDispositivo);
}
