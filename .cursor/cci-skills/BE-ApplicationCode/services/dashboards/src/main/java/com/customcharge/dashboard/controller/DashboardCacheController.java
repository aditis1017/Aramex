package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.cache.DashboardQueryCacheService;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class DashboardCacheController {

    private final DashboardQueryCacheService dashboardQueryCacheService;

    @DeleteMapping("/query-caches")
    public MessageResponse clearQueryCaches() {
        dashboardQueryCacheService.clearQueryCaches();
        return new MessageResponse("Dashboard query caches cleared successfully");
    }
}
