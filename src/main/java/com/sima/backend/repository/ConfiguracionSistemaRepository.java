package com.sima.backend.repository;

import com.sima.backend.entity.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Integer> {

    // Buscar parámetro por clave (uso más frecuente)
    Optional<ConfiguracionSistema> findByClave(String clave);

    // Verificar si existe una clave
    boolean existsByClave(String clave);
}