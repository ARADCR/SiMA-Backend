package com.sima.backend.repository;

import com.sima.backend.entity.ReporteGenerado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Integer> {
    
    // Obtener los reportes más recientes
    List<ReporteGenerado> findTop10ByOrderByFechaGeneracionDesc();
}
