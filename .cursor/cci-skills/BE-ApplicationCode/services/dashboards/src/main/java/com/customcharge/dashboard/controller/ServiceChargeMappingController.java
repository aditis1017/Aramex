package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.CreateServiceChargeMappingRequest;
import com.customcharge.dashboard.dto.UpdateServiceChargeMappingRequest;
import com.customcharge.dashboard.dto.ServiceChargeMappingScreenDTO;
import com.customcharge.dashboard.service.impl.ServiceChargeMappingService;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-charge-mappings")
@RequiredArgsConstructor
@Tag(name = "Service Charge Mapping", description = "DTA/ECD to ECS/EC3 service code mapping")
public class ServiceChargeMappingController {

    private final ServiceChargeMappingService serviceChargeMappingService;

    @GetMapping
    @PreAuthorize("hasAuthority('AR_MANAGEMENT_VIEW')")
    @Operation(summary = "Load service charge mapping screen data")
    public ServiceChargeMappingScreenDTO getMappingScreenData(
            @RequestParam String chargeType,
            @RequestParam String fromEntityCode,
            @RequestParam String toEntityCode,
            @RequestParam(required = false, defaultValue = "false") Boolean editFlag) {
        return serviceChargeMappingService.getMappingScreenData(
                chargeType, fromEntityCode, toEntityCode, editFlag);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AR_MANAGEMENT_EDIT')")
    @Operation(summary = "Create service charge mappings")
    public MessageResponse createMappings(@Valid @RequestBody CreateServiceChargeMappingRequest request) {
        return serviceChargeMappingService.createMappings(request);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('AR_MANAGEMENT_EDIT')")
    @Operation(summary = "Partial update: send only changed rows (with mappingId) or new rows (without mappingId)")
    public MessageResponse updateMappings(@Valid @RequestBody UpdateServiceChargeMappingRequest request) {
        return serviceChargeMappingService.updateMappings(request);
    }
}
