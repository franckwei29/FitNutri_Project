package com.fitnutri.backend.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class RecordDatePolicy {
    private final Clock clock;
    private final ZoneResolver zones;
    public RecordDatePolicy(Clock clock, ZoneResolver zones) { this.clock=clock; this.zones=zones; }
    public LocalDate today(String zone) { return LocalDate.now(clock.withZone(zones.resolve(zone))); }
    public LocalDate requireDate(LocalDate date, String zone) {
        LocalDate today=today(zone);
        if (date==null || date.isAfter(today)) throw BusinessException.invalid("recordDate","Enter a date that is not in the future.");
        return date;
    }
    public DateRange requireRange(LocalDate from, LocalDate to, String zone) {
        requireDate(from,zone); requireDate(to,zone);
        long days=ChronoUnit.DAYS.between(from,to)+1;
        if (days<1 || days>366) throw BusinessException.invalid("from","Choose an inclusive range of 1 to 366 days.");
        return new DateRange(from,to,(int)days);
    }
    public record DateRange(LocalDate from, LocalDate to, int dayCount) {}
}
