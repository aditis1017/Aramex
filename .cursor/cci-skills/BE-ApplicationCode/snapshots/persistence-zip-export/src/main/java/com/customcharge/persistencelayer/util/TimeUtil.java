package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.context.TimeZoneContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TimeUtil() {
    }

    public static LocalDateTime utcToLocal(Instant utc, ZoneId zone) {
        if (utc == null || zone == null) return null;
        return LocalDateTime.ofInstant(utc, zone);
    }

    public static ZonedDateTime utcToZoned(Instant utc, ZoneId zone) {
        if (utc == null || zone == null) return null;
        return ZonedDateTime.ofInstant(utc, zone);
    }

    public static String formatInZone(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) return null;
        return ISO_LOCAL.format(instant.atZone(zone));
    }

    public static ZoneId parseZoneIdOrUtc(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return TimeZoneContext.DEFAULT_ZONE;
        try {
            return ZoneId.of(zoneId.trim());
        } catch (Exception e) {
            return TimeZoneContext.DEFAULT_ZONE;
        }
    }
}
