package com.fitnutri.backend.summary;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.summary.SummaryModels.*;
import com.fitnutri.backend.user.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SummaryControllerTest {
    private final SummaryService service=mock(SummaryService.class);private final CurrentUserService current=mock(CurrentUserService.class);private final LocalDate date=LocalDate.of(2020,1,2);
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("synthetic@example.test","",List.of());
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new TodaySummaryController(service,current),new DataSummaryController(service,current)).setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void dailyUsesCanonicalDayShapeAndServerOwner() throws Exception {var user=new User();user.setId(41L);when(current.requireGeneralUser(auth)).thenReturn(user);var empty=MetricAggregate.empty();var day=new DayAggregate(date,empty,empty,empty,empty,empty,null,0,0,0,0);when(service.daily(41L,date,"UTC")).thenReturn(new DaySummaryResponse(day,null,List.of()));mvc.perform(get("/api/summary/daily").principal(auth).param("date","2020-01-02").param("zone","UTC")).andExpect(status().isOk()).andExpect(jsonPath("$.day.recordDate").value("2020-01-02")).andExpect(jsonPath("$.day.intake.hasData").value(false)).andExpect(jsonPath("$.day.intake.knownSum").isEmpty()).andExpect(jsonPath("$.caloriesRemaining").doesNotExist());verify(service).daily(41L,date,"UTC");}
    @Test void requiredDailyDateAndWeeklyEndAre400BeforeBusinessService() throws Exception {mvc.perform(get("/api/summary/daily")).andExpect(status().isBadRequest());mvc.perform(get("/api/summary/weekly").param("from","2020-01-01")).andExpect(status().isBadRequest());verifyNoInteractions(service,current);}
    @Test void summaryFailureIsStructuredAndNeverReturnsFakeEmptyData() throws Exception {var user=new User();user.setId(41L);when(current.requireGeneralUser(auth)).thenReturn(user);when(service.today(41L,null)).thenThrow(new IllegalStateException("Synthetic sensitive database detail"));mvc.perform(get("/api/summary/today").principal(auth)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andExpect(jsonPath("$.message").value("The request could not be completed.")).andExpect(jsonPath("$.day").doesNotExist());}
}
