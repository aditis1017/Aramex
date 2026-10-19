package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.persistencelayer.dto.NotificationEventDto;
import com.customcharge.persistencelayer.dto.NotificationType;
import com.customcharge.persistencelayer.entities.StagedFileUpload;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ErrorReportMailService {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportMailService.class);
    private static final String TRACE_ID_MDC = "traceId";
    private static final String TEMPLATE_PATH = "error-report-download.ftl";
    private static final String SYSTEM_AUDITOR = "SYSTEM";

    @Value("${custom-charge.notification.sqs.queue:}")
    private String notificationQueue;

    private final ObjectProvider<MessagePublisher> sqsPublisherProvider;
    private final ObjectMapper objectMapper;
    private final StagedFileUploadRepository stagedFileUploadRepository;
    private final UserRepository userRepository;

    public ErrorReportMailService(
            @Qualifier("sqsPublisher") ObjectProvider<MessagePublisher> sqsPublisherProvider,
            ObjectMapper objectMapper,
            StagedFileUploadRepository stagedFileUploadRepository,
            UserRepository userRepository) {
        this.sqsPublisherProvider = sqsPublisherProvider;
        this.objectMapper = objectMapper;
        this.stagedFileUploadRepository = stagedFileUploadRepository;
        this.userRepository = userRepository;
    }

    public void sendReportMail(UUID fileId, String fileName, String downloadUrl) {
        MessagePublisher publisher = sqsPublisherProvider.getIfAvailable();
        if (publisher == null) {
            log.warn("SQS publisher is unavailable; skipping error report notification for fileId={}", fileId);
            return;
        }
        if (isBlank(notificationQueue)) {
            log.warn("Notification queue is not configured; skipping error report notification for fileId={}", fileId);
            return;
        }

        Optional<String> uploaderEmail = resolveUploaderEmail(fileId);
        if (uploaderEmail.isEmpty()) {
            return;
        }

        String recipientEmail = uploaderEmail.get();
        User recipient = userRepository.findByEmailIgnoreCaseWithRole(recipientEmail).orElse(null);
        String subject = "Custom Charge Error Report - " + fileName;
        try {
            NotificationEventDto event = new NotificationEventDto();
            event.setMessageId(UUID.randomUUID().toString());
            event.setTraceId(resolveTraceId());
            event.setNotificationType(NotificationType.MAIL);
            event.setTo(recipientEmail);
            event.setSubject(subject);
            event.setTemplatePath(TEMPLATE_PATH);
            event.setVariables(buildTemplateVariables(fileId, fileName, downloadUrl, recipient, recipientEmail));

            publisher.send(notificationQueue, objectMapper.writeValueAsString(event));
            log.info("Error report notification published to uploaderEmail={} fileId={}", recipientEmail, fileId);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize error report notification for uploaderEmail={} fileId={}",
                    recipientEmail, fileId, ex);
        } catch (Exception ex) {
            log.error("Failed to publish error report notification to uploaderEmail={} fileId={}",
                    recipientEmail, fileId, ex);
        }
    }

    private Optional<String> resolveUploaderEmail(UUID fileId) {
        Optional<StagedFileUpload> upload = stagedFileUploadRepository.findByFileId(fileId.toString());
        if (upload.isEmpty()) {
            log.warn("StagedFileUpload not found; skipping error report notification fileId={}", fileId);
            return Optional.empty();
        }

        String createdBy = upload.get().getCreatedBy();
        if (isBlank(createdBy) || SYSTEM_AUDITOR.equalsIgnoreCase(createdBy.trim())) {
            log.warn("Uploader email unavailable (createdBy={}); skipping error report notification fileId={}",
                    createdBy, fileId);
            return Optional.empty();
        }

        return Optional.of(createdBy.trim());
    }

    private Map<String, Object> buildTemplateVariables(
            UUID fileId, String fileName, String downloadUrl, User recipient, String recipientEmail) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("timeLine", "24");
        variables.put("fileId", fileId);
        variables.put("fileName", fileName);
        variables.put("organizationName", "Aramex");
        variables.put("downloadUrl", downloadUrl);
        variables.put("userName", safeName(recipient, recipientEmail));
        return variables;
    }

    private String resolveTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC);
        if (!isBlank(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private static String safeName(User user, String fallbackEmail) {
        if (user != null && !isBlank(user.getUsername())) {
            return user.getUsername();
        }
        if (user != null && !isBlank(user.getEmail())) {
            return user.getEmail();
        }
        return fallbackEmail;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
