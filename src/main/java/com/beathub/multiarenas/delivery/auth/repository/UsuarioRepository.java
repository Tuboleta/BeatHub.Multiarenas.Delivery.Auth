package com.beathub.multiarenas.delivery.auth.repository;

import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findBySsoId(String ssoId);

    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE u.username = :login OR p.email = :login")
    Optional<Usuario> findByUsernameOrEmail(@Param("login") String login);

    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE p.email = :email")
    Optional<Usuario> findByPersonaEmail(@Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsBySsoId(String ssoId);

    @Query(value = "SELECT DISTINCT u FROM Usuario u " +
            "LEFT JOIN u.persona p " +
            "LEFT JOIN u.arenas au " +
            "LEFT JOIN au.roles rau " +
            "LEFT JOIN rau.rol r " +
            "WHERE (:arenaId IS NULL OR au.arenaId = :arenaId) " +
            "AND (:estadoId IS NULL OR u.estadoId = :estadoId) " +
            "AND (:rol IS NULL OR UPPER(r.nombre) = UPPER(:rol) OR UPPER(r.nombre) = CONCAT('ROLE_', UPPER(:rol))) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(DISTINCT u.id) FROM Usuario u " +
            "LEFT JOIN u.persona p " +
            "LEFT JOIN u.arenas au " +
            "LEFT JOIN au.roles rau " +
            "LEFT JOIN rau.rol r " +
            "WHERE (:arenaId IS NULL OR au.arenaId = :arenaId) " +
            "AND (:estadoId IS NULL OR u.estadoId = :estadoId) " +
            "AND (:rol IS NULL OR UPPER(r.nombre) = UPPER(:rol) OR UPPER(r.nombre) = CONCAT('ROLE_', UPPER(:rol))) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Usuario> buscarUsuarios(
            @Param("arenaId") Long arenaId,
            @Param("estadoId") Integer estadoId,
            @Param("rol") String rol,
            @Param("search") String search,
            Pageable pageable);
}
