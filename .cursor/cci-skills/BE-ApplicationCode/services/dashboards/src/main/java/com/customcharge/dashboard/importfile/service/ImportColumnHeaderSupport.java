package com.customcharge.dashboard.importfile.service;

import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Component
public class ImportColumnHeaderSupport {

    private final ServiceCodesRepository serviceCodesRepository;

    public ImportColumnHeaderSupport(ServiceCodesRepository serviceCodesRepository) {
        this.serviceCodesRepository = serviceCodesRepository;
    }

    public List<String> resolveServiceDescriptionHeaders(String entity, List<String> chargeList) {
        if (chargeList == null || chargeList.isEmpty() || entity == null || entity.isBlank()) {
            return List.of();
        }
        String entityCode = entity.strip();
        List<ServiceCodes> masters = serviceCodesRepository.findMastersByEntityCodeAndChargeTypes(
                entityCode, chargeList);
        List<String> descriptions = masters.stream()
                .map(ServiceCodes::getServiceDescription)
                .toList();
        return ImportTemplateXlsxHeaderEnricher.distinctServiceDescriptions(descriptions);
    }

    public static List<String> parseCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static List<String> mergeCaseInsensitiveUnique(List<String> first, List<String> second) {
        LinkedHashMap<String, String> byLowerKey = new LinkedHashMap<>();
        appendCaseInsensitive(byLowerKey, first);
        appendCaseInsensitive(byLowerKey, second);
        return List.copyOf(byLowerKey.values());
    }

    private static void appendCaseInsensitive(LinkedHashMap<String, String> byLowerKey, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.strip();
            byLowerKey.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
    }
}
