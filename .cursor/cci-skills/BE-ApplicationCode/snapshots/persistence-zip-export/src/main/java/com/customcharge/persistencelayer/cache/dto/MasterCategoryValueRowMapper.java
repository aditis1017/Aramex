package com.customcharge.persistencelayer.cache.dto;

import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Entity;

public final class MasterCategoryValueRowMapper {

    private MasterCategoryValueRowMapper() {
    }

    public static MasterCategoryValueRow from(CategoryValue v) {
        Long categoryId = v.getCategory() != null ? v.getCategory().getId() : null;
        Long parentId = v.getParent() != null ? v.getParent().getId() : null;
        return new MasterCategoryValueRow(
                v.getId(),
                categoryId,
                v.getCode(),
                v.getName(),
                v.getLabel(),
                parentId,
                v.getEntityCode(),
                v.getIsActive(),
                v.getLoadDataFrom());
    }

    public static MasterCategoryValueRow fromCountry(Country c, boolean entityTreeRoot) {
        Boolean active = c.getActive() != null ? c.getActive() : Boolean.TRUE;
        Long id = entityTreeRoot && c.getId() != null ? -c.getId() : c.getId();
        return new MasterCategoryValueRow(id, null, c.getCode(), c.getName(), c.getName(), null, null, active, null);
    }

    public static MasterCategoryValueRow fromEntityRow(Entity e) {
        Boolean active = e.getActive() != null ? e.getActive() : Boolean.TRUE;
        Country country = e.getCountry();
        Long parentId = country != null && country.getId() != null ? -country.getId() : null;
        return new MasterCategoryValueRow(
                e.getId(), null, e.getCode(), e.getName(), e.getCode(), parentId, e.getCode(), active, null);
    }
}
