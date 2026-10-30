package com.beathub.multiarenas.delivery.auth.repository;

import com.beathub.multiarenas.delivery.auth.entity.RunnerZona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RunnerZonaRepository extends JpaRepository<RunnerZona, Long> {

    List<RunnerZona> findByUsuarioIdAndArenaIdAndActivoTrueOrderByPrioridadAsc(Long usuarioId, Long arenaId);

    List<RunnerZona> findByUsuarioId(Long usuarioId);

    List<RunnerZona> findByArenaIdAndZonaIdAndActivoTrueOrderByPrioridadAsc(Long arenaId, Long zonaId);

    List<RunnerZona> findByArenaIdAndActivoTrue(Long arenaId);

    Optional<RunnerZona> findByUsuarioIdAndArenaIdAndZonaId(Long usuarioId, Long arenaId, Long zonaId);

    void deleteByUsuarioIdAndArenaId(Long usuarioId, Long arenaId);

    @Query("SELECT rz FROM RunnerZona rz JOIN FETCH rz.usuario u " +
           "WHERE rz.arenaId = :arenaId AND rz.zonaId = :zonaId AND rz.activo = true AND u.estadoId = 1 " +
           "ORDER BY rz.prioridad ASC")
    List<RunnerZona> findActiveRunnersByArenaAndZona(@Param("arenaId") Long arenaId, @Param("zonaId") Long zonaId);
}

