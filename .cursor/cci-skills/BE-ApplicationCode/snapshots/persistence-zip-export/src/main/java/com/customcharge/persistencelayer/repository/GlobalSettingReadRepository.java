package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GlobalSettingReadRepository extends JpaRepository<GlobalSetting, UUID> {

    List<GlobalSetting> findByDestinationAndTargetValueAndStatusTrue(
            String destination, String targetValue);
}
