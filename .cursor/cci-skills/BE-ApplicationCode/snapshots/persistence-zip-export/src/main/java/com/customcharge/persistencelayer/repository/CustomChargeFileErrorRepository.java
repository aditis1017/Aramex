package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomChargeFileErrorRepository extends JpaRepository<CustomChargeFileError, UUID> {

    List<CustomChargeFileError> getAllByFileId(UUID fileId);

    Page<CustomChargeFileError> findByFileId(UUID fileId, Pageable pageable);
}
