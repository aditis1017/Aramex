package com.customcharge.global_settings.rules.dto;

import lombok.Data;

@Data
public class MessageResponse {
    private String message;
    public MessageResponse(String message){
        this.message=message;
    }
}
