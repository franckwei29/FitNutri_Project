package com.fitnutri.backend.speech;

import com.fitnutri.backend.common.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpeechEvidenceTest {
    final SpeechEvidence evidence=new SpeechEvidence(new RecordDatePolicy(Clock.fixed(Instant.parse("2026-10-15T20:00:00Z"),ZoneOffset.UTC),new ZoneResolver("Australia/Sydney")));
    final LocalDate today=LocalDate.of(2026,10,16);
    @Test void dateIsServerDerivedIncludingRelativeExplicitAndDefaulted(){assertEquals(new SpeechEvidence.DateResult(today.minusDays(1),false),evidence.resolveRelativeDate("Yesterday I ate rice",null,today,"Australia/Sydney"));assertEquals(new SpeechEvidence.DateResult(LocalDate.of(2020,1,2),false),evidence.resolveRelativeDate("2020-01-02 rice","2020-01-02",today,"Australia/Sydney"));assertEquals(new SpeechEvidence.DateResult(today,true),evidence.resolveRelativeDate("Rice",null,today,"Australia/Sydney"));assertEquals(today.minusDays(1),evidence.resolveRelativeDate("昨天吃鸡肉","昨天",today,"Australia/Sydney").recordDate());}
    @Test void staleClientBaseFutureImpossibleMultipleDatesAndInventedEvidenceAreRejected(){for(Runnable action:new Runnable[]{()->evidence.resolveRelativeDate("rice",null,today.minusDays(1),"Australia/Sydney"),()->evidence.resolveRelativeDate("tomorrow rice",null,today,"Australia/Sydney"),()->evidence.resolveRelativeDate("2026-02-30 rice","2026-02-30",today,"Australia/Sydney"),()->evidence.resolveRelativeDate("yesterday and today",null,today,"Australia/Sydney"),()->evidence.resolveRelativeDate("rice","2020-01-02",today,"Australia/Sydney")})assertThrows(BusinessException.class,action::run);}
    @Test void onlyExplicitGramEvidenceBecomesGramsAndCupsBowlsRemainMissing(){assertEquals(new BigDecimal("150.125"),evidence.grams("150.125 grams"));assertEquals(new BigDecimal("100"),evidence.grams("100g"));for(String value:new String[]{"2 bowls","one cup","150 milligrams","0g","-20g","10001g","0.0001g"})assertNull(evidence.grams(value));}
    @Test void minutesUseLiteralUnitsAndHalfAnHourWithoutFabricatedDefaults(){assertEquals(new BigDecimal("30"),evidence.minutes("half an hour"));assertEquals(new BigDecimal("30"),evidence.minutes("半小时"));assertEquals(new BigDecimal("90"),evidence.minutes("1 hour 30 minutes"));assertEquals(new BigDecimal("12.5"),evidence.minutes("12.5 min"));for(String value:new String[]{"a while","0 minutes","-1 minutes","1441 minutes","0.001 minutes"})assertNull(evidence.minutes(value));}
}
