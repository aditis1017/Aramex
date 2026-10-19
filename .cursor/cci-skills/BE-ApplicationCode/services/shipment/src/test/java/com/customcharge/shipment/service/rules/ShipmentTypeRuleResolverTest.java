package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.MasterCategoryClosureLookupService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.rules.RuleEngine;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.CUSTOMS_TYPE;
import static com.customcharge.shipment.constants.MasterDataCategoryCodes.SHIPMENT_TYPE;
import static com.customcharge.shipment.constants.ShipmentConstants.ACCOUNT_SHIPPER_RUH_OVERRIDE;
import static com.customcharge.shipment.constants.ShipmentConstants.ENTITY_RUH;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDO;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDP;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentTypeRuleResolverTest {

    @Mock
    private MasterCategoryClosureLookupService masterCategoryClosureLookupService;
    @Mock
    private GlobalSettingRuleLookupService globalSettingRuleLookupService;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateService;

    @InjectMocks
    private ShipmentTypeRuleResolver resolver;

    @BeforeEach
    void stubExchangeRateProvider() {
        lenient().when(exchangeRateService.getIfAvailable()).thenReturn(null);
    }

    @Test
    void resolveShipmentType_shouldReturnEmpty_whenMessageNull() {
        assertThat(resolver.resolveShipmentType(null)).isEmpty();
    }

    @Test
    void resolveShipmentType_shouldReturnExistingValue_whenAlreadySetAndRulesDoNotMatch() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setShipmentType(SHIPMENT_TYPE_DDP);
        UUID dduRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddu")));
        when(ruleEngine.countRulesFired(any(ShipmentDto.class), eq("rule-ddu"))).thenReturn(0);

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDP);
        assertThat(result.get().assignment()).isNull();
        assertThat(message.getShipmentType()).isEqualTo(SHIPMENT_TYPE_DDP);
        verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));
    }

    @Test
    void resolveShipmentType_shouldApplyRuleMatch_whenShipmentTypeAlreadySet() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setShipmentType(SHIPMENT_TYPE_DDP);
        UUID dduRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddu")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType(SHIPMENT_TYPE_DDU);
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDU);
        assertThat(result.get().assignment()).isNotNull();
        assertThat(message.getShipmentType()).isEqualTo(SHIPMENT_TYPE_DDU);
    }

    @Test
    void resolveShipmentType_shouldApplyRuleMatch_whenExistingValueSameAsRuleResult() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setShipmentType(SHIPMENT_TYPE_DDO);
        UUID ddoRuleId = UUID.randomUUID();
        stubCandidateTypes("DDO");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDO")).thenReturn(Map.of(ddoRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddo")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType(SHIPMENT_TYPE_DDO);
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddo"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDO);
        assertThat(result.get().assignment()).isNotNull();
        assertThat(result.get().assignment().ruleId()).isEqualTo(ddoRuleId);
        assertThat(message.getShipmentType()).isEqualTo(SHIPMENT_TYPE_DDO);
    }

    @Test
    void resolveShipmentType_shouldReturnExistingValue_whenDestinationMissing() {
        ShipmentDto message = new ShipmentDto();
        message.setShipmentType(SHIPMENT_TYPE_DDP);

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDP);
        verify(ruleEngine, never()).countRulesFired(any(), any());
    }

    @Test
    void resolveShipmentType_shouldPreservePreSetDdo_whenRulesDoNotMatch() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setShipmentType(SHIPMENT_TYPE_DDO);
        UUID dduRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU", "DDO");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddu")));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDO")).thenReturn(Map.of(UUID.randomUUID().toString(), Map.of("priority", 1, "rule", "rule-ddo")));
        when(ruleEngine.countRulesFired(any(ShipmentDto.class), eq("rule-ddo"))).thenReturn(0);
        when(ruleEngine.countRulesFired(any(ShipmentDto.class), eq("rule-ddu"))).thenReturn(0);

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDO);
        assertThat(result.get().assignment()).isNull();
        assertThat(message.getShipmentType()).isEqualTo(SHIPMENT_TYPE_DDO);
        verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddo"));
        verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));
    }

    @Test
    void resolveShipmentType_shouldApplyRuhShipperOverride() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode(ENTITY_RUH);
        message.setShipperAccountNumber(ACCOUNT_SHIPPER_RUH_OVERRIDE);
        stubCandidateTypes("DDU", "DDP");

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDU);
        verify(ruleEngine, never()).countRulesFired(any(), any());
    }

    @Test
    void resolveShipmentType_shouldResolveFromRules() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        UUID ruleId = UUID.randomUUID();
        stubCandidateTypes("DDU");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "rule-ddu")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType("DDU");
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo("DDU");
        assertThat(message.getShipmentType()).isEqualTo("DDU");
    }

    @Test
    void resolveShipmentType_shouldMergeDduAndDdoRulesByPriority() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        UUID ddoRuleId = UUID.randomUUID();
        UUID dduRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU", "DDO", "DDP");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDO")).thenReturn(Map.of(ddoRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddo")));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 2, "rule", "rule-ddu")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType(SHIPMENT_TYPE_DDO);
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddo"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDO);
        verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddo"));
        verify(ruleEngine, never()).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));
        verify(globalSettingRuleLookupService, never()).loadDrlMap("AE", "DDP");
    }

    @Test
    void resolveShipmentType_shouldFallbackToDdp_whenPrimaryRulesDoNotMatch() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        UUID dduRuleId = UUID.randomUUID();
        UUID ddoRuleId = UUID.randomUUID();
        UUID ddpRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU", "DDO", "DDP");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 2, "rule", "rule-ddu")));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDO")).thenReturn(Map.of(ddoRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddo")));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDP")).thenReturn(Map.of(ddpRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddp")));
        when(ruleEngine.countRulesFired(any(ShipmentDto.class), eq("rule-ddo"))).thenReturn(0);
        when(ruleEngine.countRulesFired(any(ShipmentDto.class), eq("rule-ddu"))).thenReturn(0);
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType(SHIPMENT_TYPE_DDP);
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddp"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDP);
        var inOrder = inOrder(ruleEngine);
        inOrder.verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddo"));
        inOrder.verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));
        inOrder.verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddp"));
    }

    @Test
    void resolveShipmentType_shouldNotFallbackToDdp_whenPrimaryRuleMatches() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        UUID dduRuleId = UUID.randomUUID();
        UUID ddpRuleId = UUID.randomUUID();
        stubCandidateTypes("DDU", "DDP");
        when(globalSettingRuleLookupService.loadDrlMap("AE", "DDU")).thenReturn(Map.of(dduRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddu")));
        lenient().when(globalSettingRuleLookupService.loadDrlMap("AE", "DDP")).thenReturn(Map.of(ddpRuleId.toString(), Map.of("priority", 1, "rule", "rule-ddp")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setShipmentType(SHIPMENT_TYPE_DDU);
            return 1;
        }).when(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveShipmentType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(SHIPMENT_TYPE_DDU);
        verify(ruleEngine).countRulesFired(any(ShipmentDto.class), eq("rule-ddu"));
        verify(ruleEngine, never()).countRulesFired(any(ShipmentDto.class), eq("rule-ddp"));
    }

    private void stubCandidateTypes(String... codes) {
        when(masterCategoryClosureLookupService.loadCandidateCodes(CUSTOMS_TYPE, SHIPMENT_TYPE))
                .thenReturn(List.of(codes));
    }
}
