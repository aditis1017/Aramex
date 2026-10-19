package com.customcharge.persistencelayer.common;

import lombok.Data;

import java.util.List;

@Data
public class HierarchyRequest {
    private String categoryName;
    private List<String> path;
}