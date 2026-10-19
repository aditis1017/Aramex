package com.customcharge.global_settings.rules.scheduler;

import com.customcharge.global_settings.rules.config.GlobalSettingSyncSchedulerProperties;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingSyncService;
import com.customcharge.persistencelayer.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@EnableConfigurationProperties(GlobalSettingSyncSchedulerProperties.class)
public class GlobalSettingSyncScheduler {

    private final GlobalSettingSyncService globalSettingSyncService;
    private final GlobalSettingSyncSchedulerProperties properties;

    public GlobalSettingSyncScheduler(
            GlobalSettingSyncService globalSettingSyncService,
            GlobalSettingSyncSchedulerProperties properties) {
        this.globalSettingSyncService = globalSettingSyncService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!properties.isOnStartupEnabled()) {
            log.debug("Global settings startup sync is disabled");
            return;
        }
        runSync("startup");
    }

    @Scheduled(
            cron = "${global-settings.sync.cron:1 0 0 * * ?}",
            zone = "${global-settings.sync.zone:UTC}")
    public void syncOnSchedule() {
        if (!properties.isScheduledEnabled()) {
            return;
        }
        runSync("scheduled");
    }

    private void runSync(String trigger) {
        try {
            log.info("Global settings sync triggered by {}", trigger);
            globalSettingSyncService.syncGlobalSettings(LocalDate.now(), true);
            log.info("Global settings sync finished for trigger={}", trigger);
        } catch (ConflictException ex) {
            log.warn("Global settings sync skipped ({}): {}", trigger, ex.getMessage());
        } catch (Exception ex) {
            log.error("Global settings sync failed ({})", trigger, ex);
        }
    }
}
