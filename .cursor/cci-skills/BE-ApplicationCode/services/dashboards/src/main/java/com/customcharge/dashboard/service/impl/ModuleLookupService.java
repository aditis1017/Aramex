package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.persistencelayer.entities.Module;
import com.customcharge.persistencelayer.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleLookupService {

    private final ModuleRepository moduleRepository;

    @Cacheable(cacheNames = DashboardCacheNames.MODULES_ALL)
    public List<Module> findAllModules() {
        return Optional.ofNullable(moduleRepository.findAll()).orElse(Collections.emptyList());
    }
}
