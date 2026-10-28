package com.beathub.multiarenas.delivery.auth.repository;

import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findBySsoId(String ssoId);

    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE u.username = :login OR p.email = :login")
    Optional<Usuario> findByUsernameOrEmail(@Param("login") String login);

    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE p.email = :email")
    Optional<Usuario> findByPersonaEmail(@Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsBySsoId(String ssoId);
}
