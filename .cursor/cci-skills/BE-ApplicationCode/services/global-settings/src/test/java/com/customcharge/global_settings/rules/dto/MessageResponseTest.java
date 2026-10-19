package com.customcharge.global_settings.rules.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageResponseTest {

    @Test
    void shouldExposeMessageViaConstructorAndSetter() {
        MessageResponse response = new MessageResponse("Status Updated");

        assertThat(response.getMessage()).isEqualTo("Status Updated");

        response.setMessage("Sequence updated");
        assertThat(response.getMessage()).isEqualTo("Sequence updated");
    }
}
