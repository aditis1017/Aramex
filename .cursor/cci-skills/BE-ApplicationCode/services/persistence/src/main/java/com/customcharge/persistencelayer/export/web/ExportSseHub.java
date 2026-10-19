package com.customcharge.persistencelayer.export.web;

import com.customcharge.persistencelayer.export.dto.ExportProgressPayload;
import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExportSseHub {

    private static final Logger log = LoggerFactory.getLogger(ExportSseHub.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByJob = new ConcurrentHashMap<>();

    public ExportSseHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(UUID jobId, SseEmitter emitter) {
        emittersByJob.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(jobId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
    }

    public void broadcast(ExportProgressPayload payload) {
        List<SseEmitter> list = emittersByJob.get(payload.jobId());
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("progress").data(json, MediaType.APPLICATION_JSON));
                    if (isTerminal(payload.status())) {
                        emitter.complete();
                    }
                } catch (IOException | IllegalStateException ex) {
                    remove(payload.jobId(), emitter);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast export progress jobId={}", payload.jobId(), e);
        }
    }

    private static boolean isTerminal(ExportJobStatus status) {
        return status == ExportJobStatus.COMPLETED || status == ExportJobStatus.FAILED;
    }

    private void remove(UUID jobId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByJob.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByJob.remove(jobId, list);
            }
        }
    }
}
