package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.RuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.MAWB_FILTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentMawbRuleResolverTest {

    @Mock
    private CacheService cacheService;
    @Mock
    private RuleEngine ruleEngine;

    @InjectMocks
    private ShipmentMawbRuleResolver resolver;

    @Test
    void applyMawbFilterRules_shouldSetFlagWhenRuleRuns() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("SA");
        UUID ruleId = UUID.randomUUID();

        when(cacheService.get("rule:SA:" + MAWB_FILTER, Map.class))
                .thenReturn(Optional.of(Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "mawb-rule"))));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setIsMawbShipment(true);
            return null;
        }).when(ruleEngine).execute(any(ShipmentDto.class), eq("mawb-rule"));

        resolver.applyMawbFilterRules(message);

        verify(ruleEngine).execute(any(ShipmentDto.class), eq("mawb-rule"));
    }
}
