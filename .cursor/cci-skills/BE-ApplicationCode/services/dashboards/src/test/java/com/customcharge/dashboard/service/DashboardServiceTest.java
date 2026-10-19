package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.charge.ShipmentChargeDetailsResponse;
import com.customcharge.dashboard.mapper.ShipmentChargeDetailsMapper;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private APAccountRepository apAccountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private ShipmentChargeDetailsMapper shipmentChargeDetailsMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getShipmentChargeDetails_shouldRejectMissingShipment() {
        when(shipmentRepository.findByHawbNumber("AWB-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getShipmentChargeDetails("AWB-1", "DTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shipment not found");
    }

    @Test
    void getShipmentChargeDetails_shouldRejectMissingChargeType() {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHawbNumber("AWB-1");

        Charge charge = new Charge();
        charge.setChargeCode("DTA");

        when(shipmentRepository.findByHawbNumber("AWB-1")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of(charge));

        assertThatThrownBy(() -> dashboardService.getShipmentChargeDetails("AWB-1", "DTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Charge type DTC not found");
    }

    @Test
    void getShipmentChargeDetails_shouldRejectWhenNoChargesExist() {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());

        when(shipmentRepository.findByHawbNumber("AWB-1")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> dashboardService.getShipmentChargeDetails("AWB-1", "DTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No charges found");
    }

    @Test
    void getShipmentChargeDetails_shouldReturnMappedResponse() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        shipment.setHawbNumber("AWB-1");

        Entity entity = new Entity();
        entity.setCode("DXB");

        Charge charge = new Charge();
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setChargeCode("DTC");
        charge.setEntity(entity);
        charge.setShipment(shipment);

        ServiceCodes master = new ServiceCodes();
        master.setChargeType("DTC");
        master.setServiceCode("SC01");

        Account account = new Account();
        account.setEntity(entity);
        account.setChargeType("DTC");

        ShipmentChargeDetailsResponse expected = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-1")
                .requestedChargeType("DTC")
                .charges(List.of())
                .build();

        when(shipmentRepository.findByHawbNumber("AWB-1")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(charge));
        when(chargeLineItemRepository.findByCharge_IdIn(List.of(chargeId))).thenReturn(List.of());
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(any(), any()))
                .thenReturn(List.of(account));
        when(shipmentChargeDetailsMapper.toResponse(
                eq(shipment),
                eq("DTC"),
                eq(Set.of("DTC")),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenReturn(expected);

        ShipmentChargeDetailsResponse result = dashboardService.getShipmentChargeDetails("AWB-1", " dtc ");

        assertThat(result).isSameAs(expected);
        verify(shipmentChargeDetailsMapper).toResponse(
                eq(shipment),
                eq("DTC"),
                eq(Set.of("DTC")),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void getShipmentChargeDetails_shouldLoadApAccountsForCostCharges() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        shipment.setHawbNumber("AWB-2");

        Entity entity = new Entity();
        entity.setCode("DXB");

        Charge charge = new Charge();
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setChargeCode("ECD");
        charge.setEntity(entity);
        charge.setShipment(shipment);

        Account entitySettings = new Account();
        entitySettings.setEntity(entity);
        entitySettings.setChargeType("ECD");
        entitySettings.setDefaultAccountNumber("AP-001");

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("AP-001");
        apAccount.setEntityCode("DXB");

        ShipmentChargeDetailsResponse expected = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-2")
                .requestedChargeType("ECD")
                .charges(List.of())
                .build();

        when(shipmentRepository.findByHawbNumber("AWB-2")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(charge));
        when(chargeLineItemRepository.findByCharge_IdIn(List.of(chargeId))).thenReturn(List.of());
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of());
        when(accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(any(), any()))
                .thenReturn(List.of(entitySettings));
        when(apAccountRepository.findByAccountNumberAndEntityCode("AP-001", "DXB"))
                .thenReturn(Optional.of(apAccount));
        when(shipmentChargeDetailsMapper.toResponse(
                eq(shipment),
                eq("ECD"),
                eq(Set.of("ECD")),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenReturn(expected);

        ShipmentChargeDetailsResponse result = dashboardService.getShipmentChargeDetails("AWB-2", "ECD");

        assertThat(result).isSameAs(expected);
        verify(apAccountRepository).findByAccountNumberAndEntityCode("AP-001", "DXB");
    }

    @Test
    void getShipmentChargeDetails_shouldLoadApAccountsFromCostLineApAccountNumber() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        shipment.setHawbNumber("AWB-ECS");

        Entity entity = new Entity();
        entity.setCode("RUH");

        Charge charge = new Charge();
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setChargeCode("ECS");
        charge.setEntity(entity);
        charge.setShipment(shipment);

        ChargeLineItem lineItem = new ChargeLineItem();
        lineItem.setCharge(charge);
        lineItem.setApAccountNumber("ECS-AP-001");

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("ECS-AP-001");
        apAccount.setAccountName("ECS AP Vendor");
        apAccount.setEntityCode("RUH");

        ShipmentChargeDetailsResponse expected = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-ECS")
                .requestedChargeType("ECS")
                .charges(List.of())
                .build();

        when(shipmentRepository.findByHawbNumber("AWB-ECS")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(charge));
        when(chargeLineItemRepository.findByCharge_IdIn(List.of(chargeId))).thenReturn(List.of(lineItem));
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of());
        when(accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(any(), any()))
                .thenReturn(List.of());
        when(apAccountRepository.findByAccountNumberAndEntityCode("ECS-AP-001", "RUH"))
                .thenReturn(Optional.of(apAccount));
        when(shipmentChargeDetailsMapper.toResponse(
                eq(shipment),
                eq("ECS"),
                eq(Set.of("ECS")),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenReturn(expected);

        ShipmentChargeDetailsResponse result = dashboardService.getShipmentChargeDetails("AWB-ECS", "ECS");

        assertThat(result).isSameAs(expected);
        verify(apAccountRepository).findByAccountNumberAndEntityCode("ECS-AP-001", "RUH");
    }

    @Test
    void getShipmentChargeDetails_shouldLoadDtcArAccountWhenChargeAccountInformationIsComplete() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        shipment.setHawbNumber("AWB-3");
        shipment.setConsigneeNumber("AR-001");
        shipment.setConsigneeName("Consignee Account");

        Entity entity = new Entity();
        entity.setCode("RUH");

        Charge charge = new Charge();
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setChargeCode("DTC");
        charge.setEntity(entity);
        charge.setShipment(shipment);

        ChargeLineItem lineItem = new ChargeLineItem();
        lineItem.setCharge(charge);

        ARAccount arAccount = new ARAccount();
        arAccount.setAccountNumber("AR-001");
        arAccount.setAccountName("AR Master Account");
        arAccount.setEntityCode("RUH");

        ShipmentChargeDetailsResponse expected = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-3")
                .requestedChargeType("DTC")
                .charges(List.of())
                .build();

        when(shipmentRepository.findByHawbNumber("AWB-3")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(charge));
        when(chargeLineItemRepository.findByCharge_IdIn(List.of(chargeId))).thenReturn(List.of(lineItem));
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of());
        when(accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(any(), any()))
                .thenReturn(List.of());
        when(arAccountRepository.findByAccountNumberAndEntityCode("AR-001", "RUH"))
                .thenReturn(Optional.of(arAccount));
        when(shipmentChargeDetailsMapper.toResponse(
                eq(shipment),
                eq("DTC"),
                eq(Set.of("DTC")),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(Map.of(ShipmentChargeDetailsMapper.arAccountKey("RUH", "AR-001"), arAccount)),
                any()))
                .thenReturn(expected);

        ShipmentChargeDetailsResponse result = dashboardService.getShipmentChargeDetails("AWB-3", "DTC");

        assertThat(result).isSameAs(expected);
        verify(arAccountRepository).findByAccountNumberAndEntityCode("AR-001", "RUH");
    }

    @Test
    void getShipmentChargeDetails_shouldLoadSellerArAccountForSellerInformation() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        shipment.setHawbNumber("AWB-SELLER");
        shipment.setShipperNumber("9003");
        shipment.setOriginEntity("RUH");
        shipment.setShipperName("Fallback Shipper");

        Entity entity = new Entity();
        entity.setCode("RUH");

        Charge charge = new Charge();
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setChargeCode("ECD");
        charge.setEntity(entity);
        charge.setShipment(shipment);

        ARAccount sellerArAccount = new ARAccount();
        sellerArAccount.setAccountNumber("9003");
        sellerArAccount.setEntityCode("RUH");
        sellerArAccount.setAccountName("AR Seller Name");

        ShipmentChargeDetailsResponse expected = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-SELLER")
                .requestedChargeType("ECD")
                .charges(List.of())
                .build();

        when(shipmentRepository.findByHawbNumber("AWB-SELLER")).thenReturn(Optional.of(shipment));
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(charge));
        when(chargeLineItemRepository.findByCharge_IdIn(List.of(chargeId))).thenReturn(List.of());
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of());
        when(accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(any(), any()))
                .thenReturn(List.of());
        when(arAccountRepository.findByAccountNumberAndEntityCode("9003", "RUH"))
                .thenReturn(Optional.of(sellerArAccount));
        when(shipmentChargeDetailsMapper.toResponse(
                eq(shipment),
                eq("ECD"),
                eq(Set.of("ECD")),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(sellerArAccount))).thenReturn(expected);

        ShipmentChargeDetailsResponse result = dashboardService.getShipmentChargeDetails("AWB-SELLER", "ECD");

        assertThat(result).isSameAs(expected);
        verify(arAccountRepository).findByAccountNumberAndEntityCode("9003", "RUH");
    }
}
