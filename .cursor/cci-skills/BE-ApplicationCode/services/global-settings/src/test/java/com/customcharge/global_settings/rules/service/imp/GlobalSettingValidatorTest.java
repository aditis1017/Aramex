package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.constent.ValidationConstant;
import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.enums.Operator;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingValidatorTest {

    @Mock
    private GlobalSettingRepository repository;

    @InjectMocks
    private GlobalSettingValidator validator;

    @Test
    void resolveTargetValuesForScope_shouldReturnAllCategoryCodes_whenCategoryScope() {
        assertThat(GlobalSettingValidator.resolveTargetValuesForScope("Category"))
                .containsExactly("HV", "LV", "B2B", "Exempted");
    }

    @Test
    void resolveTargetValuesForScope_shouldReturnUppercasedValue_whenSpecificTarget() {
        assertThat(GlobalSettingValidator.resolveTargetValuesForScope("ddu"))
                .containsExactly("DDU");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "  "})
    void resolveTargetValuesForScope_shouldThrow_whenBlank(String blank) {
        assertThatThrownBy(() -> GlobalSettingValidator.resolveTargetValuesForScope(blank))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Target Value is required");
    }

    @Test
    void validateRuleCriteria_shouldNotThrow_whenInOperatorHasMultipleValues() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.IN);
        rule.setValues("JO,AE,SA");
        dto.setRules(List.of(rule));

        assertThatCode(() -> validator.validateRuleCriteria(dto)).doesNotThrowAnyException();
    }

    @Test
    void validateRuleCriteria_shouldThrow_whenInOperatorHasSingleValue() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.IN);
        rule.setValues("JO");
        dto.setRules(List.of(rule));

        assertThatThrownBy(() -> validator.validateRuleCriteria(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ValidationConstant.IN_OPERATOR_REQUIRES_MULTIPLE_VALUES);
    }

    @Test
    void validateRuleCriteria_shouldThrow_whenInOperatorHasNoValues() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.IN);
        rule.setValues(" ");
        dto.setRules(List.of(rule));

        assertThatThrownBy(() -> validator.validateRuleCriteria(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ValidationConstant.IN_OPERATOR_REQUIRES_MULTIPLE_VALUES);
    }

    @Test
    void validateRuleCriteria_shouldNotThrow_whenNotInOperatorHasMultipleValues() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.NOT_IN);
        rule.setValues("RDG,RDL,RMY");
        dto.setRules(List.of(rule));

        assertThatCode(() -> validator.validateRuleCriteria(dto)).doesNotThrowAnyException();
    }

    @Test
    void validateRuleCriteria_shouldThrow_whenNotInOperatorHasSingleValue() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.NOT_IN);
        rule.setValues("RDG");
        dto.setRules(List.of(rule));

        assertThatThrownBy(() -> validator.validateRuleCriteria(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ValidationConstant.NOT_IN_OPERATOR_REQUIRES_MULTIPLE_VALUES);
    }

    @Test
    void validateRuleCriteria_shouldNotThrow_whenEqualsOperatorHasSingleValue() {
        GlobalSettingDto dto = new GlobalSettingDto();
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setOperator(Operator.EQUALS);
        rule.setValues("JO");
        dto.setRules(List.of(rule));

        assertThatCode(() -> validator.validateRuleCriteria(dto)).doesNotThrowAnyException();
    }

    @Test
    void validateDuplicateForCreate_shouldThrowConflict_whenDuplicateExists() {
        GlobalSetting entity = new GlobalSetting();
        entity.setDestination("DXB");
        entity.setTargetValue("DDU");
        entity.setRuleCondition("originCountry = JO");
        when(repository.findByDestinationAndRuleConditionAndTargetValueIn("DXB", "originCountry = JO", List.of("DDU")))
                .thenReturn(List.of(activeSetting()));

        assertThatThrownBy(() -> validator.validateDuplicateForCreate(entity))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void resolveTargetValuesForScope_shouldAcceptCategoryTypo() {
        assertThat(GlobalSettingValidator.resolveTargetValuesForScope("Catogoery"))
                .containsExactly("HV", "LV", "B2B", "Exempted");
    }

    @Test
    void resolveTargetValuesForScope_shouldAcceptDirectCategoryCode() {
        assertThat(GlobalSettingValidator.resolveTargetValuesForScope("HV"))
                .containsExactly("HV", "LV", "B2B", "Exempted");
    }

    @Test
    void validateDuplicateForCreate_shouldUseInactiveMessage_whenOnlyInactiveConflicts() {
        GlobalSetting entity = new GlobalSetting();
        entity.setDestination("DXB");
        entity.setTargetValue("DDU");
        entity.setRuleCondition("originCountry = JO");
        GlobalSetting inactive = activeSetting();
        inactive.setStatus(Boolean.FALSE);
        when(repository.findByDestinationAndRuleConditionAndTargetValueIn("DXB", "originCountry = JO", List.of("DDU")))
                .thenReturn(List.of(inactive));

        assertThatThrownBy(() -> validator.validateDuplicateForCreate(entity))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateDuplicateForUpdate_shouldThrowConflict_whenDuplicateExists() {
        UUID id = UUID.randomUUID();
        GlobalSetting entity = new GlobalSetting();
        entity.setDestination("AMM");
        entity.setTargetValue("DDP");
        entity.setRuleCondition("cond");
        when(repository.findByDestinationAndRuleConditionAndTargetValueInAndIdNot(
                "AMM", "cond", List.of("DDP"), id)).thenReturn(List.of(activeSetting()));

        assertThatThrownBy(() -> validator.validateDuplicateForUpdate(id, entity))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateDuplicateForUpdate_shouldNotThrow_whenNoConflict() {
        UUID id = UUID.randomUUID();
        GlobalSetting entity = new GlobalSetting();
        entity.setDestination("AMM");
        entity.setTargetValue("DDP");
        entity.setRuleCondition("cond");
        when(repository.findByDestinationAndRuleConditionAndTargetValueInAndIdNot(
                "AMM", "cond", List.of("DDP"), id)).thenReturn(List.of());

        assertThatCode(() -> validator.validateDuplicateForUpdate(id, entity)).doesNotThrowAnyException();
    }

    private static GlobalSetting activeSetting() {
        GlobalSetting setting = new GlobalSetting();
        setting.setStatus(Boolean.TRUE);
        return setting;
    }
}
