package com.customcharge.dashboard.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DropdownDTO {
    private Long id;      // DB id OR null for enums
    private String code;  // Enum.name() OR CategoryValue.code
    private String name;  // Display name
}