package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.enums.MatchType;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "keyword_rules",
        indexes = {
                @Index(name = "idx_keyword_rules_keyword", columnList = "keyword"),
        }
)
public class KeywordRule extends AbstractEntity {

    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    @Column(name = "is_active")
    private boolean isActive;
}
