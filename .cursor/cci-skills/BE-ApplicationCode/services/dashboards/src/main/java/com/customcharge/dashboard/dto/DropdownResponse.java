package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DropdownResponse {

    private List<DropdownDTO> entities;
    private List<DropdownDTO> accountTypes;
    private List<DropdownDTO> accountStatuses;
    private List<DropdownDTO> currencies;
    private List<DropdownDTO> marketStructures;
    private List<DropdownDTO> splitInvoices;
    private List<DropdownDTO> invoiceFrequencies;
    private List<DropdownDTO> triggerInvoices;
    private List<DropdownDTO> billings;
}