package com.customcharge.persistencelayer.context;

import java.time.ZoneId;

public final class TimeZoneContext {

    public static final String HEADER_NAME = "X-Time-Zone";
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private static final ThreadLocal<ZoneId> HOLDER = new ThreadLocal<>();

    private TimeZoneContext() {
    }

    public static void set(ZoneId zoneId) {
        HOLDER.set(zoneId);
    }

    public static ZoneId get() {
        return HOLDER.get();
    }

    public static ZoneId getOrDefault(ZoneId defaultZone) {
        ZoneId zone = HOLDER.get();
        defaultZone= (defaultZone != null ? defaultZone : DEFAULT_ZONE);
        return zone != null ? zone : defaultZone;
    }

    public static ZoneId getOrDefault() {
        return getOrDefault(DEFAULT_ZONE);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
