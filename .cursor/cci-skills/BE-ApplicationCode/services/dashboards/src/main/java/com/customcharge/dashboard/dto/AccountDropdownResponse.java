package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDropdownResponse {
    private List<DropdownDTO> searchFields;
    private List<DropdownDTO> searchTypes;
    private List<DropdownDTO> entities;
}
