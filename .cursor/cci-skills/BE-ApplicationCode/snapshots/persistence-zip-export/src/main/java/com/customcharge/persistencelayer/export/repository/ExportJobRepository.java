package com.customcharge.persistencelayer.export.repository;

import com.customcharge.persistencelayer.export.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
}
