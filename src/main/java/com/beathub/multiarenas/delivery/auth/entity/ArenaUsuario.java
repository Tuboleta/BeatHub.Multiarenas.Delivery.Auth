package com.beathub.multiarenas.delivery.auth.entity;

import com.beathub.multiarenas.delivery.auth.entity.common.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "arena_usuario", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ArenaUsuario extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "arena_id", nullable = false)
    private Long arenaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "shop_nombre", length = 150)
    private String shopNombre;

    @OneToMany(mappedBy = "arenaUsuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RolArenaUsuario> roles = new ArrayList<>();
}
