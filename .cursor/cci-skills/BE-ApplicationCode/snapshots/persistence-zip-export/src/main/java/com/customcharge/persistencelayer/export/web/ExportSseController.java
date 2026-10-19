package com.customcharge.persistencelayer.export.web;

import com.customcharge.persistencelayer.export.config.ExportEngineProperties;
import com.customcharge.persistencelayer.export.dto.ExportProgressPayload;
import com.customcharge.persistencelayer.export.entity.ExportJob;
import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import com.customcharge.persistencelayer.export.service.ExportDownloadLinkService;
import com.customcharge.persistencelayer.export.service.ExportEngineService;
import com.customcharge.persistencelayer.export.service.ExportJobProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/exports")
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExportSseController {

    private static final Logger log = LoggerFactory.getLogger(ExportSseController.class);

    private final ConcurrentHashMap<SseEmitter, String> lastProgressJsonByEmitter = new ConcurrentHashMap<>();

    private final ExportSseHub exportSseHub;
    private final ExportJobProgressService exportJobProgressService;
    private final ExportEngineService exportEngineService;
    private final Optional<ExportDownloadLinkService> exportDownloadLinkService;
    private final ExportEngineProperties exportEngineProperties;
    private final ScheduledExecutorService exportSseScheduler;
    private final ObjectMapper objectMapper;

    public ExportSseController(
            ExportSseHub exportSseHub,
            ExportJobProgressService exportJobProgressService,
            ExportEngineService exportEngineService,
            Optional<ExportDownloadLinkService> exportDownloadLinkService,
            ExportEngineProperties exportEngineProperties,
            ScheduledExecutorService exportSseScheduler,
            ObjectMapper objectMapper) {
        this.exportSseHub = exportSseHub;
        this.exportJobProgressService = exportJobProgressService;
        this.exportEngineService = exportEngineService;
        this.exportDownloadLinkService = exportDownloadLinkService;
        this.exportEngineProperties = exportEngineProperties;
        this.exportSseScheduler = exportSseScheduler;
        this.objectMapper = objectMapper;
    }

    @GetMapping(path = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public SseEmitter events(@PathVariable UUID jobId) {
        SseEmitter emitter = new SseEmitter(0L);
        exportSseHub.register(jobId, emitter);

        try {
            ExportProgressPayload initial = withDownloadUrl(jobId, exportJobProgressService.payloadFor(jobId));
            sendProgressIfChanged(emitter, initial);
            if (isTerminal(initial.status())) {
                emitter.complete();
                return emitter;
            }
        } catch (Exception e) {
            log.warn("Failed to load initial SSE payload jobId={}", jobId, e);
            emitter.completeWithError(e);
            return emitter;
        }

        ScheduledFuture<?> heartbeat = exportSseScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(emitter),
                exportEngineProperties.getSseHeartbeatSeconds(),
                exportEngineProperties.getSseHeartbeatSeconds(),
                TimeUnit.SECONDS);

        ScheduledFuture<?> poll = exportSseScheduler.scheduleAtFixedRate(
                () -> pollDbAndPush(jobId, emitter),
                exportEngineProperties.getSseDbPollIntervalMs(),
                exportEngineProperties.getSseDbPollIntervalMs(),
                TimeUnit.MILLISECONDS);

        Runnable onClose = () -> {
            lastProgressJsonByEmitter.remove(emitter);
            cancelQuietly(heartbeat, poll);
        };
        emitter.onCompletion(onClose);
        emitter.onTimeout(onClose);
        emitter.onError(e -> onClose.run());

        return emitter;
    }

    private void pollDbAndPush(UUID jobId, SseEmitter emitter) {
        try {
            ExportProgressPayload payload = withDownloadUrl(jobId, exportJobProgressService.payloadFor(jobId));
            sendProgressIfChanged(emitter, payload);
            if (isTerminal(payload.status())) {
                emitter.complete();
            }
        } catch (IllegalStateException e) {
            log.trace("SSE poll skipped; emitter already completed jobId={}", jobId);
        } catch (Exception e) {
            log.debug("SSE poll ended jobId={}", jobId, e);
            emitter.completeWithError(e);
        }
    }

    private static boolean isTerminal(ExportJobStatus status) {
        return status == ExportJobStatus.COMPLETED || status == ExportJobStatus.FAILED;
    }

    private void sendProgressIfChanged(SseEmitter emitter, ExportProgressPayload payload) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        String previous = lastProgressJsonByEmitter.put(emitter, json);
        if (json.equals(previous)) {
            return;
        }
        emitter.send(SseEmitter.event().name("progress").data(json, MediaType.APPLICATION_JSON));
    }

    private ExportProgressPayload withDownloadUrl(UUID jobId, ExportProgressPayload payload) {
        if (!payload.downloadAvailable()) {
            return payload;
        }
        try {
            ExportJob job = exportEngineService.requireJob(jobId);
            if (job.getS3Bucket() == null || job.getS3Key() == null) {
                return payload;
            }
            ExportDownloadLinkService linkService = exportDownloadLinkService.orElse(null);
            if (linkService == null) {
                return payload;
            }
            String url = linkService.presignGetObject(job.getS3Bucket(), job.getS3Key(), job.getContentType()).toString();
            return new ExportProgressPayload(
                    payload.jobId(),
                    payload.status(),
                    payload.totalRecords(),
                    payload.processedRecords(),
                    payload.percentComplete(),
                    payload.updatedAt(),
                    payload.errorCode(),
                    payload.errorMessage(),
                    payload.downloadAvailable(),
                    url
            );
        } catch (Exception ex) {
            log.debug("Could not enrich SSE payload with download URL for jobId={}", jobId, ex);
            return payload;
        }
    }

    private static void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } catch (Exception e) {
            log.trace("SSE heartbeat skipped");
        }
    }

    private static void cancelQuietly(ScheduledFuture<?> heartbeat, ScheduledFuture<?> poll) {
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
        if (poll != null) {
            poll.cancel(false);
        }
    }
}
