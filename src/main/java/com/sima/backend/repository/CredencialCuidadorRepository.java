package com.sima.backend.repository;

import com.sima.backend.entity.CredencialCuidador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CredencialCuidadorRepository extends JpaRepository<CredencialCuidador, Integer> {
    List<CredencialCuidador> findByCuidadorIdUsuarioOrderByFechaSubidaDesc(Integer idCuidador);
}
