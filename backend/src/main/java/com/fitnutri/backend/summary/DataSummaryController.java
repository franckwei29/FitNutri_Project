package com.fitnutri.backend.summary;
import com.fitnutri.backend.common.CurrentUserService;
import com.fitnutri.backend.summary.SummaryModels.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/summary")
public class DataSummaryController {
    private final SummaryService summaries;private final CurrentUserService current;
    public DataSummaryController(SummaryService summaries,CurrentUserService current){this.summaries=summaries;this.current=current;}
    @GetMapping("/daily") public DaySummaryResponse daily(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date,@RequestParam(required=false)String zone,Authentication auth){return summaries.daily(current.requireGeneralUser(auth).getId(),date,zone);}
    @GetMapping("/weekly") public WeeklySummaryResponse weekly(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate end,@RequestParam(required=false)String zone,Authentication auth){return summaries.weekly(current.requireGeneralUser(auth).getId(),end,zone);}
}
