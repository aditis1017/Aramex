package com.customcharge.persistencelayer.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class CategoryNode {
    private Long id;
    private String label;
    private String value;
    private Boolean active;
    private List<CategoryNode> children = new ArrayList<>();
}
