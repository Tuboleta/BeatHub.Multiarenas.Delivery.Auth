package com.beathub.multiarenas.delivery.auth.repository.specification;

import com.beathub.multiarenas.delivery.auth.entity.ArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Persona;
import com.beathub.multiarenas.delivery.auth.entity.Rol;
import com.beathub.multiarenas.delivery.auth.entity.RolArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(Long arenaId, Integer estadoId, String rol, String search) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (arenaId != null || StringUtils.hasText(rol)) {
                Join<Usuario, ArenaUsuario> arenasJoin = root.join("arenas", JoinType.LEFT);

                if (arenaId != null) {
                    predicates.add(cb.equal(arenasJoin.get("arenaId"), arenaId));
                }

                if (StringUtils.hasText(rol)) {
                    String rolNormalizado = rol.trim().toUpperCase();
                    if (rolNormalizado.startsWith("ROLE_")) {
                        rolNormalizado = rolNormalizado.substring(5);
                    }
                    Join<ArenaUsuario, RolArenaUsuario> rolArenaJoin = arenasJoin.join("roles", JoinType.LEFT);
                    Join<RolArenaUsuario, Rol> rolJoin = rolArenaJoin.join("rol", JoinType.LEFT);

                    Predicate p1 = cb.equal(cb.upper(rolJoin.get("nombre")), rolNormalizado);
                    Predicate p2 = cb.equal(cb.upper(rolJoin.get("nombre")), "ROLE_" + rolNormalizado);
                    predicates.add(cb.or(p1, p2));
                }
            }

            if (estadoId != null) {
                predicates.add(cb.equal(root.get("estadoId"), estadoId));
            }

            if (StringUtils.hasText(search)) {
                Join<Usuario, Persona> personaJoin = root.join("persona", JoinType.LEFT);
                String pattern = "%" + search.trim().toLowerCase() + "%";

                Predicate pUsername = cb.like(cb.lower(root.get("username")), pattern);
                Predicate pEmail = cb.like(cb.lower(personaJoin.get("email")), pattern);
                Predicate pNombres = cb.like(cb.lower(personaJoin.get("nombres")), pattern);
                Predicate pApellidos = cb.like(cb.lower(personaJoin.get("apellidos")), pattern);
                Predicate pDoc = cb.like(cb.lower(personaJoin.get("numeroDocumento")), pattern);

                predicates.add(cb.or(pUsername, pEmail, pNombres, pApellidos, pDoc));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

