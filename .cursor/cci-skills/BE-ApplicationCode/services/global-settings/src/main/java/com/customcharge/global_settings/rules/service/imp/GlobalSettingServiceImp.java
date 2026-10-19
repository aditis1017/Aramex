package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.audit.GlobalSettingSequenceAudit;
import com.customcharge.global_settings.rules.constent.ValidationConstant;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.*;
import com.customcharge.persistencelayer.search.SearchCriteria;
import com.customcharge.persistencelayer.search.SpecBuilderUtil;
import com.customcharge.global_settings.rules.dao.GlobalSettingDao;
import com.customcharge.global_settings.rules.dto.*;
import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.global_settings.rulesmanager.DrlRuleConverter;
import com.customcharge.global_settings.rulesmanager.DrlRuleResolutionHelper;
import com.customcharge.global_settings.rulesmanager.RuleConditionBuilder;
import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GlobalSettingServiceImp implements GlobalSettingService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSettingServiceImp.class);

    private static final Sort DEFAULT_GLOBAL_SETTING_LIST_SORT = Sort.by(
            Sort.Order.desc("status"),
            Sort.Order.asc("sequenceNumber"));

    private static final Set<String> AUDIT_METADATA_FIELDS_TO_STRIP = Set.of(
            "id",
            "createdBy",
            "updatedBy",
            "createdTime",
            "updatedTime");
    private static final Set<String> AUDIT_LABEL_KEYS = Set.of(
            "destination",
            "Destination");


    @Value("#{'${mailNotificationRoles}'.split(',')}")
    private List<String> notificationRoles;

    private final GlobalSettingDao globalSettingDao;
    private final DrlRuleConverter drlRuleConverter;
    private final GlobalSettingDrlCacheManager drlCacheManager;
    private final PersistenceAuditQueryEngine auditQueryEngine;
    private final GlobalSettingRepository globalSettingRepository;
    private final GlobalSettingValidator globalSettingValidator;
    private final GlobalSettingDtoMapper globalSettingDtoMapper;
    private final GlobalSettingEntityFactory entityFactory;
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;
    private final GlobalSettingSyncService globalSettingSyncService;
    private final NotificationService notificationService;
    private final ObjectProvider<Javers> javers;
    private final ObjectProvider<AuthorProvider> authorProvider;

    public GlobalSettingServiceImp(
            GlobalSettingDao globalSettingDao,
            @Qualifier("templateDrlRuleConverter") DrlRuleConverter drlRuleConverter,
            GlobalSettingDrlCacheManager drlCacheManager,
            PersistenceAuditQueryEngine auditQueryEngine,
            GlobalSettingRepository globalSettingRepository,
            GlobalSettingValidator globalSettingValidator,
            GlobalSettingDtoMapper globalSettingDtoMapper,
            GlobalSettingEntityFactory entityFactory,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            GlobalSettingSyncService globalSettingSyncService,
            NotificationService notificationService,
            ObjectProvider<Javers> javers,
            ObjectProvider<AuthorProvider> authorProvider
            ) {
        this.globalSettingDao = globalSettingDao;
        this.drlRuleConverter = drlRuleConverter;
        this.drlCacheManager = drlCacheManager;
        this.auditQueryEngine = auditQueryEngine;
        this.globalSettingRepository = globalSettingRepository;
        this.globalSettingValidator = globalSettingValidator;
        this.globalSettingDtoMapper = globalSettingDtoMapper;
        this.entityFactory = entityFactory;
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
        this.notificationService = notificationService;
        this.globalSettingSyncService = globalSettingSyncService;
        this.javers = javers;
        this.authorProvider = authorProvider;
    }

    @Override
    @Transactional(readOnly = false)
    @CacheEvict(cacheNames = GlobalSettingCacheNames.FIND_ALL, allEntries = true)
    public GlobalSettingDto create(GlobalSettingDto dto) {
        log.info("Entering GlobalSettingService.create");
        try {
            requireNonNullDto(dto);
            globalSettingValidator.validateRuleCriteria(dto);
            GlobalSetting globalSetting = entityFactory.newEntityFromDto(dto);
            buildAndSetRuleCondition(globalSetting);
            globalSettingValidator.validateDuplicateForCreate(globalSetting);

            String droolsRules = drlRuleConverter.convert(globalSetting, ShipmentDto.class);
            globalSetting.setDrlRule(droolsRules);

            int count = globalSettingDao.countByTargetValueIn(
                    GlobalSettingValidator.resolveTargetValuesForScope(dto.getTargetValue())) + 1;
            globalSetting.setSequenceNumber(count);
            if (globalSetting.getStatus() == null) {
                globalSetting.setStatus(Boolean.TRUE);
            }
            GlobalSetting saved = globalSettingDao.save(globalSetting);
            drlCacheManager.syncOnCreate(saved);
            triggerCreateOrUpdateNotification(saved, "CREATED");
            return globalSettingDtoMapper.toDtoCodeOnly(saved);
        } catch (AbstractApiException ex) {
            log.error("Failed in GlobalSettingService.create", ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.create");
        }
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = GlobalSettingCacheNames.BY_ID, key = "#id"),
            @CacheEvict(cacheNames = GlobalSettingCacheNames.FIND_ALL, allEntries = true)
    })
    public Optional<GlobalSettingDto> update(UUID id, GlobalSettingDto dto) {
        log.info("Entering GlobalSettingService.update id={}", id);
        try {
            requireNonNullId(id);
            requireNonNullDto(dto);
            globalSettingValidator.validateRuleCriteria(dto);
            return Optional.of(globalSettingDao.findById(id).map(existing -> {
                String previousDestinationCode = existing.getDestination();
                String previousTargetValue = existing.getTargetValue();
                entityFactory.applyDtoToEntity(dto, existing);
                buildAndSetRuleCondition(existing);
                globalSettingValidator.validateDuplicateForUpdate(id, existing);

                String droolsRules = drlRuleConverter.convert(existing, ShipmentDto.class);
                existing.setDrlRule(droolsRules);
                if (existing.getStatus() == null) {
                    existing.setStatus(Boolean.TRUE);
                }

                GlobalSetting saved = globalSettingDao.save(existing);
                drlCacheManager.syncOnUpdate(id, previousDestinationCode, previousTargetValue, saved);
                triggerCreateOrUpdateNotification(saved, "UPDATED");
                return globalSettingDtoMapper.toDtoCodeOnly(saved);
            }).orElseThrow(() -> new GlobalSettingNotFoundException(id)));
        } catch (AbstractApiException ex) {
            log.error("Failed in GlobalSettingService.update id={}", id, ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.update id={}", id);
        }
    }

    @Override
    @Cacheable(cacheNames = GlobalSettingCacheNames.BY_ID, key = "#id")
    public Optional<GlobalSettingDto> findById(UUID id) {
        log.info("Entering GlobalSettingService.findById id={}", id);
        try {
            requireNonNullId(id);
            return Optional.of(globalSettingDao.findById(id).map(globalSettingDtoMapper::toDtoCodeOnly)
                    .orElseThrow(() -> new GlobalSettingNotFoundException(id)));
        } catch (RuntimeException ex) {
            log.error("Failed in GlobalSettingService.findById id={}", id, ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.findById id={}", id);
        }
    }

    @Override
    @Cacheable(cacheNames = GlobalSettingCacheNames.FIND_ALL,  key = "'ALL'")
    public List<GlobalSettingDto> findAll() {
        return loadAll();
    }

    @Override
    @Cacheable(cacheNames = GlobalSettingCacheNames.FIND_ALL,
            key = "#size == null ? 'ALL' : #page + '_' + #size")
    public List<GlobalSettingDto> findAll(Integer page, Integer size) {
        if (size == null) {
            return loadAll();
        }
        return getDataByCriteria(List.of(), page == null ? 0 : page, size).getContent();
    }

    private List<GlobalSettingDto> loadAll() {
        log.info("Entering GlobalSettingService.findAll");
        try {
            return globalSettingDao.findAll(DEFAULT_GLOBAL_SETTING_LIST_SORT).stream()
                    .filter(Objects::nonNull)
                    .map(globalSettingDtoMapper::toDtoWithResolvedValues)
                    .toList();
        } catch (RuntimeException ex) {
            log.error("Failed in GlobalSettingService.findAll", ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.findAll");
        }
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = GlobalSettingCacheNames.BY_ID, key = "#id"),
            @CacheEvict(cacheNames = GlobalSettingCacheNames.FIND_ALL, allEntries = true)
    })
    public boolean deleteById(UUID id) {
        log.info("Entering GlobalSettingService.deleteById id={}", id);
        try {
            requireNonNullId(id);
            Optional<GlobalSetting> existingOpt = globalSettingDao.findById(id);
            if (existingOpt.isEmpty()) {
                throw new GlobalSettingNotFoundException(id);
            }
            GlobalSetting existing = existingOpt.get();
            globalSettingDao.deleteById(id);
            drlCacheManager.syncOnDelete(existing);
            return true;
        } catch (RuntimeException ex) {
            log.error("Failed in GlobalSettingService.deleteById id={}", id, ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.deleteById id={}", id);
        }
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = GlobalSettingCacheNames.BY_ID, allEntries = true),
            @CacheEvict(cacheNames = GlobalSettingCacheNames.FIND_ALL, allEntries = true)
    })
    public MessageResponse updateStatus(UUID id, boolean status) {
        log.info("Entering GlobalSettingService.updateStatus id={} status={}", id, status);
        try {
            requireNonNullId(id);
            GlobalSetting existing = globalSettingDao.findById(id)
                    .orElseThrow(() -> new GlobalSettingNotFoundException(id));
            String oldStatus = existing.getStatus() ? "ACTIVE" : "INACTIVE";
            existing.setStatus(status);
            GlobalSetting saved = globalSettingDao.save(existing);
            drlCacheManager.syncOnStatusChange(saved);
            triggerStatusNotification(saved, oldStatus, status ? "ACTIVE" : "INACTIVE");
            List<String> idList = globalSettingDao.findAllByTargetValueIn(
                    GlobalSettingValidator.resolveTargetValuesForScope(existing.getTargetValue()),
                    DEFAULT_GLOBAL_SETTING_LIST_SORT)
                    .stream().map(GlobalSetting::getId).map(UUID::toString).toList();
            updateSequence(new UpdateGlobalSettingSequenceRequest(existing.getTargetValue(),idList));
            return new MessageResponse("Status Updated");
        } catch (RuntimeException ex) {
            log.error("Failed in GlobalSettingService.updateStatus id={} status={}", id, status, ex);
            throw ex;
        } finally {
            log.info("Exiting GlobalSettingService.updateStatus id={}", id);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<GlobalSettingDto> getDataByCriteria(List<SearchCriteria> criteriaList, Integer page, Integer size) {
        validatePagination(page, size);
        try {
            List<SearchCriteria> normalizedCriteria = normalizeCriteriaForEntityFields(criteriaList);
            var specBuilder = new SpecBuilderUtil().addCriteria(normalizedCriteria).from(globalSettingRepository);
            Specification<GlobalSetting> spec =
                    (Specification<GlobalSetting>) (Object) specBuilder.getSpecification();
            Pageable pageable = PageRequest.of(page, size, DEFAULT_GLOBAL_SETTING_LIST_SORT);
            Page<GlobalSetting> entityPage = globalSettingRepository.findAll(spec, pageable);
            return entityPage.map(globalSettingDtoMapper::toDtoWithResolvedValues);
        } catch (AbstractApiException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid global setting search request: {}", ex.getMessage());
            throw new BadRequestException(
                    ex.getMessage() != null ? ex.getMessage() : "Invalid search criteria or pagination parameters.");
        } catch (RuntimeException ex) {
            log.error("Failed to search global settings by criteria", ex);
            throw new InternalServerException(
                    "Unable to load global settings for the given filters. Please adjust your criteria and try again.");
        }
    }

    private static void requireNonNullId(UUID id) {
        if (id == null) {
            throw new ValidationException(ValidationConstant.ID_MUST_NOT_NULL);
        }
    }

    private static void requireNonNullDto(GlobalSettingDto dto) {
        if (dto == null) {
            throw new ValidationException("Request body must contain a global setting payload.");
        }
    }

    private static void validatePagination(Integer page, Integer size) {
        if (page == null || size == null) {
            throw new BadRequestException("Query parameters 'page' and 'size' are required.");
        }
        if (page < 0) {
            throw new BadRequestException("Query parameter 'page' must be zero or greater.");
        }
        if (size < 1) {
            throw new BadRequestException("Query parameter 'size' must be at least 1.");
        }
    }

    private void buildAndSetRuleCondition(GlobalSetting setting) {
        List<GlobalRuleCriteria> ruleCriteria = DrlRuleResolutionHelper
                .buildCriteriaForDrl(setting, categoryCodesGlobalDataCacheService, true)
                .stream()
                .sorted(Comparator
                        .comparing(GlobalRuleCriteria::getFieldName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(r -> r.getOperator() != null ? r.getOperator().name() : "")
                        .thenComparing(GlobalRuleCriteria::getValues, Comparator.nullsLast(String::compareTo))
                        .thenComparing(GlobalRuleCriteria::getExactValue, Comparator.nullsLast(String::compareTo))
                        .thenComparing(GlobalRuleCriteria::getCurrency, Comparator.nullsLast(String::compareTo)))
                .toList();
        setting.setRuleCondition(normalizeRuleCondition(RuleConditionBuilder.build(ruleCriteria)));
    }

    private static String normalizeRuleCondition(String raw) {
        if (raw == null) {
            return null;
        }
        String result = raw;
        result = result.replace("&&", "AND")
                .replace("==", "=")
                .replace("\"", "");
        result = result.replaceAll("(\\w+)\\.contains\\(([^)]+)\\)", "$1 CONTAINS $2");
        Pattern pattern = Pattern.compile("\\b[a-z]+[A-Z]\\w*\\b");
        Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String word = matcher.group();
            String converted = Character.toUpperCase(word.charAt(0)) + word.substring(1);
            matcher.appendReplacement(sb, converted);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static List<SearchCriteria> normalizeCriteriaForEntityFields(List<SearchCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return List.of();
        }
        List<SearchCriteria> normalized = new ArrayList<>(criteriaList.size());
        for (SearchCriteria criteria : criteriaList) {
            if (criteria == null) {
                continue;
            }
            SearchCriteria copy = new SearchCriteria();
            copy.setCondition(criteria.getCondition());
            copy.setValue(criteria.getValue());
            copy.setSecondValue(criteria.getSecondValue());
            copy.setFieldName(criteria.getFieldName());
            normalized.add(copy);
        }
        return normalized;
    }

    private void triggerCreateOrUpdateNotification(GlobalSetting setting, String action) {
        try {
            notificationService.sendCreateOrUpdateNotification(
                    notificationRoles,
                    action,
                    setting.getTargetValue(),
                    setting.getId() != null ? setting.getId().toString() : null,
                    setting.getUpdatedBy(),
                    setting.getTargetField()
            );
        } catch (RuntimeException ex) {
            log.error("Failed to trigger create/update notification for id={}", setting.getId(), ex);
        }
    }

    private void triggerStatusNotification(GlobalSetting setting, String oldStatus, String newStatus) {
        try {
            notificationService.sendStatusChangeNotification(
                    notificationRoles,
                    oldStatus,
                    newStatus,
                    setting.getTargetValue(),
                    setting.getId() != null ? setting.getId().toString() : null,
                    setting.getUpdatedBy()
            );
        } catch (RuntimeException ex) {
            log.error("Failed to trigger status notification for id={}", setting.getId(), ex);
        }
    }

    @Override
    public Page<GlobalSettingAuditHistoryCommitDto> getAuditHistory(AuditQueryRequest request) {
        request.setManagedType(Optional.empty());
        request.setManagedTypes(List.of(
                GlobalSetting.class.getName(),
                GlobalSettingSequenceAudit.class.getName()));
        Page<AuditCommitBlock> page = auditQueryEngine.getAuditHistory(request);
        List<GlobalSettingAuditHistoryCommitDto> content =
                page.getContent().stream().map(this::toAuditHistoryCommitDto).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private GlobalSettingAuditHistoryCommitDto toAuditHistoryCommitDto(AuditCommitBlock block) {
        GlobalSettingAuditHistoryCommitDto dto = new GlobalSettingAuditHistoryCommitDto();
        dto.setCommitId(block.getCommitId());
        dto.setManagedType(block.getManagedType());
        dto.setAuthor(block.getAuthor());
        dto.setCommitDate(block.getCommitDate());
        List<AuditStateChangeEntry> entries = block.getStateChangeData();
        if (entries != null && !entries.isEmpty()) {
            GlobalSettingAuditStateChangeDto stateChange = toAuditStateChangeDto(entries.getFirst());
            if ("GlobalSettingSequenceAudit".equals(block.getManagedType())) {
                reshapeSequenceAudit(stateChange);
            }
            dto.setStateChangeData(stateChange);
        }
        return dto;
    }

    private void reshapeSequenceAudit(GlobalSettingAuditStateChangeDto dto) {
        if (dto.getCurrentState() == null || !dto.getCurrentState().isObject()) {
            return;
        }
        ObjectNode current = (ObjectNode) dto.getCurrentState();
        JsonNode previous = current.remove("previousSequence");
        if (previous != null && previous.isTextual() && !previous.asText().isBlank()) {
            ObjectNode preState = current.deepCopy();
            preState.put("sequence", previous.asText());
            dto.setPreState(preState);
        }
        dto.setType("UPDATE");
        ObjectNode changeParameter = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        if (current.has("sequence")) {
            changeParameter.set("sequence", current.get("sequence"));
        }
        dto.setChangeParameter(changeParameter);
    }

    private GlobalSettingAuditStateChangeDto toAuditStateChangeDto(AuditStateChangeEntry entry) {
        GlobalSettingAuditStateChangeDto dto = new GlobalSettingAuditStateChangeDto();
        dto.setPreState(stripAuditMetadata(entry.getPreState()));
        dto.setType(entry.getType());
        dto.setCurrentState(stripAuditMetadata(entry.getCurrentState()));
        dto.setChangeParameter(stripAuditMetadata(entry.getChangeParameter()));
        return dto;
    }

    private JsonNode stripAuditMetadata(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return node;
        }
        ObjectNode copy = (ObjectNode) node.deepCopy();
        copy.remove(AUDIT_METADATA_FIELDS_TO_STRIP);
        resolveAuditLabels(copy);
        return copy;
    }

    private void resolveAuditLabels(ObjectNode node) {
        for (String key : AUDIT_LABEL_KEYS) {
            JsonNode valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull() || !valueNode.isTextual()) {
                continue;
            }
            String code = valueNode.asText();
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                String label = categoryCodesGlobalDataCacheService.getCategoryCodeByCode(code.trim()).getLabel();
                if (label != null && !label.isBlank()) {
                    node.put(key, label);
                }
            } catch (RuntimeException ignored) {
                // Keep original code when no mapping is available.
            }
        }
    }

    @Override
    public long countAuditExportRows(AuditQueryRequest request) {
        request.setManagedType(Optional.empty());
        request.setManagedTypes(List.of(
                GlobalSetting.class.getName(),
                GlobalSettingSequenceAudit.class.getName()));
        return auditQueryEngine.countAuditExportRows(request);
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = GlobalSettingCacheNames.BY_ID, allEntries = true),
            @CacheEvict(cacheNames = GlobalSettingCacheNames.FIND_ALL, allEntries = true)
    })
    public MessageResponse updateSequence(UpdateGlobalSettingSequenceRequest request) {
        log.info("Entering GlobalSettingService.updateSequence");
        try {
            if (request == null) {
                throw new BadRequestException("Request body is required.");
            }
            List<String> targetValues = GlobalSettingValidator.resolveTargetValuesForScope(request.getTargetValue());
            List<String> idStrings = request.getOrderedRowIds();
            if (idStrings == null || idStrings.isEmpty()) {
                throw new BadRequestException("Ordered Row Ids must not be null or empty.");
            }

            List<GlobalSetting> scopedSettings = globalSettingDao.findAllByTargetValueIn(
                    targetValues, DEFAULT_GLOBAL_SETTING_LIST_SORT);
            Map<UUID, GlobalSetting> globalSettingMap = scopedSettings.stream()
                    .collect(Collectors.toMap(GlobalSetting::getId, setting -> setting));

            if (idStrings.size() != scopedSettings.size()) {
                throw new BadRequestException("Ordered row ids must include all rules in the selected scope.");
            }

            Set<UUID> seen = new HashSet<>();
            Map<UUID, Integer> idToSequence = new LinkedHashMap<>();
            List<UUID> orderedIds = new ArrayList<>(idStrings.size());
            for (int i = 0; i < idStrings.size(); i++) {
                String raw = idStrings.get(i);
                if (raw == null || raw.isBlank()) {
                    throw new BadRequestException("Each ordered row id must be non-blank.");
                }
                UUID id;
                try {
                    id = UUID.fromString(raw.trim());
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid UUID: " + raw);
                }
                if (!seen.add(id)) {
                    throw new BadRequestException("Duplicate row id found in ordered list: " + id);
                }
                if (!globalSettingMap.containsKey(id)) {
                    throw new BadRequestException("Ordered row id is not part of the selected scope: " + id);
                }
                orderedIds.add(id);
                idToSequence.put(id, i + 1);
            }

            int updatedRows = globalSettingDao.bulkUpdateSequenceNumbers(idToSequence, targetValues);
            if (updatedRows != orderedIds.size()) {
                throw new BadRequestException(
                        "Unable to update sequence for all selected rules. Please refresh and try again.");
            }

            List<UUID> currentOrder = scopedSettings.stream().map(GlobalSetting::getId).toList();
            if (!orderedIds.equals(currentOrder)) {
                Map<UUID, Integer> oldIndexById = new HashMap<>();
                for (int i = 0; i < currentOrder.size(); i++) {
                    oldIndexById.put(currentOrder.get(i), i);
                }
                List<String> previousParts = new ArrayList<>();
                List<String> currentParts = new ArrayList<>();
                for (int i = 0; i < currentOrder.size(); i++) {
                    if (Objects.equals(currentOrder.get(i), orderedIds.get(i))) {
                        continue;
                    }
                    previousParts.add(String.valueOf(i + 1));
                    currentParts.add(String.valueOf(oldIndexById.get(orderedIds.get(i)) + 1));
                }
                if (!previousParts.isEmpty()) {
                    commitSequenceAudit(
                            request.getTargetValue(),
                            targetValues,
                            String.join(",", previousParts),
                            String.join(",", currentParts));
                }
            }

            globalSettingSyncService.syncGlobalSettings(null, false);
            return new MessageResponse("Sequence updated successfully");
        } catch (AbstractApiException ex) {
            log.error("Failed in GlobalSettingService.updateSequence", ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Failed in GlobalSettingService.updateSequence", ex);
            throw new InternalServerException("Unable to update global setting sequence. Please try again.");
        } finally {
            log.info("Exiting GlobalSettingService.updateSequence");
        }
    }

    private void commitSequenceAudit(
            String rawTargetValue,
            List<String> targetValues,
            String previousSequence,
            String sequence) {
        Javers javersInstance = javers.getIfAvailable();
        AuthorProvider author = authorProvider.getIfAvailable();
        if (javersInstance == null || author == null) {
            return;
        }
        GlobalSettingSequenceAudit snapshot = new GlobalSettingSequenceAudit();
        snapshot.setScopeKey(targetValues.size() > 1 ? "CATEGORY" : targetValues.getFirst());
        snapshot.setTargetValue(rawTargetValue == null ? null : rawTargetValue.trim());
        snapshot.setPreviousSequence(previousSequence);
        snapshot.setSequence(sequence);
        javersInstance.commit(
                author.provide(),
                snapshot,
                Map.of(AuditCommitProperties.GROUP_NAME, "GLOBAL_SETTING"));
    }

}
