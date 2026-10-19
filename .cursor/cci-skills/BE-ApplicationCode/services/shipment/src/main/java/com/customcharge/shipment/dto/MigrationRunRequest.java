package com.customcharge.shipment.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MigrationRunRequest {

    private List<String> hawbNumbers;
}
