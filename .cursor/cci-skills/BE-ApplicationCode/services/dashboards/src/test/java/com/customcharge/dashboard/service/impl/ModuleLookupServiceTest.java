package com.customcharge.dashboard.service.impl;

import com.customcharge.persistencelayer.entities.Module;
import com.customcharge.persistencelayer.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleLookupServiceTest {

    @Mock
    private ModuleRepository moduleRepository;

    @InjectMocks
    private ModuleLookupService moduleLookupService;

    @Test
    void findAllModules_shouldReturnRepositoryResults() {
        Module module = new Module();
        when(moduleRepository.findAll()).thenReturn(List.of(module));

        assertThat(moduleLookupService.findAllModules()).containsExactly(module);
    }

    @Test
    void findAllModules_shouldReturnEmptyList_whenRepositoryReturnsNull() {
        when(moduleRepository.findAll()).thenReturn(null);

        assertThat(moduleLookupService.findAllModules()).isEmpty();
    }
}
