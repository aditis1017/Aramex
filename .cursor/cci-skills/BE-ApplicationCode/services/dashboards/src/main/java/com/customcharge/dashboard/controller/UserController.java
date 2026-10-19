package com.customcharge.dashboard.controller;

import com.customcharge.persistencelayer.dto.ExchangeRateRequest;
import com.customcharge.persistencelayer.dto.ExchangeRateResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.customcharge.dashboard.service.UserRoleService;
import com.customcharge.persistencelayer.dto.response.ApiResponse;
import com.customcharge.persistencelayer.dto.response.ExceptionResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserRoleService userRoleService;
    private final RestClient restClient = RestClient.create();

    @PostMapping("/setup/default-users")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ApiResponse<String, ExceptionResponse> createDefaultUsers() {
        userRoleService.createDefaultUsers();
        return ApiResponse.success("5 default users created", null);
    }

    @PostMapping("/setup/modules")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ApiResponse<String, ExceptionResponse> createModules() {
        userRoleService.createModules();
        return ApiResponse.success("Modules created successfully", null);
    }




    @PostMapping("/exchange")
    public double fetchRateFromApi(@RequestBody ExchangeRateRequest request) {
        String apiUrl= "http://devbe01.aramex.com/corp.shippingapi.coredata.v1/api/DataUtilities/GetExchangeRate";
//        ExchangeRateRequest request = new ExchangeRateRequest(1.0, from, to);
        try {
            ExchangeRateResponse response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExchangeRateResponse.class);

            Double convertedAmount = response != null ? response.convertedAmount() : null;
            if (convertedAmount == null || convertedAmount <= 0 || !Double.isFinite(convertedAmount)) {
                throw new IllegalStateException("Invalid convertedAmount from exchange API");
            }
            return convertedAmount;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Exchange rate HTTP " + ex.getStatusCode().value() + ": " + ex.getStatusText(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Exchange rate call failed: " + ex.getMessage(), ex);
        }
    }
}