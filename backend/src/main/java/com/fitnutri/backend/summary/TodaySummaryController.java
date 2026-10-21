package com.fitnutri.backend.summary;
import com.fitnutri.backend.common.CurrentUserService;
import com.fitnutri.backend.summary.SummaryModels.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/summary")
public class TodaySummaryController {
    private final SummaryService summaries;private final CurrentUserService current;
    public TodaySummaryController(SummaryService summaries,CurrentUserService current){this.summaries=summaries;this.current=current;}
    @GetMapping("/today") public DaySummaryResponse today(@RequestParam(required=false)String zone,Authentication auth){return summaries.today(current.requireGeneralUser(auth).getId(),zone);}
    @GetMapping("/trends") public PeriodSummary trends(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Integer days,@RequestParam(required=false)String zone,Authentication auth){return summaries.trends(current.requireGeneralUser(auth).getId(),from,to,days,zone);}
}
