package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.constants.DashboardMessageConstants;
import com.customcharge.dashboard.constants.ServiceChargeMappingMessageConstants;
import com.customcharge.dashboard.dto.CreateServiceChargeMappingRequest;
import com.customcharge.dashboard.dto.EntityServiceChargeGroupDTO;
import com.customcharge.dashboard.dto.ExistingMappingDTO;
import com.customcharge.dashboard.dto.FromServiceCodeDTO;
import com.customcharge.dashboard.dto.MappingRequestDTO;
import com.customcharge.dashboard.dto.ServiceChargeMappingPairDTO;
import com.customcharge.dashboard.dto.UpdateServiceChargeMappingRequest;
import com.customcharge.dashboard.dto.ServiceChargeMappingItemDTO;
import com.customcharge.dashboard.dto.ServiceChargeMappingScreenDTO;
import com.customcharge.dashboard.dto.ServiceChargeWithMappingsDTO;
import com.customcharge.dashboard.dto.ToServiceCodeDTO;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.ServiceChargeMapping;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.customcharge.dashboard.cache.DashboardCacheNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceChargeMappingService {

    private static final List<String> FROM_SOURCE_CHARGE_TYPES =
            ChargeTypeConstants.resolveMasterChargeTypes(ChargeTypeConstants.DTA);

    private final ServiceChargeMappingRepository serviceChargeMappingRepository;
    private final ServiceCodesRepository serviceCodesRepository;
    private final ServiceCodesService serviceCodesService;

    @Cacheable(
            cacheNames = DashboardCacheNames.SERVICE_CHARGE_MAPPING_SCREEN,
            key = "{#chargeType, #fromEntityCode, #toEntityCode, #editFlag ?: false}")
    public ServiceChargeMappingScreenDTO getMappingScreenData(
            String chargeType, String fromEntityCode, String toEntityCode, Boolean editFlag) {
        String resolvedChargeType = requireDestinationChargeType(chargeType);
        String resolvedFromEntity = ServiceCodesService.requireEntityCode(fromEntityCode);
        String resolvedToEntity = ServiceCodesService.requireEntityCode(toEntityCode);

        if (serviceChargeMappingRepository.existsByFromEntityAndToEntityAndChargeType(
                resolvedFromEntity, resolvedToEntity, resolvedChargeType)
                && !Boolean.TRUE.equals(editFlag)) {
            throw new ConflictException(ServiceChargeMappingMessageConstants.MAPPING_COMBINATION_EXISTS);
        }

        List<FromServiceCodeDTO> fromServiceCodes = findFromEntityServiceCodes(resolvedFromEntity)
                .stream()
                .map(this::toFromServiceCodeDto)
                .toList();

        List<ToServiceCodeDTO> toServiceCodes = serviceCodesService
                .findByEntityCodeAndChargeType(resolvedToEntity, resolvedChargeType)
                .stream()
                .map(this::toToServiceCodeDto)
                .toList();

        List<ExistingMappingDTO> mappings = serviceChargeMappingRepository
                .findByFromEntityAndToEntityAndChargeType(
                        resolvedFromEntity, resolvedToEntity, resolvedChargeType)
                .stream()
                .map(this::toExistingMappingDto)
                .toList();

        return new ServiceChargeMappingScreenDTO(fromServiceCodes, toServiceCodes, mappings);
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGE_MAPPING_SCREEN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGES_GROUPED, allEntries = true)
    })
    public MessageResponse createMappings(CreateServiceChargeMappingRequest request) {
        MappingContext context = resolveCreateContext(request);
        requireAllFromServiceCodesMapped(
                context.fromEntity(),
                request.getMappings().stream()
                        .map(ServiceChargeMappingPairDTO::getFromUuid)
                        .collect(Collectors.toSet()));
        List<ServiceChargeMapping> toPersist = new ArrayList<>();

        for (ServiceChargeMappingPairDTO pair : request.getMappings()) {
            if (serviceChargeMappingRepository.existsByFromServiceCodeAndToServiceCodeAndFromEntityAndToEntityAndChargeType(
                    pair.getFromUuid(),
                    pair.getToUuid(),
                    context.fromEntity(),
                    context.toEntity(),
                    context.chargeType())) {
                throw new ConflictException(ServiceChargeMappingMessageConstants.MAPPING_ROW_EXISTS);
            }
            toPersist.add(buildMapping(pair.getFromUuid(), pair.getToUuid(), context));
        }

        if (!toPersist.isEmpty()) {
            serviceChargeMappingRepository.saveAll(toPersist);
        }
        return new MessageResponse(DashboardMessageConstants.SAVED_SUCCESSFULLY);
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGE_MAPPING_SCREEN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGES_GROUPED, allEntries = true)
    })
    public MessageResponse updateMappings(UpdateServiceChargeMappingRequest request) {
        MappingContext context = resolveUpdateContext(request);
        List<ServiceChargeMapping> toPersist = new ArrayList<>();

        for (MappingRequestDTO row : request.getMappings()) {
            if (row.getMappingId() != null) {
                ServiceChargeMapping existing = serviceChargeMappingRepository.findById(row.getMappingId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ServiceChargeMappingMessageConstants.MAPPING_NOT_FOUND));
                assertMappingBelongsToContext(existing, context);
                if (isMappingUnchanged(existing, row.getFromUuid(), row.getToUuid())) {
                    continue;
                }
                assertPairNotUsedByAnotherMapping(
                        row.getFromUuid(),
                        row.getToUuid(),
                        context,
                        row.getMappingId());
                applyServiceCodes(existing, row.getFromUuid(), row.getToUuid(), context);
                toPersist.add(existing);
                continue;
            }

            if (serviceChargeMappingRepository.existsByFromServiceCodeAndToServiceCodeAndFromEntityAndToEntityAndChargeType(
                    row.getFromUuid(),
                    row.getToUuid(),
                    context.fromEntity(),
                    context.toEntity(),
                    context.chargeType())) {
                throw new ConflictException(ServiceChargeMappingMessageConstants.MAPPING_ROW_EXISTS);
            }
            toPersist.add(buildMapping(row.getFromUuid(), row.getToUuid(), context));
        }

        if (!toPersist.isEmpty()) {
            serviceChargeMappingRepository.saveAll(toPersist);
        }

        requireAllFromServiceCodesMappedInDatabase(context);

        if (toPersist.isEmpty()) {
            return new MessageResponse(ServiceChargeMappingMessageConstants.NO_CHANGES);
        }
        return new MessageResponse(DashboardMessageConstants.SAVED_SUCCESSFULLY);
    }

    @Cacheable(
            cacheNames = DashboardCacheNames.SERVICE_CHARGES_GROUPED,
            key = "{#entityCode, #chargeType}")
    public Map<String, EntityServiceChargeGroupDTO> getGroupedServiceCharges(
            String entityCode, String chargeType) {
        String resolvedChargeType = requireDestinationChargeType(chargeType);
        String resolvedToEntity = ServiceCodesService.requireEntityCode(entityCode);

        List<ServiceChargeMapping> mappings = serviceChargeMappingRepository.findByToEntityAndChargeType(
                resolvedToEntity, resolvedChargeType);
        if (mappings.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<UUID, List<ServiceChargeMapping>>> mappingsByFromEntityAndFromId = mappings.stream()
                .collect(Collectors.groupingBy(
                        ServiceChargeMapping::getFromEntity,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                mapping -> mapping.getFromServiceCode().getId(),
                                LinkedHashMap::new,
                                Collectors.toList())));

        Map<String, EntityServiceChargeGroupDTO> response = new LinkedHashMap<>();
        for (Map.Entry<String, Map<UUID, List<ServiceChargeMapping>>> fromEntityEntry
                : mappingsByFromEntityAndFromId.entrySet()) {
            String fromEntity = fromEntityEntry.getKey();
            List<ServiceCodes> fromMasters = findFromEntityServiceCodes(fromEntity);
            if (fromMasters.isEmpty()) {
                continue;
            }

            Map<UUID, List<ServiceChargeMapping>> mappingsByFromId = fromEntityEntry.getValue();
            List<ServiceChargeWithMappingsDTO> serviceCharges = fromMasters.stream()
                    .map(master -> toGroupedServiceChargeDto(
                            master,
                            mappingsByFromId.getOrDefault(master.getId(), List.of())))
                    .toList();
            response.put(fromEntity, new EntityServiceChargeGroupDTO(serviceCharges));
        }
        return response;
    }

    private MappingContext resolveCreateContext(CreateServiceChargeMappingRequest request) {
        String resolvedChargeType = requireDestinationChargeType(request.getChargeType());
        String resolvedFromEntity = ServiceCodesService.requireEntityCode(request.getFromEntityCode());
        String resolvedToEntity = ServiceCodesService.requireEntityCode(request.getToEntityCode());

        Set<UUID> fromIds = request.getMappings().stream()
                .map(ServiceChargeMappingPairDTO::getFromUuid)
                .collect(Collectors.toSet());
        Set<UUID> toIds = request.getMappings().stream()
                .map(ServiceChargeMappingPairDTO::getToUuid)
                .collect(Collectors.toSet());

        return buildMappingContext(resolvedFromEntity, resolvedToEntity, resolvedChargeType, fromIds, toIds);
    }

    private MappingContext resolveUpdateContext(UpdateServiceChargeMappingRequest request) {
        String resolvedChargeType = requireDestinationChargeType(request.getChargeType());
        String resolvedFromEntity = ServiceCodesService.requireEntityCode(request.getFromEntityCode());
        String resolvedToEntity = ServiceCodesService.requireEntityCode(request.getToEntityCode());

        if (request.getMappings() == null || request.getMappings().isEmpty()) {
            return new MappingContext(
                    resolvedFromEntity,
                    resolvedToEntity,
                    resolvedChargeType,
                    Map.of(),
                    Map.of());
        }

        Set<UUID> fromIds = request.getMappings().stream()
                .map(MappingRequestDTO::getFromUuid)
                .collect(Collectors.toSet());
        Set<UUID> toIds = request.getMappings().stream()
                .map(MappingRequestDTO::getToUuid)
                .collect(Collectors.toSet());

        return buildMappingContext(resolvedFromEntity, resolvedToEntity, resolvedChargeType, fromIds, toIds);
    }

    private MappingContext buildMappingContext(
            String fromEntity,
            String toEntity,
            String chargeType,
            Set<UUID> fromIds,
            Set<UUID> toIds) {
        Map<UUID, ServiceCodes> fromById = indexServiceCodes(
                serviceCodesRepository.findAllByIdInWithEntity(fromIds), fromIds,
                ServiceChargeMappingMessageConstants.FROM_SERVICE_CODE_LABEL);
        Map<UUID, ServiceCodes> toById = indexServiceCodes(
                serviceCodesRepository.findAllByIdInWithEntity(toIds), toIds,
                ServiceChargeMappingMessageConstants.TO_SERVICE_CODE_LABEL);
        return new MappingContext(fromEntity, toEntity, chargeType, fromById, toById);
    }

    private ServiceChargeMapping buildMapping(UUID fromUuid, UUID toUuid, MappingContext context) {
        ServiceCodes fromServiceCode = context.fromServiceCodesById().get(fromUuid);
        ServiceCodes toServiceCode = context.toServiceCodesById().get(toUuid);
        validateServiceCodePair(fromServiceCode, toServiceCode, context);

        ServiceChargeMapping mapping = new ServiceChargeMapping();
        mapping.setFromServiceCode(fromServiceCode);
        mapping.setToServiceCode(toServiceCode);
        mapping.setFromEntity(context.fromEntity());
        mapping.setToEntity(context.toEntity());
        mapping.setChargeType(context.chargeType());
        return mapping;
    }

    private void applyServiceCodes(
            ServiceChargeMapping mapping, UUID fromUuid, UUID toUuid, MappingContext context) {
        ServiceCodes fromServiceCode = context.fromServiceCodesById().get(fromUuid);
        ServiceCodes toServiceCode = context.toServiceCodesById().get(toUuid);
        validateServiceCodePair(fromServiceCode, toServiceCode, context);
        mapping.setFromServiceCode(fromServiceCode);
        mapping.setToServiceCode(toServiceCode);
        mapping.setChargeType(context.chargeType());
    }

    private static Map<UUID, ServiceCodes> indexServiceCodes(
            List<ServiceCodes> loaded, Set<UUID> expectedIds, String label) {
        if (expectedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, ServiceCodes> byId = loaded.stream()
                .collect(Collectors.toMap(ServiceCodes::getId, Function.identity()));
        if (byId.size() != expectedIds.size()) {
            throw new ResourceNotFoundException(
                    ServiceChargeMappingMessageConstants.SERVICE_CODE_IDS_NOT_FOUND_PREFIX
                            + label
                            + ServiceChargeMappingMessageConstants.SERVICE_CODE_IDS_NOT_FOUND_SUFFIX);
        }
        return byId;
    }

    private void requireAllFromServiceCodesMappedInDatabase(MappingContext context) {
        List<ServiceChargeMapping> persistedMappings = serviceChargeMappingRepository
                .findByFromEntityAndToEntityAndChargeType(
                        context.fromEntity(), context.toEntity(), context.chargeType());
        Set<UUID> mappedFromIds = persistedMappings.stream()
                .map(mapping -> mapping.getFromServiceCode().getId())
                .collect(Collectors.toSet());
        requireAllFromServiceCodesMapped(context.fromEntity(), mappedFromIds);
    }

    private void assertPairNotUsedByAnotherMapping(
            UUID fromUuid, UUID toUuid, MappingContext context, UUID excludeMappingId) {
        if (serviceChargeMappingRepository.existsByPairAndEntitiesAndChargeTypeExcludingMappingId(
                fromUuid,
                toUuid,
                context.fromEntity(),
                context.toEntity(),
                context.chargeType(),
                excludeMappingId)) {
            throw new ConflictException(ServiceChargeMappingMessageConstants.MAPPING_ROW_EXISTS);
        }
    }

    private static boolean isMappingUnchanged(ServiceChargeMapping existing, UUID fromUuid, UUID toUuid) {
        return Objects.equals(existing.getFromServiceCode().getId(), fromUuid)
                && Objects.equals(existing.getToServiceCode().getId(), toUuid);
    }

    private List<ServiceCodes> findFromEntityServiceCodes(String fromEntity) {
        return serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                fromEntity, FROM_SOURCE_CHARGE_TYPES);
    }

    private void requireAllFromServiceCodesMapped(String fromEntity, Set<UUID> mappedFromIds) {
        List<ServiceCodes> requiredFromMasters = findFromEntityServiceCodes(fromEntity);
        if (requiredFromMasters.isEmpty()) {
            return;
        }

        List<String> unmappedCodes = requiredFromMasters.stream()
                .filter(master -> !mappedFromIds.contains(master.getId()))
                .map(ServiceCodes::getServiceCode)
                .toList();
        if (!unmappedCodes.isEmpty()) {
            throw new BadRequestException(
                    ServiceChargeMappingMessageConstants.ALL_FROM_SERVICE_CODES_MUST_BE_MAPPED
                            + String.join(", ", unmappedCodes));
        }
    }

    private void validateServiceCodePair(
            ServiceCodes fromServiceCode, ServiceCodes toServiceCode, MappingContext context) {
        if (!isActiveFromServiceCode(fromServiceCode, context.fromEntity())) {
            throw new BadRequestException(ServiceChargeMappingMessageConstants.FROM_SERVICE_CODE_INVALID);
        }
        if (!isActiveMasterForEntityAndChargeType(
                toServiceCode, context.toEntity(), context.chargeType())) {
            throw new BadRequestException(ServiceChargeMappingMessageConstants.TO_SERVICE_CODE_INVALID);
        }
    }

    private static boolean isActiveFromServiceCode(ServiceCodes serviceCode, String entityCode) {
        return serviceCode != null
                && serviceCode.getEntity() != null
                && entityCode.equalsIgnoreCase(serviceCode.getEntity().getCode())
                && FROM_SOURCE_CHARGE_TYPES.contains(
                        ChargeTypeConstants.normalize(serviceCode.getChargeType()))
                && !Boolean.TRUE.equals(serviceCode.getIsDeleted());
    }

    private static boolean isActiveMasterForEntityAndChargeType(
            ServiceCodes serviceCode, String entityCode, String chargeType) {
        return serviceCode != null
                && serviceCode.getEntity() != null
                && entityCode.equalsIgnoreCase(serviceCode.getEntity().getCode())
                && chargeType.equalsIgnoreCase(ChargeTypeConstants.normalize(serviceCode.getChargeType()))
                && !Boolean.TRUE.equals(serviceCode.getIsDeleted());
    }

    private static void assertMappingBelongsToContext(ServiceChargeMapping mapping, MappingContext context) {
        String persistedChargeType = mapping.getChargeType() != null
                ? mapping.getChargeType()
                : mapping.getToServiceCode() != null ? mapping.getToServiceCode().getChargeType() : null;
        if (!context.fromEntity().equalsIgnoreCase(mapping.getFromEntity())
                || !context.toEntity().equalsIgnoreCase(mapping.getToEntity())
                || persistedChargeType == null
                || !context.chargeType().equalsIgnoreCase(ChargeTypeConstants.normalize(persistedChargeType))) {
            throw new BadRequestException(ServiceChargeMappingMessageConstants.MAPPING_DOES_NOT_BELONG_TO_CONTEXT);
        }
    }

    private static String requireDestinationChargeType(String chargeType) {
        String resolved = ServiceCodesService.requireChargeType(chargeType);
        if (!ChargeTypeConstants.isEcsOrEc3(resolved)) {
            throw new BadRequestException(ServiceChargeMappingMessageConstants.INVALID_DESTINATION_CHARGE_TYPE);
        }
        return resolved;
    }

    private FromServiceCodeDTO toFromServiceCodeDto(ServiceCodes master) {
        return new FromServiceCodeDTO(
                master.getId(), master.getServiceCode(), master.getServiceDescription());
    }

    private ToServiceCodeDTO toToServiceCodeDto(ServiceCodes master) {
        return new ToServiceCodeDTO(
                master.getId(), master.getServiceCode(), master.getServiceDescription());
    }

    private ExistingMappingDTO toExistingMappingDto(ServiceChargeMapping mapping) {
        ServiceCodes from = mapping.getFromServiceCode();
        ServiceCodes to = mapping.getToServiceCode();
        String chargeType = mapping.getChargeType() != null
                ? mapping.getChargeType()
                : to.getChargeType();
        return new ExistingMappingDTO(
                mapping.getId(),
                from.getId(),
                from.getServiceCode(),
                to.getId(),
                to.getServiceCode(),
                mapping.getFromEntity(),
                mapping.getToEntity(),
                chargeType);
    }

    private ServiceChargeWithMappingsDTO toGroupedServiceChargeDto(
            ServiceCodes fromMaster, List<ServiceChargeMapping> mappings) {
        List<ServiceChargeMappingItemDTO> mappingItems = mappings.stream()
                .map(mapping -> new ServiceChargeMappingItemDTO(
                        mapping.getToServiceCode().getServiceCode(),
                        mapping.getToServiceCode().getServiceDescription()))
                .toList();

        return new ServiceChargeWithMappingsDTO(
                fromMaster.getId(),
                fromMaster.getServiceCode(),
                fromMaster.getServiceDescription(),
                fromMaster.getServiceDescription(),
                null,
                null,
                null,
                fromMaster.getTaxCode(),
                null,
                null,
                null,
                null,
                null,
                null,
                mappingItems);
    }

    private record MappingContext(
            String fromEntity,
            String toEntity,
            String chargeType,
            Map<UUID, ServiceCodes> fromServiceCodesById,
            Map<UUID, ServiceCodes> toServiceCodesById) {
    }
}
