package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSearchResponseDTO {
    private String accountNumber;
    private String accountName;
    private String entity;
    private String createdBy;
    private Long entityId;
    private String accountStatus;
    private String createdDate;
    private String updatedDate;
}
