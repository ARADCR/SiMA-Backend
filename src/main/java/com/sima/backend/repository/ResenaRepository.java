package com.sima.backend.repository;

import com.sima.backend.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    List<Resena> findByCuidador_IdUsuarioOrderByFechaCreacionDesc(Integer idCuidador);

    @Query("SELECT AVG(r.puntos) FROM Resena r WHERE r.cuidador.idUsuario = :idCuidador")
    Double obtenerPromedioPorCuidador(@Param("idCuidador") Integer idCuidador);

    boolean existsByCuidador_IdUsuarioAndFamiliar_IdUsuario(Integer idCuidador, Integer idFamiliar);
}
