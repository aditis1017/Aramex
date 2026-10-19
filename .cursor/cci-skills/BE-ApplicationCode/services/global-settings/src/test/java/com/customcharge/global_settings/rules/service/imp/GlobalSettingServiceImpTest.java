package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.dao.GlobalSettingDao;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.global_settings.rules.dto.UpdateGlobalSettingSequenceRequest;
import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import com.customcharge.global_settings.rulesmanager.DrlRuleConverter;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingDrlCacheManager;
import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.GlobalSettingNotFoundException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.search.SearchCriteria;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingServiceImpTest {

    @Mock
    private GlobalSettingDao globalSettingDao;
    @Mock
    private DrlRuleConverter drlRuleConverter;
    @Mock
    private GlobalSettingDrlCacheManager drlCacheManager;
    @Mock
    private PersistenceAuditQueryEngine auditQueryEngine;
    @Mock
    private GlobalSettingRepository globalSettingRepository;
    @Mock
    private GlobalSettingValidator globalSettingValidator;
    @Mock
    private GlobalSettingDtoMapper globalSettingDtoMapper;
    @Mock
    private GlobalSettingEntityFactory entityFactory;
    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;
    @Mock
    private GlobalSettingSyncService globalSettingSyncService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ObjectProvider<Javers> javers;
    @Mock
    private ObjectProvider<AuthorProvider> authorProvider;

    @InjectMocks
    private GlobalSettingServiceImp service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "notificationRoles", List.of("ADMIN"));
    }

    @Test
    void create_shouldPersistSetting_whenValidDto() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        entity.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        entity.getRules().getFirst().setValues(null);
        entity.getRules().getFirst().setExactValue("JO");

        when(entityFactory.newEntityFromDto(dto)).thenReturn(entity);
        mockCategoryLookups();
        doNothing().when(globalSettingValidator).validateDuplicateForCreate(entity);
        when(drlRuleConverter.convert(entity, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.countByTargetValueIn(List.of("DDU"))).thenReturn(0);
        when(globalSettingDao.save(entity)).thenReturn(entity);
        when(globalSettingDtoMapper.toDtoCodeOnly(entity)).thenReturn(dto);

        GlobalSettingDto result = service.create(dto);

        assertThat(result).isSameAs(dto);
        verify(drlCacheManager).syncOnCreate(entity);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> targetValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendCreateOrUpdateNotification(
                any(), actionCaptor.capture(), targetValueCaptor.capture(), any(), any(), any());
        assertThat(actionCaptor.getValue()).isEqualTo("CREATED");
        assertThat(targetValueCaptor.getValue()).isEqualTo("DDU");
    }

    @Test
    void create_shouldThrow_whenDtoNull() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findById_shouldThrow_whenIdNull() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findById_shouldThrowNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(globalSettingDao.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(GlobalSettingNotFoundException.class);
    }

    @Test
    void deleteById_shouldDeleteAndSyncCache() {
        UUID id = UUID.randomUUID();
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        when(globalSettingDao.findById(id)).thenReturn(Optional.of(existing));

        assertThat(service.deleteById(id)).isTrue();
        verify(globalSettingDao).deleteById(id);
        verify(drlCacheManager).syncOnDelete(existing);
    }

    @Test
    void getDataByCriteria_shouldThrowBadRequest_whenPaginationInvalid() {
        List<SearchCriteria> criteria = List.of();

        assertThatThrownBy(() -> service.getDataByCriteria(criteria, null, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateSequence_shouldThrow_whenRequestNull() {
        assertThatThrownBy(() -> service.updateSequence(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateSequence_shouldUpdateAndSync() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(setting.getId().toString()));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(1);
        when(globalSettingSyncService.syncGlobalSettings(null, false))
                .thenReturn(new MessageResponse("Sync Completed"));

        MessageResponse response = service.updateSequence(request);

        assertThat(response.getMessage()).contains("Sequence updated");
        verify(globalSettingDao).bulkUpdateSequenceNumbers(any(), eq(List.of("DDU")));
    }

    @Test
    void getAuditHistory_shouldMapAuditBlocks() throws Exception {
        AuditQueryRequest request = new AuditQueryRequest();
        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c1");
        block.setAuthor("tester");
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        ObjectNode change = objectMapper.createObjectNode();
        change.put("destination", "DXB");
        entry.setChangeParameter(change);
        ObjectNode current = objectMapper.createObjectNode();
        current.put("targetValue", "DDU");
        current.put("sequenceNumber", 1);
        entry.setCurrentState(current);
        entry.setType("INITIAL");
        block.setStateChangeData(List.of(entry));
        Page<AuditCommitBlock> page = new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1);
        when(auditQueryEngine.getAuditHistory(any())).thenReturn(page);
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DXB"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DXB", "Dubai", "Dubai Label"));

        Page<?> result = service.getAuditHistory(request);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void update_shouldReturnUpdatedDto_whenEntityExists() {
        UUID id = UUID.randomUUID();
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("AMM", "DDP");
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        existing.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        existing.getRules().getFirst().setValues(null);
        existing.getRules().getFirst().setExactValue("JO");

        when(globalSettingDao.findById(id)).thenReturn(Optional.of(existing));
        mockCategoryLookups();
        doNothing().when(globalSettingValidator).validateDuplicateForUpdate(id, existing);
        when(drlRuleConverter.convert(existing, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.save(existing)).thenReturn(existing);
        when(globalSettingDtoMapper.toDtoCodeOnly(existing)).thenReturn(dto);

        GlobalSettingDto result = service.update(id, dto).orElseThrow();

        assertThat(result).isSameAs(dto);
        verify(drlCacheManager).syncOnUpdate(id, "DXB", "DDU", existing);
    }

    @Test
    void findAll_shouldReturnMappedDtos() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingDao.findAll(any())).thenReturn(List.of(setting));
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        assertThat(service.findAll()).containsExactly(dto);
    }

    @Test
    void getDataByCriteria_shouldReturnPage_whenValidRequest() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setFieldName("destination");
        criteria.setValue("DXB");
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(setting), PageRequest.of(0, 10), 1));
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        Page<GlobalSettingDto> page = service.getDataByCriteria(List.of(criteria), 0, 10);

        assertThat(page.getContent()).containsExactly(dto);
    }

    @Test
    void countAuditExportRows_shouldDelegateToAuditEngine() {
        AuditQueryRequest request = new AuditQueryRequest();
        when(auditQueryEngine.countAuditExportRows(any())).thenReturn(5L);

        assertThat(service.countAuditExportRows(request)).isEqualTo(5L);
    }

    @Test
    void findById_shouldReturnDto_whenFound() {
        UUID id = UUID.randomUUID();
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingDao.findById(id)).thenReturn(Optional.of(setting));
        when(globalSettingDtoMapper.toDtoCodeOnly(setting)).thenReturn(dto);

        assertThat(service.findById(id)).contains(dto);
    }

    @Test
    void deleteById_shouldThrowNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(globalSettingDao.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(GlobalSettingNotFoundException.class);
    }

    @Test
    void deleteById_shouldThrow_whenIdNull() {
        assertThatThrownBy(() -> service.deleteById(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingDao.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, dto))
                .isInstanceOf(GlobalSettingNotFoundException.class);
    }

    @Test
    void update_shouldThrow_whenDtoNull() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(id, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateStatus_shouldUpdateAndNotify() {
        UUID id = UUID.randomUUID();
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        existing.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        existing.getRules().getFirst().setValues(null);
        existing.getRules().getFirst().setExactValue("JO");
        when(globalSettingDao.findById(id)).thenReturn(Optional.of(existing));
        when(globalSettingDao.save(existing)).thenReturn(existing);
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(existing));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(1);
        when(globalSettingSyncService.syncGlobalSettings(null, false))
                .thenReturn(new MessageResponse("Sync Completed"));

        MessageResponse response = service.updateStatus(id, false);

        assertThat(response.getMessage()).isEqualTo("Status Updated");
        verify(drlCacheManager).syncOnStatusChange(existing);
        ArgumentCaptor<String> previousStatusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newStatusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> targetValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendStatusChangeNotification(
                any(), previousStatusCaptor.capture(), newStatusCaptor.capture(),
                targetValueCaptor.capture(), any(), any());
        assertThat(previousStatusCaptor.getValue()).isEqualTo("ACTIVE");
        assertThat(newStatusCaptor.getValue()).isEqualTo("INACTIVE");
        assertThat(targetValueCaptor.getValue()).isEqualTo("DDU");
    }

    @Test
    void updateStatus_shouldThrowNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(globalSettingDao.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(id, true))
                .isInstanceOf(GlobalSettingNotFoundException.class);
    }

    @Test
    void create_shouldDefaultStatusToTrue_whenNull() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        entity.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        entity.getRules().getFirst().setValues(null);
        entity.getRules().getFirst().setExactValue("JO");
        entity.setStatus(null);

        when(entityFactory.newEntityFromDto(dto)).thenReturn(entity);
        mockCategoryLookups();
        doNothing().when(globalSettingValidator).validateDuplicateForCreate(entity);
        when(drlRuleConverter.convert(entity, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.countByTargetValueIn(List.of("DDU"))).thenReturn(0);
        when(globalSettingDao.save(entity)).thenReturn(entity);
        when(globalSettingDtoMapper.toDtoCodeOnly(entity)).thenReturn(dto);

        service.create(dto);

        assertThat(entity.getStatus()).isTrue();
    }

    @Test
    void create_shouldNotFail_whenNotificationThrows() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        entity.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        entity.getRules().getFirst().setValues(null);
        entity.getRules().getFirst().setExactValue("JO");

        when(entityFactory.newEntityFromDto(dto)).thenReturn(entity);
        mockCategoryLookups();
        doNothing().when(globalSettingValidator).validateDuplicateForCreate(entity);
        when(drlRuleConverter.convert(entity, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.countByTargetValueIn(List.of("DDU"))).thenReturn(0);
        when(globalSettingDao.save(entity)).thenReturn(entity);
        when(globalSettingDtoMapper.toDtoCodeOnly(entity)).thenReturn(dto);
        doThrow(new RuntimeException("notify failed")).when(notificationService)
                .sendCreateOrUpdateNotification(any(), any(), any(), any(), any(), any());

        assertThat(service.create(dto)).isSameAs(dto);
    }

    @Test
    void getDataByCriteria_shouldThrowBadRequest_whenIllegalArgument() {
        List<SearchCriteria> criteria = List.of();
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("bad field"));

        assertThatThrownBy(() -> service.getDataByCriteria(criteria, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bad field");
    }

    @Test
    void getDataByCriteria_shouldThrowInternalServer_whenUnexpectedError() {
        List<SearchCriteria> criteria = List.of();
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.getDataByCriteria(criteria, 0, 10))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void getDataByCriteria_shouldIgnoreNullCriteria() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(setting), PageRequest.of(0, 10), 1));
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        List<SearchCriteria> criteria = new ArrayList<>();
        criteria.add(null);
        Page<GlobalSettingDto> page = service.getDataByCriteria(criteria, 0, 10);

        assertThat(page.getContent()).containsExactly(dto);
    }

    @Test
    void updateSequence_shouldThrow_whenOrderedIdsEmpty() {
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of());

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateSequence_shouldThrow_whenIdBlank() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(" "));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("non-blank");
    }

    @Test
    void updateSequence_shouldThrow_whenInvalidUuid() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of("not-a-uuid"));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid UUID");
    }

    @Test
    void updateSequence_shouldThrow_whenSettingNotFound() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(UUID.randomUUID().toString()));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not part of the selected scope");
    }

    @Test
    void updateSequence_shouldThrowInternalServer_whenUnexpectedError() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(setting.getId().toString()));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(1);
        when(globalSettingSyncService.syncGlobalSettings(null, false)).thenThrow(new RuntimeException("sync down"));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void findAll_shouldFilterNullEntities() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        ArrayList<GlobalSetting> entities = new ArrayList<>();
        entities.add(null);
        entities.add(setting);
        when(globalSettingDao.findAll(any())).thenReturn(entities);
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        assertThat(service.findAll()).containsExactly(dto);
    }

    @Test
    void getAuditHistory_shouldResolveDestinationLabel_andKeepCodeWhenLookupFails() throws Exception {
        AuditQueryRequest request = new AuditQueryRequest();
        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c1");
        block.setAuthor("tester");
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        ObjectNode preState = objectMapper.createObjectNode();
        preState.put("destination", "DXB");
        entry.setPreState(preState);
        ObjectNode change = objectMapper.createObjectNode();
        change.put("Destination", "AMM");
        entry.setChangeParameter(change);
        ObjectNode current = objectMapper.createObjectNode();
        current.put("targetValue", "DDU");
        current.put("sequenceNumber", 1);
        entry.setCurrentState(current);
        entry.setType("UPDATE");
        block.setStateChangeData(List.of(entry));
        Page<AuditCommitBlock> page = new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1);
        when(auditQueryEngine.getAuditHistory(any())).thenReturn(page);
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DXB"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DXB", "Dubai", "Dubai Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("AMM"))
                .thenThrow(new RuntimeException("missing"));

        Page<?> result = service.getAuditHistory(request);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAll_withPagination_shouldDelegateToCriteriaSearch() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(setting), PageRequest.of(1, 5), 1));
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        List<GlobalSettingDto> result = service.findAll(1, 5);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void findAll_withNullSize_shouldLoadAll() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        when(globalSettingDao.findAll(any())).thenReturn(List.of(setting));
        when(globalSettingDtoMapper.toDtoWithResolvedValues(setting)).thenReturn(dto);

        assertThat(service.findAll(0, null)).containsExactly(dto);
    }

    @Test
    void getDataByCriteria_shouldThrowBadRequest_whenPageNegative() {
        assertThatThrownBy(() -> service.getDataByCriteria(List.of(), -1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("page");
    }

    @Test
    void getDataByCriteria_shouldThrowBadRequest_whenSizeLessThanOne() {
        assertThatThrownBy(() -> service.getDataByCriteria(List.of(), 0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size");
    }

    @Test
    void getDataByCriteria_shouldThrowBadRequest_whenIllegalArgumentHasNullMessage() {
        when(globalSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException());

        assertThatThrownBy(() -> service.getDataByCriteria(List.of(), 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid search criteria");
    }

    @Test
    void update_shouldDefaultStatusToTrue_whenNull() {
        UUID id = UUID.randomUUID();
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("AMM", "DDP");
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        existing.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        existing.getRules().getFirst().setValues(null);
        existing.getRules().getFirst().setExactValue("JO");
        existing.setStatus(null);

        when(globalSettingDao.findById(id)).thenReturn(Optional.of(existing));
        mockCategoryLookups();
        doNothing().when(globalSettingValidator).validateDuplicateForUpdate(id, existing);
        when(drlRuleConverter.convert(existing, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.save(existing)).thenReturn(existing);
        when(globalSettingDtoMapper.toDtoCodeOnly(existing)).thenReturn(dto);

        service.update(id, dto);

        assertThat(existing.getStatus()).isTrue();
    }

    @Test
    void update_shouldThrow_whenIdNull() {
        assertThatThrownBy(() -> service.update(null, GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateStatus_shouldThrow_whenIdNull() {
        assertThatThrownBy(() -> service.updateStatus(null, true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateStatus_shouldNotFail_whenNotificationThrows() {
        UUID id = UUID.randomUUID();
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        existing.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        existing.getRules().getFirst().setValues(null);
        existing.getRules().getFirst().setExactValue("JO");
        when(globalSettingDao.findById(id)).thenReturn(Optional.of(existing));
        when(globalSettingDao.save(existing)).thenReturn(existing);
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(existing));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(1);
        when(globalSettingSyncService.syncGlobalSettings(null, false))
                .thenReturn(new MessageResponse("Sync Completed"));
        doThrow(new RuntimeException("notify failed")).when(notificationService)
                .sendStatusChangeNotification(any(), any(), any(), any(), any(), any());

        MessageResponse response = service.updateStatus(id, false);

        assertThat(response.getMessage()).isEqualTo("Status Updated");
    }

    @Test
    void create_shouldNormalizeRuleCondition_whenContainsOperatorUsed() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        entity.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.CONTAINS);
        entity.getRules().getFirst().setValues(null);
        entity.getRules().getFirst().setExactValue("JO");

        when(entityFactory.newEntityFromDto(dto)).thenReturn(entity);
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin Country"));
        doNothing().when(globalSettingValidator).validateDuplicateForCreate(entity);
        when(drlRuleConverter.convert(entity, ShipmentDto.class)).thenReturn("drl");
        when(globalSettingDao.countByTargetValueIn(List.of("DDU"))).thenReturn(0);
        when(globalSettingDao.save(entity)).thenReturn(entity);
        when(globalSettingDtoMapper.toDtoCodeOnly(entity)).thenReturn(dto);

        service.create(dto);

        assertThat(entity.getRuleCondition()).contains("CONTAINS");
        assertThat(entity.getRuleCondition()).doesNotContain("\"");
    }

    @Test
    void updateSequence_shouldThrow_whenDuplicateId() {
        GlobalSetting first = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSetting second = GlobalSettingsTestDataFactory.globalSetting("AMM", "DDU", "f2", "v2");
        String duplicateId = first.getId().toString();
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(duplicateId, duplicateId));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate row id");
    }

    @Test
    void updateSequence_shouldThrow_whenOrderedIdsCountMismatch() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(setting.getId().toString()));
        GlobalSetting other = GlobalSettingsTestDataFactory.globalSetting("AMM", "DDU", "f2", "v2");
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting, other));

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must include all rules");
    }

    @Test
    void updateSequence_shouldThrow_whenBulkUpdateCountMismatch() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UpdateGlobalSettingSequenceRequest request =
                new UpdateGlobalSettingSequenceRequest("DDU", List.of(setting.getId().toString()));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(setting));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.updateSequence(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unable to update sequence");
    }

    @Test
    void updateSequence_shouldCommitAudit_whenOrderChanges() {
        GlobalSetting first = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSetting second = GlobalSettingsTestDataFactory.globalSetting("AMM", "DDU", "f2", "v2");
        UpdateGlobalSettingSequenceRequest request = new UpdateGlobalSettingSequenceRequest(
                "DDU",
                List.of(second.getId().toString(), first.getId().toString()));
        when(globalSettingDao.findAllByTargetValueIn(any(), any())).thenReturn(List.of(first, second));
        when(globalSettingDao.bulkUpdateSequenceNumbers(any(), any())).thenReturn(2);
        when(globalSettingSyncService.syncGlobalSettings(null, false))
                .thenReturn(new MessageResponse("Sync Completed"));
        Javers javersInstance = org.mockito.Mockito.mock(Javers.class);
        AuthorProvider authorProviderInstance = org.mockito.Mockito.mock(AuthorProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<Javers> javersProvider = org.mockito.Mockito.mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AuthorProvider> authorProviderObject = org.mockito.Mockito.mock(ObjectProvider.class);
        when(javersProvider.getIfAvailable()).thenReturn(javersInstance);
        when(authorProviderObject.getIfAvailable()).thenReturn(authorProviderInstance);
        when(authorProviderInstance.provide()).thenReturn("tester");
        ReflectionTestUtils.setField(service, "javers", javersProvider);
        ReflectionTestUtils.setField(service, "authorProvider", authorProviderObject);

        MessageResponse response = service.updateSequence(request);

        assertThat(response.getMessage()).contains("Sequence updated");
        verify(javersInstance).commit(eq("tester"), any(), any());
    }

    @Test
    void getAuditHistory_shouldReshapeSequenceAudit() throws Exception {
        AuditQueryRequest request = new AuditQueryRequest();
        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("seq-1");
        block.setManagedType("GlobalSettingSequenceAudit");
        block.setAuthor("tester");
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        ObjectNode current = objectMapper.createObjectNode();
        current.put("previousSequence", "1,2");
        current.put("sequence", "2,1");
        entry.setCurrentState(current);
        entry.setType("UPDATE");
        block.setStateChangeData(List.of(entry));
        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1));

        Page<?> result = service.getAuditHistory(request);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAuditHistory_shouldStripMetadataAndKeepBlankDestination() throws Exception {
        AuditQueryRequest request = new AuditQueryRequest();
        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c2");
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        ObjectNode current = objectMapper.createObjectNode();
        current.put("id", "should-strip");
        current.put("destination", " ");
        entry.setCurrentState(current);
        block.setStateChangeData(List.of(entry));
        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1));

        Page<?> result = service.getAuditHistory(request);

        assertThat(result.getContent()).hasSize(1);
    }

    private void mockCategoryLookups() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin Country"));
    }
}
