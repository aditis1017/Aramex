package com.customcharge.persistencelayer.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.SneakyThrows;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private JsonUtils(){}

    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    public static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        objectMapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        final SimpleModule localDateTimeSerialization = new SimpleModule();
        localDateTimeSerialization.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        localDateTimeSerialization.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        objectMapper.registerModule(localDateTimeSerialization);
        return objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static <T> T convertToClass(String response, Class<T> returnClass) {
        try {
            return convert(response, returnClass);
        } catch (Exception e) {
            return returnClass.cast(response);
        }
    }

    @SneakyThrows
    public static  <T> T convert(String string, Class<T> toObjectOf) {
        try {
            return JsonUtils.getObjectMapper().readValue(string, toObjectOf);
        } catch (JsonProcessingException e) {
            try {
                return JsonUtils.getObjectMapper().convertValue(string, toObjectOf);
            } catch (Exception ex) {
                return JsonUtils.getObjectMapper().convertValue(getFromValue(string),toObjectOf);
            }
        }

    }

    @SneakyThrows
    private static Object getFromValue(String string) {
        try {
            return JsonUtils.getObjectMapper().readValue(string, Map.class);
        } catch (JsonProcessingException e) {
            return JsonUtils.getObjectMapper().readValue(string, List.class);
        }
    }
}

class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.format(format));
    }
}

class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext context) throws IOException {
        try {
            return parseLocalDateTime(p);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(JsonParser p) throws IOException {
        try {
            return LocalDateTime.parse(p.getValueAsString(), fmt);
        } catch (Exception e) {
            return LocalDateTime.parse(p.getValueAsString(), fmt2);
        }
    }
}