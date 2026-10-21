package com.fitnutri.backend.activity;
import com.fitnutri.backend.records.ExerciseCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records/exercise")
public class ActivityReferenceController {
    private final ExerciseCatalog catalogue;
    public ActivityReferenceController(ExerciseCatalog catalogue){this.catalogue=catalogue;}
    @GetMapping("/types") public List<ExerciseTypeResponse> types(){return catalogue.listActiveTypes();}
}
