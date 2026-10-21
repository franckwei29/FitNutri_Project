package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.CurrentUserService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/food")
public class FoodRecentController {
 private final FoodRecentService recent;private final CurrentUserService current;
 public FoodRecentController(FoodRecentService recent,CurrentUserService current){this.recent=recent;this.current=current;}
 @GetMapping("/recent") public ResponseEntity<FoodSnapshotReader.Page> recent(@RequestParam(defaultValue="5") int limit,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recent.recentSinglesAndDishes(current.requireGeneralUser(auth).getId(),limit));}
}
