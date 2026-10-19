package com.customcharge.global_settings.rules.audit;

import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AuditableEntity(groupName = "GLOBAL_SETTING")
public class GlobalSettingSequenceAudit {

    @Id
    private String scopeKey;

    private String targetValue;

    private String previousSequence;
    private String sequence;
}
