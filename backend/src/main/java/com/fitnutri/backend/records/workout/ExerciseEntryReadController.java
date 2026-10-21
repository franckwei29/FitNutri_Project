package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.CurrentUserService;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/exercise")
public class ExerciseEntryReadController{
 private final ExerciseEntryReadService reads;private final ExerciseRecentService recent;private final CurrentUserService current;
 public ExerciseEntryReadController(ExerciseEntryReadService reads,ExerciseRecentService recent,CurrentUserService current){this.reads=reads;this.recent=recent;this.current=current;}
 @GetMapping("/entries") public ResponseEntity<ExerciseEntryReadService.Page> list(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false)String zone,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reads.listRoots(current.requireGeneralUser(auth).getId(),from,to,zone,page,size));}
 @GetMapping("/entries/{kind}/{id}") public ResponseEntity<ExerciseEntryReadService.Root> detail(@PathVariable String kind,@PathVariable long id,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reads.getOwnedRoot(current.requireGeneralUser(auth).getId(),kind,id));}
 @GetMapping("/recent") public ResponseEntity<ExerciseEntryReadService.Page> recent(@RequestParam(defaultValue="5")int limit,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recent.recentEntries(current.requireGeneralUser(auth).getId(),limit));}
}
