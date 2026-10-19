package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.search.SearchCriteria;
import com.customcharge.persistencelayer.search.SpecBuilderUtil;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;

public final class EntityChargeChargeSpecificationFactory {

    private EntityChargeChargeSpecificationFactory() {
    }

    public static Specification<Charge> entityIdIn(List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("entity").get("id").in(entityIds);
    }

    @SuppressWarnings("unchecked")
    public static Specification<Charge> build(
            List<Long> entityIds,
            boolean applyEntityScope,
            List<SearchCriteria> criteriaWithoutEntity,
            JpaSpecificationExecutor<Charge> chargeRepository) {
        Specification<Charge> spec = applyEntityScope ? entityIdIn(entityIds) : unconstrained();
        List<SearchCriteria> remapped = new ArrayList<>();
        for (SearchCriteria criterion : criteriaWithoutEntity) {
            SearchCriteria chargeCriterion = EntityChargeChargeFieldPaths.remapToChargePath(criterion);
            if (chargeCriterion != null) {
                remapped.add(chargeCriterion);
            }
        }
        if (remapped.isEmpty()) {
            return spec;
        }
        SpecBuilderUtil specBuilder = SpecBuilderUtil.getInstance()
                .addCriteria(remapped)
                .from(chargeRepository);
        Specification<Charge> filterSpec = (Specification<Charge>) (Object) specBuilder.getSpecification();
        return spec.and(filterSpec);
    }

    private static Specification<Charge> unconstrained() {
        return (root, query, cb) -> cb.conjunction();
    }
}
