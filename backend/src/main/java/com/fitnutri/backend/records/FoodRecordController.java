package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records/food")
public class FoodRecordController {
    private final RecordReadService records;private final CurrentUserService current;
    public FoodRecordController(RecordReadService records,CurrentUserService current){this.records=records;this.current=current;}
    @GetMapping public List<FoodRecordResponse> list(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)MealType mealType,@RequestParam(required=false)String zone,Authentication auth){return records.listFoods(current.requireGeneralUser(auth).getId(),from,to,mealType,zone);}
    @GetMapping("/{id}") public FoodRecordResponse detail(@PathVariable Long id,Authentication auth){return records.getFood(current.requireGeneralUser(auth).getId(),id);}
}
