package com.customcharge.persistencelayer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEventDto {

    private String messageId;
    private String traceId;
    private NotificationType notificationType;

    private String from;
    private String to;
    private List<String> cc;
    private String subject;
    private String templatePath;
    private Map<String, Object> variables;

    private String phoneNumber;
}
