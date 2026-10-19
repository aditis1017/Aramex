package com.customcharge.global_settings.rules.constent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationConstantTest {

    @Test
    void constants_shouldHaveExpectedValues() {
        assertThat(ValidationConstant.ID_MUST_NOT_NULL).contains("required");
        assertThat(ValidationConstant.DUPLICATE_GLOBAL_SETTING_ACTIVE).isEqualTo("Rule already exists.");
        assertThat(ValidationConstant.DUPLICATE_GLOBAL_SETTING_INACTIVE).isEqualTo("Rule already exists.");
        assertThat(ValidationConstant.IN_OPERATOR_REQUIRES_MULTIPLE_VALUES)
                .contains("Equals")
                .contains("two values");
    }
}
