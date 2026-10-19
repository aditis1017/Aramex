package com.customcharge.persistencelayer.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.customcharge.persistencelayer.context.TimeZoneContext;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InstantTimeZoneSerializer extends JsonSerializer<Instant> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        java.time.ZoneId zone = TimeZoneContext.getOrDefault();
        ZonedDateTime zoned = value.atZone(zone);
        gen.writeString(zoned.format(FORMATTER));
    }
}
