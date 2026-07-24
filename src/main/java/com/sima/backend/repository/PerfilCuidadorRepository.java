package com.sima.backend.repository;

import com.sima.backend.entity.PerfilCuidador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilCuidadorRepository extends JpaRepository<PerfilCuidador, Integer> {

    Optional<PerfilCuidador> findByIdUsuario(Integer idUsuario);
}
