package com.customcharge.persistencelayer.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void utcToLocal_shouldConvertInstant() {
        Instant instant = Instant.parse("2024-01-15T10:00:00Z");
        LocalDateTime local = TimeUtil.utcToLocal(instant, ZoneId.of("UTC"));
        assertThat(local).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
    }

    @Test
    void parseZoneIdOrUtc_shouldFallbackForInvalidZone() {
        assertThat(TimeUtil.parseZoneIdOrUtc("Invalid/Zone")).isEqualTo(ZoneId.of("UTC"));
        assertThat(TimeUtil.parseZoneIdOrUtc("Asia/Dubai").getId()).isEqualTo("Asia/Dubai");
    }

    @Test
    void formatInZone_shouldReturnNull_whenInputMissing() {
        assertThat(TimeUtil.formatInZone(null, ZoneId.of("UTC"))).isNull();
    }

    @Test
    void utcToZoned_shouldConvertInstant() {
        Instant instant = Instant.parse("2024-01-15T10:00:00Z");
        assertThat(TimeUtil.utcToZoned(instant, ZoneId.of("UTC")).toInstant()).isEqualTo(instant);
    }

    @Test
    void utcToLocal_shouldReturnNull_whenArgumentsMissing() {
        assertThat(TimeUtil.utcToLocal(null, ZoneId.of("UTC"))).isNull();
        assertThat(TimeUtil.utcToLocal(Instant.now(), null)).isNull();
    }

    @Test
    void formatInZone_shouldFormatInstant() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        assertThat(TimeUtil.formatInZone(instant, ZoneId.of("UTC"))).isEqualTo("2024-01-15T10:30:00");
    }

    @Test
    void parseZoneIdOrUtc_shouldDefaultForBlankZone() {
        assertThat(TimeUtil.parseZoneIdOrUtc(null).getId()).isEqualTo("UTC");
        assertThat(TimeUtil.parseZoneIdOrUtc("  ").getId()).isEqualTo("UTC");
    }
}
