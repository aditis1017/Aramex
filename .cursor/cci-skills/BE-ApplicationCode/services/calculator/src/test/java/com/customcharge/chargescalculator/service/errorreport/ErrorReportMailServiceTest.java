package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.dto.NotificationEventDto;
import com.customcharge.persistencelayer.dto.NotificationType;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportMailServiceTest {

    private static final String QUEUE = "custom-charge-notification";

    @Mock
    private ObjectProvider<MessagePublisher> sqsPublisherProvider;
    @Mock
    private StagedFileUploadRepository stagedFileUploadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessagePublisher messagePublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ErrorReportMailService service;

    @BeforeEach
    void setUp() {
        service = new ErrorReportMailService(
                sqsPublisherProvider, objectMapper, stagedFileUploadRepository, userRepository);
        ReflectionTestUtils.setField(service, "notificationQueue", QUEUE);
    }

    @AfterEach
    void tearDown() {
        MDC.remove("traceId");
    }

    @Test
    void sendReportMail_shouldSkip_whenSqsPublisherUnavailable() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(null);

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(stagedFileUploadRepository, never()).findByFileId(any());
    }

    @Test
    void sendReportMail_shouldSkip_whenUploaderMissing() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString())).thenReturn(Optional.empty());

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendReportMail_shouldSkip_whenCreatedBySystem() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload("SYSTEM")));

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendReportMail_shouldSkip_whenNotificationQueueBlank() {
        ReflectionTestUtils.setField(service, "notificationQueue", " ");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(stagedFileUploadRepository, never()).findByFileId(any());
        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendReportMail_shouldSkip_whenCreatedByBlank() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload(" ")));

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendReportMail_shouldUseMdcTraceId_whenPresent() throws Exception {
        MDC.put("traceId", "trace-123");
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload("user@example.com")));
        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com"))
                .thenReturn(Optional.empty());

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).send(eq(QUEUE), payloadCaptor.capture());
        NotificationEventDto event = objectMapper.readValue(payloadCaptor.getValue(), NotificationEventDto.class);
        assertThat(event.getTraceId()).isEqualTo("trace-123");
        assertThat(event.getVariables()).containsEntry("userName", "user@example.com");
    }

    @Test
    void sendReportMail_shouldContinue_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = spy(objectMapper);
        service = new ErrorReportMailService(
                sqsPublisherProvider, failingMapper, stagedFileUploadRepository, userRepository);
        ReflectionTestUtils.setField(service, "notificationQueue", QUEUE);
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload("user@example.com")));
        doThrow(new JsonProcessingException("serialize failed") { })
                .when(failingMapper).writeValueAsString(any());

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(messagePublisher, never()).send(any(), any());
    }

    @Test
    void sendReportMail_shouldContinue_whenPublishFails() {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload("user@example.com")));
        doThrow(new RuntimeException("sqs down")).when(messagePublisher).send(any(), any());

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        verify(messagePublisher).send(eq(QUEUE), any());
    }

    @Test
    void sendReportMail_shouldPublishNotification_whenUploaderPresent() throws Exception {
        when(sqsPublisherProvider.getIfAvailable()).thenReturn(messagePublisher);
        when(stagedFileUploadRepository.findByFileId(CalculatorTestDataFactory.fileId().toString()))
                .thenReturn(Optional.of(CalculatorTestDataFactory.stagedFileUpload("user@example.com")));
        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.user("user@example.com", "User One")));

        service.sendReportMail(CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).send(eq(QUEUE), payloadCaptor.capture());
        NotificationEventDto event = objectMapper.readValue(payloadCaptor.getValue(), NotificationEventDto.class);
        assertThat(event.getNotificationType()).isEqualTo(NotificationType.MAIL);
        assertThat(event.getTo()).isEqualTo("user@example.com");
        assertThat(event.getSubject()).contains("Custom Charge Error Report");
        assertThat(event.getTemplatePath()).isEqualTo("error-report-download.ftl");
        assertThat(event.getVariables()).containsEntry("downloadUrl", "http://download");
    }
}
