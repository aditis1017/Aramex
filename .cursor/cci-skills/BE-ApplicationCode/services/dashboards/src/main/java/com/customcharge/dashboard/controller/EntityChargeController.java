package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.dashboard.service.EntityChargeSearchService;
import com.customcharge.persistencelayer.search.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entity-charges")
@RequiredArgsConstructor
public class EntityChargeController {

    private final EntityChargeSearchService entityChargeSearchService;

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('DOCUMENTS_LIST_VIEW', 'CUSTOMS_VIEW')")
    public Page<EntityChargeGridRowDTO> search(
            @RequestBody List<SearchCriteria> filters,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return entityChargeSearchService.search(filters, page, size);
    }
}
