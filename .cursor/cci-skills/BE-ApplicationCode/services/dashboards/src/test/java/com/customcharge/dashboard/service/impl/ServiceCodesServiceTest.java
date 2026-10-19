package com.customcharge.dashboard.service.impl;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCodesServiceTest {

    @Mock
    private ServiceCodesRepository serviceCodesRepository;

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private ServiceCodesService serviceCodesService;

    @Test
    void requireChargeType_shouldNormalizeAndValidate() {
        assertThat(ServiceCodesService.requireChargeType(" dtc ")).isEqualTo("DTC");
    }

    @Test
    void requireChargeType_shouldRejectBlank() {
        assertThatThrownBy(() -> ServiceCodesService.requireChargeType(" "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireEntityCode_shouldTrimValue() {
        assertThat(ServiceCodesService.requireEntityCode(" DXB ")).isEqualTo("DXB");
    }

    @Test
    void requireExistingMasterInChargeTypes_shouldRejectEmptyChargeTypes() {
        assertThatThrownBy(() -> serviceCodesService.requireExistingMasterInChargeTypes("DXB", List.of(), "SC01"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireExistingMasterInChargeTypes_shouldThrowWhenNotFound() {
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCodesService.requireExistingMasterInChargeTypes(
                "DXB", List.of("DTC"), "SC01"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByEntityCodeAndChargeType_shouldDelegateToRepository() {
        ServiceCodes master = new ServiceCodes();
        when(serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse("DXB", "DTC"))
                .thenReturn(List.of(master));

        assertThat(serviceCodesService.findByEntityCodeAndChargeType("DXB", "DTC")).containsExactly(master);
    }

    @Test
    void requireExistingMaster_shouldReturnMatch() {
        ServiceCodes master = new ServiceCodes();
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.of(master));

        assertThat(serviceCodesService.requireExistingMaster("DXB", "DTC", "SC01")).isSameAs(master);
    }

    @Test
    void ensureMaster_shouldReturnExistingRow() {
        ServiceCodes existing = new ServiceCodes();
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.of(existing));

        assertThat(serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "Desc", "VAT")).isSameAs(existing);
    }

    @Test
    void requireServiceCode_shouldRejectBlank() {
        assertThatThrownBy(() -> serviceCodesService.requireExistingMaster("DXB", "DTC", " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireChargeType_shouldRejectTooLongValue() {
        assertThatThrownBy(() -> ServiceCodesService.requireChargeType("A".repeat(51)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireExistingMasterInChargeTypes_shouldReturnFirstMatch() {
        ServiceCodes master = new ServiceCodes();
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTA")).thenReturn(Optional.of(master));

        assertThat(serviceCodesService.requireExistingMasterInChargeTypes(
                "DXB", List.of("DTC", "DTA"), "SC01")).isSameAs(master);
    }

    @Test
    void requireExistingMaster_shouldThrowWhenMissing() {
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCodesService.requireExistingMaster("DXB", "DTC", "SC01"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireEntityCode_shouldRejectBlank() {
        assertThatThrownBy(() -> ServiceCodesService.requireEntityCode(" "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ensureMaster_shouldCreateNewMaster() {
        Entity entity = new Entity();
        entity.setActive(true);
        ServiceCodes saved = new ServiceCodes();

        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(serviceCodesRepository.save(any(ServiceCodes.class))).thenReturn(saved);

        assertThat(serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "Description", "VAT")).isSameAs(saved);

        ArgumentCaptor<ServiceCodes> captor = ArgumentCaptor.forClass(ServiceCodes.class);
        verify(serviceCodesRepository).save(captor.capture());
        assertThat(captor.getValue().getServiceCode()).isEqualTo("SC01");
        assertThat(captor.getValue().getChargeType()).isEqualTo("DTC");
    }

    @Test
    void ensureMaster_shouldRejectInvalidTaxCode() {
        assertThatThrownBy(() -> serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "Description", "A".repeat(51)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ensureMaster_shouldThrowConflictOnDataIntegrityViolation() {
        Entity entity = new Entity();
        entity.setActive(true);

        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(serviceCodesRepository.save(any(ServiceCodes.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "Description", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ensureMaster_shouldRejectDescriptionThatIsTooLong() {
        assertThatThrownBy(() -> serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "A".repeat(256), "VAT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ensureMaster_shouldRejectInactiveEntity() {
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "DXB", "SC01", "DTC")).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCodesService.ensureMaster(
                "DXB", "DTC", "SC01", "Description", "VAT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireServiceCode_shouldRejectTooLongValue() {
        assertThatThrownBy(() -> serviceCodesService.ensureMaster(
                "DXB", "DTC", "A".repeat(51), "Description", "VAT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ensureSampleMastersForEntityAndChargeType_shouldSeedDutyForRevenueChargeTypes() {
        Entity entity = new Entity();
        entity.setActive(true);

        when(serviceCodesRepository.existsByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));
        when(serviceCodesRepository.save(any(ServiceCodes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = serviceCodesService.ensureSampleMastersForEntityAndChargeType("RUH", "DTA");

        assertThat(created).isEqualTo(2);

        ArgumentCaptor<ServiceCodes> captor = ArgumentCaptor.forClass(ServiceCodes.class);
        verify(serviceCodesRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ServiceCodes::getServiceCode)
                .containsExactly("DUTY", "REVENUE");
        assertThat(captor.getAllValues())
                .extracting(ServiceCodes::getChargeType)
                .containsOnly("DTA");
    }

    @Test
    void ensureSampleMastersForEntityAndChargeType_shouldSeedDutyForCostChargeTypes() {
        Entity entity = new Entity();
        entity.setActive(true);

        when(serviceCodesRepository.existsByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));
        when(serviceCodesRepository.save(any(ServiceCodes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = serviceCodesService.ensureSampleMastersForEntityAndChargeType("RUH", "ECD");

        assertThat(created).isEqualTo(3);
        ArgumentCaptor<ServiceCodes> captor = ArgumentCaptor.forClass(ServiceCodes.class);
        verify(serviceCodesRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ServiceCodes::getServiceCode)
                .containsExactly("DUTY", "VAT", "OF");
    }

    @Test
    void updateEntitySettingsActiveFlag_shouldUpdateCostChargeTypeAndSave() {
        Account account = new Account();
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("ECD");
        Entity entity = new Entity();
        entity.setCode("RUH");
        account.setEntity(entity);

        ServiceCodes master = new ServiceCodes();
        master.setIsActive(false);

        serviceCodesService.updateEntitySettingsActiveFlag(account, master, null, true);

        assertThat(master.getIsActive()).isTrue();
        verify(serviceCodesRepository).save(master);
        verify(accountService).syncAccountToRedis(isNull(), eq(AccountNature.ENTITY_SETTINGS), eq("RUH"), eq("ECD"));
    }

    @Test
    void updateEntitySettingsActiveFlag_shouldNoOpForArAccount() {
        Account account = new Account();
        account.setAccountNature(AccountNature.AR);

        ServiceCodes master = new ServiceCodes();

        serviceCodesService.updateEntitySettingsActiveFlag(account, master, "DTC", true);

        verify(serviceCodesRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateEntitySettingsActiveFlag_shouldNoOpWhenActiveNull() {
        Account account = new Account();
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);

        serviceCodesService.updateEntitySettingsActiveFlag(account, new ServiceCodes(), "ECD", null);

        verify(serviceCodesRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateEntitySettingsActiveFlag_shouldNoOpForUnsupportedChargeType() {
        Account account = new Account();
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("UNSUPPORTED");

        ServiceCodes master = new ServiceCodes();

        serviceCodesService.updateEntitySettingsActiveFlag(account, master, null, true);

        verify(serviceCodesRepository, org.mockito.Mockito.never()).save(any());
    }
}
