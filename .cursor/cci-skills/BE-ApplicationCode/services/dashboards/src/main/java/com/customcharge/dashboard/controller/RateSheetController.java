package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.*;
import com.customcharge.dashboard.service.AccountRateSheetResolver;
import com.customcharge.dashboard.service.impl.RateSheetService;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rate-sheets")
@RequiredArgsConstructor
public class RateSheetController {

    private final RateSheetService service;
    private final AccountRateSheetResolver accountRateSheetResolver;

    @PostMapping("/items")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_CREATE', 'AP_MANAGEMENT_CREATE', 'ENTITY_SETTINGS_CREATE')")
    public CreateRateSheetItemResponse createItem(@Valid @RequestBody CreateRateSheetItemRequest req) {
        return service.createItem(req);
    }

    @PutMapping("/updateItem")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_EDIT', 'AP_MANAGEMENT_EDIT', 'ENTITY_SETTINGS_EDIT')")
    public List<CreateRateSheetItemResponse> updateItems(
            @Valid @RequestBody List<@Valid CreateRateSheetItemRequest> requests) {
        return service.updateItems(requests);
    }

    @DeleteMapping("/items")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_DELETE', 'AP_MANAGEMENT_DELETE', 'ENTITY_SETTINGS_DELETE')")
    public MessageResponse deleteItem(
            @RequestParam UUID id) {
        service.deleteItem(id);
        return new MessageResponse("Item deleted");
    }

//    @GetMapping("/{rateSheetId}")
//    public List<RateSheetItemResponse> getRateSheet(@PathVariable UUID rateSheetId) {
//        return service.getRateSheet(rateSheetId);
//    }

    @GetMapping("/getAllRateSheetItem")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW', 'ENTITY_SETTINGS_VIEW')")
    public List<RateSheetItemResponse> getRateSheet(
            @RequestParam(required = false) UUID rateSheetId,
            @RequestParam(required = false) AccountNature accountNature,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String chargeType) {
        if (rateSheetId == null) {
            return List.of();
        }
        return service.getRateSheet(rateSheetId);
    }

//    @PostMapping("/upsert")
//    public MessageResponse upsert(
//            @Valid @RequestBody RateConfigRequest request) {
//        return service.upsert(request);
//    }

    @GetMapping("/getDropDownData")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_EDIT', 'AP_MANAGEMENT_EDIT', 'ENTITY_SETTINGS_EDIT')")
    public Map<String, Object> getDropDownData(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) AccountNature accountNature,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String chargeType) {
        return service.buildResponse(accountId, accountNumber, accountNature, entityCode, chargeType);
    }

    @GetMapping("/getRateSheetItem")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW', 'ENTITY_SETTINGS_VIEW')")
    public RateSheetItemUpdateDTO getRateSheetItem(@RequestParam UUID rateSheetItemId) {
        return service.getRateSheetItemID(rateSheetItemId);
    }
}