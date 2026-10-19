package com.customcharge.global_settings.rules.dao;

import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class GlobalSettingDao {
    private final GlobalSettingRepository globalSettingRepository;

    public GlobalSettingDao(GlobalSettingRepository globalSettingRepository) {
        this.globalSettingRepository = globalSettingRepository;
    }

    public GlobalSetting save(GlobalSetting globalSetting) {
        return globalSettingRepository.save(globalSetting);
    }
    public List<GlobalSetting> saveAll(List<GlobalSetting> globalSettings) {
        return globalSettingRepository.saveAll(globalSettings);
    }
    public Optional<GlobalSetting> findById(UUID id) {
        return globalSettingRepository.findById(id);
    }

    public List<GlobalSetting> findAll(Sort sort) {
        return globalSettingRepository.findAll(sort);
    }

    public boolean existsById(UUID id) {
        return globalSettingRepository.existsById(id);
    }

    public void deleteById(UUID id) {
        globalSettingRepository.deleteById(id);
    }

    public List<GlobalSetting> findByStatusTrue(){
        return globalSettingRepository.findByStatusTrue();
    }


    public int countByTargetValueIn(List<String> targetValue){
        return globalSettingRepository.countByTargetValueIn(targetValue);
    }

    public List<GlobalSetting> findAllByTargetValueIn(List<String> targetValue,Sort sort){
        return globalSettingRepository.findAllByTargetValueIn(targetValue,sort);
    }

    public int bulkUpdateSequenceNumbers(Map<UUID, Integer> idToSequence, Collection<String> targetValues) {
        if (idToSequence == null || idToSequence.isEmpty()) {
            return 0;
        }
        UUID[] ids = idToSequence.keySet().toArray(new UUID[0]);
        Integer[] sequences = idToSequence.values().toArray(new Integer[0]);
        return globalSettingRepository.bulkUpdateSequenceNumbers(ids, sequences, targetValues);
    }
}
