package com.customcharge.dashboard.search;

import com.customcharge.dashboard.constants.PaymentTypeConstants;
import com.customcharge.persistencelayer.constants.ChargeAccountTypeConstants;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EntityChargeViewSearchSpecificationFactory {

    private EntityChargeViewSearchSpecificationFactory() {
    }

    /**
     * Matches shipment party numbers or the resolved display account_number
     * (AP for ECD/ECS/EC3, AR for DTA, shipment parties for DTC/DTS/DT3).
     */
    public static Specification<EntityChargeDashboardView> chargeIdIn(List<UUID> chargeIds) {
        if (chargeIds == null || chargeIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("chargeId").in(chargeIds);
    }

    public static Specification<EntityChargeDashboardView> accountNumberMatchesAnyParty(
            List<String> accountNumbers) {
        if (accountNumbers == null || accountNumbers.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Predicate consigneeMatch = root.get("consigneeNumber").in(accountNumbers);
            Predicate shipperMatch = root.get("shipperNumber").in(accountNumbers);
            Predicate thirdPartyMatch = root.get("thirdPartyNumber").in(accountNumbers);
            Predicate resolvedAccountMatch = root.get("accountNumber").in(accountNumbers);
            return cb.or(consigneeMatch, shipperMatch, thirdPartyMatch, resolvedAccountMatch);
        };
    }

    public static Specification<EntityChargeDashboardView> accountTypeIn(List<String> filterAccountTypes) {
        List<String> dbAccountTypes = mapFilterAccountTypesToDb(filterAccountTypes);
        if (dbAccountTypes.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("accountType").in(dbAccountTypes);
    }


    public static Specification<EntityChargeDashboardView> boeNumberMatchesAnyLineItem(
            List<String> boeNumbers) {
        if (boeNumbers == null || boeNumbers.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<ChargeLineItem> lineItemRoot = subquery.from(ChargeLineItem.class);
            subquery.select(cb.literal(1));
            subquery.where(
                    cb.equal(lineItemRoot.get("charge").get("id"), root.get("chargeId")),
                    lineItemRoot.get("declarationNumber").in(boeNumbers));
            return cb.exists(subquery);
        };
    }

    static List<String> mapFilterAccountTypesToDb(List<String> filterAccountTypes) {
        if (filterAccountTypes == null || filterAccountTypes.isEmpty()) {
            return List.of();
        }
        List<String> dbAccountTypes = new ArrayList<>();
        for (String filterToken : filterAccountTypes) {
            String dbValue = toDbAccountType(filterToken);
            if (dbValue != null && !dbAccountTypes.contains(dbValue)) {
                dbAccountTypes.add(dbValue);
            }
        }
        return List.copyOf(dbAccountTypes);
    }

    private static String toDbAccountType(String filterToken) {
        if (filterToken == null || filterToken.isBlank()) {
            return null;
        }
        String normalized = filterToken.trim().toLowerCase(Locale.ROOT);
        if (PaymentTypeConstants.FILTER_CASH.equals(normalized)) {
            return ChargeAccountTypeConstants.CASH;
        }
        if (PaymentTypeConstants.FILTER_ACCOUNT.equals(normalized)) {
            return ChargeAccountTypeConstants.ACCOUNT;
        }
        return null;
    }

}
