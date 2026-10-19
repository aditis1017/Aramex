package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "custom_charge_file_error")
public class CustomChargeFileError extends AbstractEntity {

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_list", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, String>> errorList;
}
