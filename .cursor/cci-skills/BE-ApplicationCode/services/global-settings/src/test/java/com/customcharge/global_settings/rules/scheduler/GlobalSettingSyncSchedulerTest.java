package com.customcharge.global_settings.rules.scheduler;

import com.customcharge.global_settings.rules.config.GlobalSettingSyncSchedulerProperties;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingSyncService;
import com.customcharge.persistencelayer.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingSyncSchedulerTest {

    @Mock
    private GlobalSettingSyncService globalSettingSyncService;

    private GlobalSettingSyncSchedulerProperties properties;
    private GlobalSettingSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new GlobalSettingSyncSchedulerProperties();
        scheduler = new GlobalSettingSyncScheduler(globalSettingSyncService, properties);
    }

    @Test
    void syncOnStartup_shouldRunSync_whenEnabled() {
        properties.setOnStartupEnabled(true);

        scheduler.syncOnStartup();

        verify(globalSettingSyncService).syncGlobalSettings(eq(LocalDate.now()), eq(true));
    }

    @Test
    void syncOnStartup_shouldSkip_whenDisabled() {
        properties.setOnStartupEnabled(false);

        scheduler.syncOnStartup();

        verify(globalSettingSyncService, never()).syncGlobalSettings(eq(LocalDate.now()), eq(true));
    }

    @Test
    void syncOnSchedule_shouldRunSync_whenEnabled() {
        properties.setScheduledEnabled(true);

        scheduler.syncOnSchedule();

        verify(globalSettingSyncService).syncGlobalSettings(eq(LocalDate.now()), eq(true));
    }

    @Test
    void syncOnSchedule_shouldSkip_whenDisabled() {
        properties.setScheduledEnabled(false);

        scheduler.syncOnSchedule();

        verify(globalSettingSyncService, never()).syncGlobalSettings(eq(LocalDate.now()), eq(true));
    }

    @Test
    void runSync_shouldNotPropagateConflictException() {
        properties.setOnStartupEnabled(true);
        when(globalSettingSyncService.syncGlobalSettings(eq(LocalDate.now()), eq(true)))
                .thenThrow(new ConflictException("already running"));

        scheduler.syncOnStartup();

        verify(globalSettingSyncService).syncGlobalSettings(eq(LocalDate.now()), eq(true));
    }
}
