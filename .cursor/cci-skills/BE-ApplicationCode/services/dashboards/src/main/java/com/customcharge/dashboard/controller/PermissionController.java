package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.PermissionMatrixDTO;
import com.customcharge.dashboard.service.PermissionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Validated
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ResponseEntity<List<PermissionMatrixDTO>> getMatrix(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Positive Integer size) {
        return ResponseEntity.ok(permissionService.getMatrix(page, size));
    }
}