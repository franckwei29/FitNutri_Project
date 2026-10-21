package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.CurrentUserService;
import com.fitnutri.backend.records.MealType;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/food")
public class FoodEntryReadController {
 private final FoodEntryReadService reads;private final CurrentUserService current;
 public FoodEntryReadController(FoodEntryReadService reads,CurrentUserService current){this.reads=reads;this.current=current;}
 @GetMapping("/entries") public ResponseEntity<FoodSnapshotReader.Page> list(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) MealType meal,@RequestParam(required=false) String zone,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reads.listRoots(current.requireGeneralUser(auth).getId(),from,to,meal,zone,page,size));}
 @GetMapping("/entries/{kind}/{id}") public ResponseEntity<FoodSnapshotReader.Root> detail(@PathVariable String kind,@PathVariable Long id,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reads.getOwnedRoot(current.requireGeneralUser(auth).getId(),kind,id));}
}
