package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EntityChargeSearchService {
    Page<EntityChargeGridRowDTO> search(List<SearchCriteria> filters, Integer page, Integer size);
}
