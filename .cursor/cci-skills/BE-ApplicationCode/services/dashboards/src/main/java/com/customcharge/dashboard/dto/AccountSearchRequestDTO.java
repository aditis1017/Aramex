package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountSearchRequestDTO {
    private String searchField;   // ACCOUNT_NUMBER / ACCOUNT_NAME
    private String searchType;    // CONTAINS / EXACT_MATCH
    private String searchText;         // user input
    private Long entityId;        // RUH
    private AccountNature accountNature;
    /** Zero-based page (optional; default 0). */
    private Integer page;
    /** Page size (optional; default 100, max 500). */
    private Integer pageSize;
}
