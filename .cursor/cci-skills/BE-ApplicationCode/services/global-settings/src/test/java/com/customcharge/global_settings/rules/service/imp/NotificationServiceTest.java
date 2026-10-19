package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.NotificationEventDto;
import com.customcharge.persistencelayer.dto.NotificationType;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String PERFORMED_BY = "tester@aramex.com";
    private static final String TARGET_FIELD = "originCountry";
    private static final String QUEUE = "custom-charge-notification";

    @Mock
    private ObjectProvider<MessagePublisher> sqsPublisherProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @Mock
    private MessagePublisher messagePublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                sqsPublisherProvider, objectMapper, userRepository, categoryCodesGlobalDataCacheService);
        ReflectionTestUtils.setField(notificationService, "createRuleUiLink", "https://rules/");
        ReflectionTestUtils.setField(notificationService, "notificationQueue", QUEUE);
    }

    @AfterEach
    void tearDown() {
        org.slf4j.MDC.remove("traceId");
    }

    private String captureSentPayload() {
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).send(queueCaptor.capture(), payloadCaptor.capture());
        assertThat(queueCaptor.getValue()).isEqualTo(QUEUE);
        return payloadCaptor.getValue();
    }

    @Test
    void sendEmail_shouldSkip_whenSqsPublisherUnavailable() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(null);

        notificationService.sendEmail("user@test.com", "template.ftl", "Subject", java.util.Map.of());

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendCreateOrUpdateNotification_shouldPublish_whenRecipientsExist() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));

        notificationService.sendCreateOrUpdateNotification(
                List.of("ADMIN"), "CREATED", "DDU", UUID.randomUUID().toString(), PERFORMED_BY, TARGET_FIELD);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getNotificationType()).isEqualTo(NotificationType.MAIL);
        assertThat(event.getTo()).isEqualTo("admin@test.com");
        assertThat(event.getTemplatePath()).isEqualTo("create-update-notification.ftl");
        assertThat(event.getSubject()).isEqualTo("Global Setting Created");
    }

    @Test
    void sendStatusChangeNotification_shouldUseDeactivatedSubject_whenInactive() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));

        notificationService.sendStatusChangeNotification(
                List.of("ADMIN"), "ACTIVE", "INACTIVE", "DDU", UUID.randomUUID().toString(), PERFORMED_BY);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getSubject()).isEqualTo("Global Setting Deactivated");
        assertThat(event.getTemplatePath()).isEqualTo("status-change-notification.ftl");
    }

    @Test
    void sendCreateOrUpdateNotification_shouldNotFail_whenRoleListEmpty() {
        notificationService.sendCreateOrUpdateNotification(List.of(), "CREATED", "DDU", "id", PERFORMED_BY, TARGET_FIELD);
        verify(sqsPublisherProvider, never()).getIfAvailable();
    }

    @Test
    void sendCreateOrUpdateNotification_shouldUseUpdateSubject_whenUpdated() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));

        notificationService.sendCreateOrUpdateNotification(
                List.of("ADMIN"), "UPDATED", "DDU", UUID.randomUUID().toString(), PERFORMED_BY, TARGET_FIELD);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getSubject()).isEqualTo("Global Setting Updated");
    }

    @Test
    void sendCreateOrUpdateNotification_shouldSkipBlankEmail() {
        User user = GlobalSettingsTestDataFactory.user("  ", "Admin");
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));

        notificationService.sendCreateOrUpdateNotification(List.of("ADMIN"), "CREATED", "DDU", "id", PERFORMED_BY, TARGET_FIELD);

        verify(sqsPublisherProvider, never()).getIfAvailable();
        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendCreateOrUpdateNotification_shouldUseNaCategory_whenShipmentTypeBlank() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));

        notificationService.sendCreateOrUpdateNotification(List.of("ADMIN"), "CREATED", " ", "id", PERFORMED_BY, TARGET_FIELD);

        verify(messagePublisher).send(any(String.class), any(String.class));
        verify(categoryCodesGlobalDataCacheService, never()).getCategoryCodeByCode(any());
    }

    @Test
    void sendStatusChangeNotification_shouldUseActivatedSubject_whenActive() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));

        notificationService.sendStatusChangeNotification(
                List.of("ADMIN"), "INACTIVE", "ACTIVE", "DDU", UUID.randomUUID().toString(), PERFORMED_BY);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getSubject()).isEqualTo("Global Setting Activated");
    }

    @Test
    void sendEmail_shouldPublish_whenPublisherAvailable() throws Exception {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);

        notificationService.sendEmail("user@test.com", "template.ftl", "Subject", java.util.Map.of("k", "v"));

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getTo()).isEqualTo("user@test.com");
        assertThat(event.getSubject()).isEqualTo("Subject");
        assertThat(event.getTemplatePath()).isEqualTo("template.ftl");
        assertThat(event.getVariables()).containsEntry("k", "v");
    }

    @Test
    void sendCreateOrUpdateNotification_shouldContinue_whenSingleRecipientFails() {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU Label"));
        doThrow(new RuntimeException("sqs down")).when(messagePublisher).send(any(), any());

        notificationService.sendCreateOrUpdateNotification(List.of("ADMIN"), "CREATED", "DDU", "id", PERFORMED_BY, TARGET_FIELD);

        verify(messagePublisher).send(any(String.class), any(String.class));
    }

    @Test
    void sendCreateOrUpdateNotification_shouldSkipBlankRoleName() {
        notificationService.sendCreateOrUpdateNotification(List.of(" "), "CREATED", "DDU", "id", PERFORMED_BY, TARGET_FIELD);
        verify(userRepository, never()).findByRoleCodeAndActiveTrue(any());
    }

    @Test
    void sendEmail_shouldSkip_whenNotificationQueueBlank() {
        ReflectionTestUtils.setField(notificationService, "notificationQueue", " ");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);

        notificationService.sendEmail("user@test.com", "template.ftl", "Subject", java.util.Map.of());

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendEmail_shouldThrow_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = spy(objectMapper);
        notificationService = new NotificationService(
                sqsPublisherProvider, failingMapper, userRepository, categoryCodesGlobalDataCacheService);
        ReflectionTestUtils.setField(notificationService, "notificationQueue", QUEUE);
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        org.mockito.Mockito.doThrow(new JsonProcessingException("serialize failed") { })
                .when(failingMapper).writeValueAsString(any());
        java.util.Map<String, Object> variables = java.util.Map.of();

        assertThatThrownBy(() -> notificationService.sendEmail(
                "user@test.com", "template.ftl", "Subject", variables))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }

    @Test
    void sendCreateOrUpdateNotification_shouldUseCategoryRuleType_forNonDduDdp() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("HV"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("HV", "HV", "High Value"));

        notificationService.sendCreateOrUpdateNotification(
                List.of("ADMIN"), "CREATED", "HV", "id", PERFORMED_BY, TARGET_FIELD);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getVariables()).containsEntry("ruleType", "Category");
        assertThat(event.getVariables()).containsEntry("actionSummary", "Mark Shipment originCountry");
    }

    @Test
    void sendStatusChangeNotification_shouldUseDdpRuleType() throws Exception {
        User user = GlobalSettingsTestDataFactory.user("admin@test.com", "Admin");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(userRepository.findByRoleCodeAndActiveTrue("ADMIN")).thenReturn(List.of(user));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDP"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDP", "DDP", "DDP"));

        notificationService.sendStatusChangeNotification(
                List.of("ADMIN"), "INACTIVE", "ACTIVE", "DDP", "id", PERFORMED_BY);

        NotificationEventDto event = objectMapper.readValue(captureSentPayload(), NotificationEventDto.class);
        assertThat(event.getVariables()).containsEntry("ruleType", "DDP");
        assertThat(event.getVariables()).containsEntry("action", "ACTIVATED");
    }

    @Test
    void sendCreateOrUpdateNotification_shouldContinue_whenResolveCategoryFails() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenThrow(new RuntimeException("cache down"));

        notificationService.sendCreateOrUpdateNotification(
                List.of("ADMIN"), "CREATED", "DDU", "id", PERFORMED_BY, TARGET_FIELD);

        verify(sqsPublisherProvider, never()).getIfAvailable();
    }
}
