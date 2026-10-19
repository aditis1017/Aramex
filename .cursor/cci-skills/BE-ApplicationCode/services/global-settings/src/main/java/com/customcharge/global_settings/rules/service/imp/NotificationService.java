package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.NotificationEventDto;
import com.customcharge.persistencelayer.dto.NotificationType;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final String TRACE_ID_MDC = "traceId";

    @Value("${createRuleUiLink}")
    private String createRuleUiLink;

    @Value("${custom-charge.notification.sqs.queue:}")
    private String notificationQueue;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String CREATE_TEMPLATE = "create-update-notification.ftl";
    private static final String STATUS_TEMPLATE = "status-change-notification.ftl";
    private static final String CREATE_SUBJECT = "Global Setting Created";
    private static final String UPDATE_SUBJECT = "Global Setting Updated";
    private static final String STATUS_SUBJECT = "Global Setting";
    private static final String DEFAULT_ORGANIZATION_NAME = "Aramex";

    private final ObjectProvider<MessagePublisher> sqsPublisherProvider;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    public NotificationService(
            @Qualifier("sqsPublisher") ObjectProvider<MessagePublisher> sqsPublisherProvider,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        this.sqsPublisherProvider = sqsPublisherProvider;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
    }

    @Async
    public void sendCreateOrUpdateNotification(List<String> roleNames,
                                               String action,
                                               String shipmentTypeCode,
                                               String recordUuid,
                                               String performedBy, String targetField) {
        try {
            String category = resolveCategory(shipmentTypeCode);
            String actionValue = action == null ? "" : action.trim().toUpperCase();
            String actionPerformed = "CREATED".equals(actionValue)
                    ? "Create Rule"
                    : "Edit Rule";
            String subject = "CREATED".equals(actionValue)
                    ? CREATE_SUBJECT
                    : UPDATE_SUBJECT;
           String ruleType=
                    (category.equalsIgnoreCase("DDU")  || category.equalsIgnoreCase("DDP")) ?category: "Category";
            String actionSummary=  "Mark Shipment "+  targetField;
            notifyUsersByRoles(roleNames, CREATE_TEMPLATE, subject, user -> {
                Map<String, Object> model = new LinkedHashMap<>();
                model.put("userName", safeName(user));
                model.put("action", actionValue);
                model.put("actionPerformed", actionPerformed);
                model.put("category", category);
                model.put("actionSummary",actionSummary);
                model.put("performedBy", performedBy);
                model.put("ruleType",ruleType);
                model.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FORMATTER) + " UTC");
                model.put("ruleLink", createRuleUiLink);
                model.put("organizationName", DEFAULT_ORGANIZATION_NAME);
                return model;
            });
        } catch (Exception ex) {
            log.error("Create/update notification flow failed for uuid={}", recordUuid, ex);
        }
    }

    @Async
    public void sendStatusChangeNotification(List<String> roleNames,
                                             String oldStatus,
                                             String newStatus,
                                             String shipmentTypeCode,
                                             String recordUuid,
                                             String performedBy) {
        try {
            String category = resolveCategory(shipmentTypeCode);
            String actionPerformed= (Objects.equals(newStatus, "INACTIVE") ? "Deactivate Rule" : " Activate Rule");
            String actionSummary=  (Objects.equals(newStatus, "INACTIVE")) ?
                    "The rule has been disabled and will no longer influence shipment classification or processing." :
                    "The previously inactive rule has been reactivated and will now be applied during shipment processing.";
            String action= Objects.equals(newStatus, "INACTIVE") ? "DEACTIVATED" : "ACTIVATED";
            String ruleType=
                    (category.equalsIgnoreCase("DDU")  || category.equalsIgnoreCase("DDP")) ? category: "Category" ;
            notifyUsersByRoles(roleNames, STATUS_TEMPLATE,
                    STATUS_SUBJECT + " " + (Objects.equals(newStatus, "INACTIVE") ? "Deactivated" : "Activated"),
                    user -> {
                        Map<String, Object> model = new LinkedHashMap<>();
                        model.put("actionPerformed", actionPerformed);
                        model.put("action", action);
                        model.put("userName", safeName(user));
                        model.put("oldStatus", oldStatus);
                        model.put("newStatus", newStatus);
                        model.put("category", category);
                        model.put("ruleType",ruleType);
                        model.put("actionSummary",actionSummary);
                        model.put("performedBy", performedBy);
                        model.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FORMATTER) + " UTC");
                        model.put("ruleLink", createRuleUiLink);
                        model.put("organizationName", DEFAULT_ORGANIZATION_NAME);
                        return model;
                    });
        } catch (Exception ex) {
            log.error("Status notification flow failed for uuid={}", recordUuid, ex);
        }
    }

    public void sendEmail(String toEmail,
                          String templateName,
                          String mailSubject,
                          Map<String, Object> data) {
        MessagePublisher publisher = sqsPublisherProvider.getIfAvailable();
        if (publisher == null) {
            log.warn("SQS publisher is unavailable; skipping notification to {}", toEmail);
            return;
        }
        if (isBlank(notificationQueue)) {
            log.warn("Notification queue is not configured; skipping notification to {}", toEmail);
            return;
        }

        NotificationEventDto event = new NotificationEventDto();
        event.setMessageId(UUID.randomUUID().toString());
        event.setTraceId(resolveTraceId());
        event.setNotificationType(NotificationType.MAIL);
        event.setTo(toEmail);
        event.setSubject(mailSubject);
        event.setTemplatePath(templateName);
        event.setVariables(data);

        try {
            publisher.send(notificationQueue, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize notification event", ex);
        }
    }

    private void notifyUsersByRoles(List<String> roleNames,
                                    String templateName,
                                    String subject,
                                    ModelBuilder modelBuilder) {
        Set<User> recipients = resolveRecipientsByRoles(roleNames);
        for (User recipient : recipients) {
            if (recipient == null || isBlank(recipient.getEmail())) {
                log.warn("Skipping email for invalid user record: {}", recipient);
                continue;
            }
            try {
                sendEmail(recipient.getEmail(), templateName, subject, modelBuilder.build(recipient));
            } catch (Exception ex) {
                String roleName = recipient.getRole() != null ? recipient.getRole().getName() : "UNKNOWN";
                log.error("Failed to send notification to userEmail={} role={}", recipient.getEmail(), roleName, ex);
            }
        }
    }

    private Set<User> resolveRecipientsByRoles(List<String> roleNames) {
        Set<User> users = new LinkedHashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            return users;
        }
        for (String roleName : roleNames) {
            if (isBlank(roleName)) {
                continue;
            }
            users.addAll(userRepository.findByRoleCodeAndActiveTrue(roleName.trim()));
        }
        return users;
    }

    private String resolveCategory(String shipmentTypeCode) {
        if (isBlank(shipmentTypeCode)) {
            return "N/A";
        }
        return categoryCodesGlobalDataCacheService.getCategoryCodeByCode(shipmentTypeCode).getLabel();
    }

    private String resolveTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC);
        if (!isBlank(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String safeName(User user) {
        if (user == null) {
            return "User";
        }
        return !isBlank(user.getUsername()) ? user.getUsername() : user.getEmail();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface ModelBuilder {
        Map<String, Object> build(User user);
    }
}