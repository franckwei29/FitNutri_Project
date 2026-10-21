package com.fitnutri.backend.activity.catalogue;
import com.fitnutri.backend.common.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/exercise")
public class ExerciseOptionController {
 private final ExerciseOptionService options;private final MovementCatalogueService movements;private final CurrentUserService current;
 public ExerciseOptionController(ExerciseOptionService options,MovementCatalogueService movements,CurrentUserService current){this.options=options;this.movements=movements;this.current=current;}
 @GetMapping("/options") public ResponseEntity<ExerciseCataloguePage<ExerciseOptionService.Response>> options(@RequestParam(required=false) String category,@RequestParam(required=false) String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam Map<String,String> parameters,Authentication auth){current.requireGeneralUser(auth);requireFields(parameters,Set.of("category","query","page","size"));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(options.search(category,query,page,size));}
 @GetMapping("/movements") public ResponseEntity<ExerciseCataloguePage<MovementCatalogueService.Response>> movements(@RequestParam(required=false) String bodyRegion,@RequestParam(required=false) String equipment,@RequestParam(required=false) String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam Map<String,String> parameters,Authentication auth){current.requireGeneralUser(auth);requireFields(parameters,Set.of("bodyRegion","equipment","query","page","size"));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(movements.search(bodyRegion,equipment,query,page,size));}
 private static void requireFields(Map<String,String> parameters,Set<String> fields){if(!fields.containsAll(parameters.keySet()))throw BusinessException.invalid("query","Use only the supported filters for this directory.");}
}
