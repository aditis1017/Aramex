package com.customcharge.persistencelayer.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    /**
     * Primary human-readable label (synced to {@link #displayName} for legacy queries/exports).
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    /** Localized display name for Arabic UI (optional). */
    @Column(name = "arabic_name", length = 255)
    private String arabicName;

    @Column(name = "arabic_description", length = 2000)
    private String arabicDescription;

    @Column(name = "color", length = 64)
    private String color;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> permissions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    void syncNameAndDisplayName() {
        if (name != null && !name.isBlank()) {
            name = name.trim();
            displayName = name;
        } else if (displayName != null && !displayName.isBlank()) {
            displayName = displayName.trim();
            name = displayName;
        }
    }
}
