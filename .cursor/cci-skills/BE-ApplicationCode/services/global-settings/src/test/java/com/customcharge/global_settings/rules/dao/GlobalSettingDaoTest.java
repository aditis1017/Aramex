package com.customcharge.global_settings.rules.dao;

import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingDaoTest {

    @Mock
    private GlobalSettingRepository globalSettingRepository;

    @InjectMocks
    private GlobalSettingDao globalSettingDao;

    @Test
    void save_shouldDelegateToRepository() {
        GlobalSetting setting = new GlobalSetting();
        when(globalSettingRepository.save(setting)).thenReturn(setting);

        assertThat(globalSettingDao.save(setting)).isSameAs(setting);
        verify(globalSettingRepository).save(setting);
    }

    @Test
    void findById_shouldDelegateToRepository() {
        UUID id = UUID.randomUUID();
        when(globalSettingRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(globalSettingDao.findById(id)).isEmpty();
    }

    @Test
    void findByStatusTrue_shouldDelegateToRepository() {
        when(globalSettingRepository.findByStatusTrue()).thenReturn(List.of());

        assertThat(globalSettingDao.findByStatusTrue()).isEmpty();
        verify(globalSettingRepository).findByStatusTrue();
    }

    @Test
    void saveAll_shouldDelegateToRepository() {
        List<GlobalSetting> settings = List.of(new GlobalSetting());
        when(globalSettingRepository.saveAll(settings)).thenReturn(settings);

        assertThat(globalSettingDao.saveAll(settings)).isSameAs(settings);
        verify(globalSettingRepository).saveAll(settings);
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        Sort sort = Sort.by("sequenceNumber");
        when(globalSettingRepository.findAll(sort)).thenReturn(List.of());

        assertThat(globalSettingDao.findAll(sort)).isEmpty();
        verify(globalSettingRepository).findAll(sort);
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        UUID id = UUID.randomUUID();
        when(globalSettingRepository.existsById(id)).thenReturn(true);

        assertThat(globalSettingDao.existsById(id)).isTrue();
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        UUID id = UUID.randomUUID();

        globalSettingDao.deleteById(id);

        verify(globalSettingRepository).deleteById(id);
    }

    @Test
    void countByTargetValueIn_shouldDelegateToRepository() {
        when(globalSettingRepository.countByTargetValueIn(List.of("DDU"))).thenReturn(3);

        assertThat(globalSettingDao.countByTargetValueIn(List.of("DDU"))).isEqualTo(3);
    }

    @Test
    void findAllByTargetValueIn_shouldDelegateToRepository() {
        Sort sort = Sort.by("sequenceNumber");
        when(globalSettingRepository.findAllByTargetValueIn(List.of("DDU"), sort)).thenReturn(List.of());

        assertThat(globalSettingDao.findAllByTargetValueIn(List.of("DDU"), sort)).isEmpty();
    }
}
