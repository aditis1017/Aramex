package com.customcharge.persistencelayer.common;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/master-data")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_VIEW')")
    public List<MasterDropdownResponse> masterData(@RequestBody List<HierarchyRequest> requests) {
        return categoryService.loadMasterData(requests);
    }
}
