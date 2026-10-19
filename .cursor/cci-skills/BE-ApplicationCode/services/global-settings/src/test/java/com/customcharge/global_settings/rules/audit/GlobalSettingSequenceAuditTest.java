package com.customcharge.global_settings.rules.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalSettingSequenceAuditTest {

    @Test
    void shouldSupportGettersAndSetters() {
        GlobalSettingSequenceAudit audit = new GlobalSettingSequenceAudit();
        audit.setScopeKey("DDU");
        audit.setTargetValue("DDU");
        audit.setPreviousSequence("1,2");
        audit.setSequence("2,1");

        assertThat(audit.getScopeKey()).isEqualTo("DDU");
        assertThat(audit.getTargetValue()).isEqualTo("DDU");
        assertThat(audit.getPreviousSequence()).isEqualTo("1,2");
        assertThat(audit.getSequence()).isEqualTo("2,1");
    }
}
