package com.customcharge.persistencelayer.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilsTest {

    @Test
    void convert_shouldDeserializeJsonObject() {
        Map<?, ?> map = JsonUtils.convert("{\"name\":\"test\"}", Map.class);
        assertThat(map.get("name")).isEqualTo("test");
    }

    @Test
    void getObjectMapper_shouldSerializeLocalDateTime() throws Exception {
        String json = JsonUtils.getObjectMapper().writeValueAsString(
                Map.of("ts", LocalDateTime.of(2024, 6, 1, 12, 0, 0)));
        assertThat(json).contains("2024-06-01");
    }

    @Test
    void convertToClass_shouldDeserializeOrCast() {
        Map<?, ?> parsed = JsonUtils.convertToClass("{\"key\":\"value\"}", Map.class);
        assertThat(parsed.get("key")).isEqualTo("value");

        String cast = JsonUtils.convertToClass("plain-text", String.class);
        assertThat(cast).isEqualTo("plain-text");
    }

    @Test
    void convert_shouldDeserializeJsonArray() {
        @SuppressWarnings("unchecked")
        List<Integer> list = JsonUtils.convert("[1,2,3]", List.class);
        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    void getObjectMapper_shouldDeserializeLocalDateTimeVariants() throws Exception {
        LocalDateTime withMillis = JsonUtils.getObjectMapper().readValue(
                "\"2024-06-01T12:00:00.000Z\"", LocalDateTime.class);
        assertThat(withMillis).isEqualTo(LocalDateTime.of(2024, 6, 1, 12, 0));

        LocalDateTime withoutMillis = JsonUtils.getObjectMapper().readValue(
                "\"2024-06-01T12:00:00\"", LocalDateTime.class);
        assertThat(withoutMillis).isEqualTo(LocalDateTime.of(2024, 6, 1, 12, 0));
    }

    @Test
    void buildObjectMapper_shouldReturnConfiguredMapper() {
        assertThat(JsonUtils.buildObjectMapper()).isNotNull();
    }

    @Test
    void convertToClass_shouldCast_whenDeserializationFails() {
        String value = JsonUtils.convertToClass("{invalid-json", String.class);
        assertThat(value).isEqualTo("{invalid-json");
    }

    @Test
    void convert_shouldFallbackToConvertValue() {
        assertThat(JsonUtils.convert("plain", String.class)).isEqualTo("plain");
    }

    @Test
    void getObjectMapper_shouldReturnNullForUnparseableLocalDateTime() throws Exception {
        LocalDateTime result = JsonUtils.getObjectMapper().readValue("\"not-a-date\"", LocalDateTime.class);
        assertThat(result).isNull();
    }
}
