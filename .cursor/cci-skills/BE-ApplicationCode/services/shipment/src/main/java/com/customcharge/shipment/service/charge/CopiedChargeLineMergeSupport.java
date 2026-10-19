package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges cost lines copied from a paired cost charge with revenue-defined charge lines.
 * Applies to every revenue charge that copies from a cost charge (DTC/DTA from ECD, DTS from ECS,
 * DT3 from EC3). When the same normalized service code exists in both lists, the revenue line wins.
 */
public final class CopiedChargeLineMergeSupport {

    private CopiedChargeLineMergeSupport() {
    }

    public static List<ChargeLineItem> mergeCopiedWithRevenueLines(
            List<ChargeLineItem> copiedLines,
            List<ChargeLineItem> revenueLines) {
        List<ChargeLineItem> copied = copiedLines == null ? List.of() : copiedLines;
        List<ChargeLineItem> revenue = revenueLines == null ? List.of() : revenueLines;
        if (copied.isEmpty()) {
            return new ArrayList<>(revenue);
        }
        if (revenue.isEmpty()) {
            return new ArrayList<>(copied);
        }

        Map<String, ChargeLineItem> revenueByCode = indexByServiceCode(revenue);
        Set<String> addedCodes = new HashSet<>();
        List<ChargeLineItem> merged = new ArrayList<>();

        for (ChargeLineItem copiedLine : copied) {
            String serviceCode = ChargeServiceCodeSupport.normalize(copiedLine.getServiceCode());
            if (!StringUtils.hasText(serviceCode)) {
                merged.add(copiedLine);
                continue;
            }
            if (!addedCodes.add(serviceCode)) {
                continue;
            }
            merged.add(revenueByCode.getOrDefault(serviceCode, copiedLine));
        }

        for (ChargeLineItem revenueLine : revenue) {
            String serviceCode = ChargeServiceCodeSupport.normalize(revenueLine.getServiceCode());
            if (!StringUtils.hasText(serviceCode)) {
                merged.add(revenueLine);
                continue;
            }
            if (addedCodes.add(serviceCode)) {
                merged.add(revenueLine);
            }
        }
        return merged;
    }

    private static Map<String, ChargeLineItem> indexByServiceCode(List<ChargeLineItem> lines) {
        Map<String, ChargeLineItem> indexed = new HashMap<>();
        for (ChargeLineItem line : lines) {
            String serviceCode = ChargeServiceCodeSupport.normalize(line.getServiceCode());
            if (StringUtils.hasText(serviceCode)) {
                indexed.putIfAbsent(serviceCode, line);
            }
        }
        return indexed;
    }
}
