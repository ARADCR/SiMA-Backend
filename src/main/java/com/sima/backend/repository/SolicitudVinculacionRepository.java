package com.sima.backend.repository;

import com.sima.backend.entity.SolicitudVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudVinculacionRepository extends JpaRepository<SolicitudVinculacion, Integer> {
    List<SolicitudVinculacion> findByCuidador_IdUsuarioAndEstadoOrderByFechaCreacionDesc(Integer idCuidador, String estado);
    List<SolicitudVinculacion> findByFamiliar_IdUsuarioOrderByFechaCreacionDesc(Integer idFamiliar);
    List<SolicitudVinculacion> findTop5ByEstadoOrderByFechaCreacionDesc(String estado);
    boolean existsByFamiliar_IdUsuarioAndCuidador_IdUsuarioAndAdulto_IdAdultoAndEstado(Integer idFamiliar, Integer idCuidador, Integer idAdulto, String estado);
    int countByCuidador_IdUsuarioAndEstado(Integer idCuidador, String estado);
}
