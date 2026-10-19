package com.customcharge.persistencelayer.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MasterDropdownResponse(
        String categoryName,
        List<DropdownOption> options,
        CategoryNode tree
) {
}
