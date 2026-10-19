package com.customcharge.dashboard.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies cached value types deserialize with the same {@link ObjectMapper} settings as
 * {@link DashboardSpringCacheConfiguration}.
 */
class DashboardSpringCacheSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.customcharge.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("org.springframework.data.")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
    }

    @Test
    void roundTrip_userAuthorityCodes() throws Exception {
        List<String> codes = List.of("GLOBAL_SETTINGS_VIEW", "CUSTOMS_EDIT");
        String json = mapper.writeValueAsString(codes);
        List<String> restored = mapper.readValue(json, List.class);
        assertThat(restored).containsExactlyElementsOf(codes);
    }

    @Test
    void roundTrip_routeAccessCookieString() throws Exception {
        String encoded = "eyJyZWFkeSI6dHJ1ZX0";
        String json = mapper.writeValueAsString(encoded);
        String restored = mapper.readValue(json, String.class);
        assertThat(restored).isEqualTo(encoded);
    }
}
