package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.rules.RuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.SHIPMENT_FILTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentFullnessRuleResolverTest {

    @Mock
    private GlobalSettingRuleLookupService globalSettingRuleLookupService;
    @Mock
    private RuleEngine ruleEngine;

    @InjectMocks
    private ShipmentFullnessRuleResolver resolver;

    @Test
    void applyShipmentFilterRules_shouldNoOp_whenMessageNull() {
        resolver.applyShipmentFilterRules(null);
        verify(globalSettingRuleLookupService, never()).loadDrlMap(any(), any());
    }

    @Test
    void applyShipmentFilterRules_shouldNoOp_whenAlreadyFull() {
        ShipmentDto message = new ShipmentDto();
        message.setIsShipmentFull(true);

        resolver.applyShipmentFilterRules(message);

        verify(globalSettingRuleLookupService, never()).loadDrlMap(any(), any());
    }

    @Test
    void applyShipmentFilterRules_shouldNoOp_whenDestinationMissing() {
        ShipmentDto message = new ShipmentDto();
        message.setIsShipmentFull(false);

        resolver.applyShipmentFilterRules(message);

        verify(globalSettingRuleLookupService, never()).loadDrlMap(any(), any());
    }

    @Test
    void applyShipmentFilterRules_shouldExecuteRules() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setIsShipmentFull(false);
        UUID ruleId = UUID.randomUUID();

        when(globalSettingRuleLookupService.loadDrlMap("AE", SHIPMENT_FILTER))
                .thenReturn(Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "fullness-rule")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setIsShipmentFull(true);
            return null;
        }).when(ruleEngine).execute(any(ShipmentDto.class), eq("fullness-rule"));

        resolver.applyShipmentFilterRules(message);

        verify(ruleEngine).execute(any(ShipmentDto.class), eq("fullness-rule"));
    }
}
