package com.customcharge.persistencelayer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Maps Entra security group object ids (GUID) to intranet entity codes for persisting {@code users.groups}.
 */
@Entity
@Table(name = "entra_guid_entity_map")
@Getter
@Setter
public class EntraGuidEntityMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "guid", nullable = false, unique = true, length = 36)
    private String guid;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "display_name", length = 512)
    private String displayName;
}
