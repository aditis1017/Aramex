package com.customcharge.global_settings.rules.controller;

import com.customcharge.global_settings.rules.dto.GlobalSettingAuditHistoryCommitDto;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.global_settings.rules.dto.UpdateGlobalSettingSequenceRequest;
import com.customcharge.global_settings.rules.dto.UpdateStatusRequest;
import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingSyncService;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.search.SearchCriteria;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/rules")
public class GlobalSettingController {

    private final GlobalSettingService globalSettingService;
    private final GlobalSettingSyncService globalSettingSyncService;

    public GlobalSettingController(
            GlobalSettingService globalSettingService,
            GlobalSettingSyncService globalSettingSyncService) {
        this.globalSettingService = globalSettingService;
        this.globalSettingSyncService = globalSettingSyncService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_CREATE')")
    public GlobalSettingDto create(@Valid @RequestBody GlobalSettingDto dto) {
        return globalSettingService.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public Optional<GlobalSettingDto> update(@PathVariable String id, @Valid @RequestBody GlobalSettingDto dto) {
        return globalSettingService.update(UUID.fromString(id), dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_VIEW')")
    public Optional<GlobalSettingDto> getById(@PathVariable String id) {
        return globalSettingService.findById(UUID.fromString(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_VIEW')")
    public List<GlobalSettingDto> findAll(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return globalSettingService.findAll(page, size);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_DELETE')")
    public void deleteById(@PathVariable String id) {
        globalSettingService.deleteById(UUID.fromString(id));
    }

    @PostMapping("/history")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_VIEW')")
    public Page<GlobalSettingAuditHistoryCommitDto> getAuditHistory(@RequestBody AuditQueryRequest request) {
        return globalSettingService.getAuditHistory(request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public MessageResponse updateStatus(@PathVariable("id") String id,
                                        @RequestBody UpdateStatusRequest request) {
        return globalSettingService.updateStatus(UUID.fromString(id), request.isStatus());
    }

    @PostMapping("/criteria")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_VIEW')")
    public Page<GlobalSettingDto> getDataByCriteria(
            @RequestBody List<SearchCriteria> searchCriteria,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size) {
        return globalSettingService.getDataByCriteria(searchCriteria, page, size);
    }

    @PostMapping("/sync")
    public MessageResponse sync(
            @RequestParam(value = "syncDate", required = false) LocalDate referenceDate) {
        return globalSettingSyncService.syncGlobalSettings(referenceDate, true);
    }

    @PostMapping("/sequence")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public MessageResponse updateSequence(@RequestBody UpdateGlobalSettingSequenceRequest request) {
        return globalSettingService.updateSequence(request);
    }
}
