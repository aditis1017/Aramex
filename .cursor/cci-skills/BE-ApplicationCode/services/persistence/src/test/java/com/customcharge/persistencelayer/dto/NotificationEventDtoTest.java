package com.customcharge.persistencelayer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesFieldsCompatibleWithNotificationConsumer() throws Exception {
        NotificationEventDto dto = new NotificationEventDto();
        dto.setMessageId("msg-1");
        dto.setTraceId("trace-1");
        dto.setNotificationType(NotificationType.MAIL);
        dto.setTo("user@example.com");
        dto.setSubject("Subject");
        dto.setTemplatePath("create-update-notification.ftl");
        dto.setVariables(Map.of("userName", "Admin"));

        String json = objectMapper.writeValueAsString(dto);
        NotificationEventDto roundTrip = objectMapper.readValue(json, NotificationEventDto.class);

        assertThat(roundTrip.getMessageId()).isEqualTo("msg-1");
        assertThat(roundTrip.getTraceId()).isEqualTo("trace-1");
        assertThat(roundTrip.getNotificationType()).isEqualTo(NotificationType.MAIL);
        assertThat(roundTrip.getTo()).isEqualTo("user@example.com");
        assertThat(roundTrip.getSubject()).isEqualTo("Subject");
        assertThat(roundTrip.getTemplatePath()).isEqualTo("create-update-notification.ftl");
        assertThat(roundTrip.getVariables()).containsEntry("userName", "Admin");
        assertThat(json).contains("\"notificationType\":\"MAIL\"");
    }
}
