package com.fitnutri.backend.common;

import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ZoneResolver {
    public static final String DEFAULT_ZONE="Australia/Sydney";
    private final ZoneId defaultZone;
    public ZoneResolver(@Value("${app.default-zone:Australia/Sydney}") String defaultZone) {
        this.defaultZone=ZoneId.of(defaultZone);
    }
    public ZoneId resolve(String zone) {
        if (zone==null || zone.isBlank()) return defaultZone;
        if (!ZoneId.getAvailableZoneIds().contains(zone)) throw BusinessException.invalid("zone","Choose a supported IANA time zone.");
        return ZoneId.of(zone);
    }
}
