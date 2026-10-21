package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.summary.SummaryService;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class QaContextBuilderTest {
 final SummaryService summaries=mock(SummaryService.class);final QaContextBuilder context=new QaContextBuilder(summaries,new RecordDatePolicy(Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"),ZoneOffset.UTC),new ZoneResolver("Australia/Sydney")));
 @Test void absentRecordRangeNeverLoadsRecords(){assertNull(context.buildAllowedFacts(41L,null,"Australia/Sydney"));verifyNoInteractions(summaries);}
 @Test void invalidOverlongFutureAndZoneContextsAreRejectedBeforeAnySummaryRead(){for(var range:new QaRequest.RecordRange[]{new QaRequest.RecordRange(null,LocalDate.of(2026,1,1)),new QaRequest.RecordRange(LocalDate.of(2026,1,1),null),new QaRequest.RecordRange(LocalDate.of(2026,1,1),LocalDate.of(2026,2,1)),new QaRequest.RecordRange(LocalDate.of(2026,10,17),LocalDate.of(2026,10,17)),new QaRequest.RecordRange(LocalDate.of(2026,1,2),LocalDate.of(2026,1,1))})assertThrows(BusinessException.class,()->context.buildAllowedFacts(41L,range,"Australia/Sydney"));assertThrows(BusinessException.class,()->context.buildAllowedFacts(41L,null,"Not/AZone"));verifyNoInteractions(summaries);}
}
