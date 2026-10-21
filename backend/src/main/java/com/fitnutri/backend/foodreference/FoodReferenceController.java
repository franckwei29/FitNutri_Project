package com.fitnutri.backend.foodreference;

import java.math.BigDecimal;
import com.fitnutri.backend.records.OwnedPreviewService;
import com.fitnutri.backend.common.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/food-reference")
public class FoodReferenceController {
    private final FoodReferenceService foods;
    private final FoodCalculationService calculations;
    private final OwnedPreviewService previews;private final CurrentUserService current;
    public FoodReferenceController(FoodReferenceService foods,FoodCalculationService calculations,OwnedPreviewService previews,CurrentUserService current) {this.foods=foods;this.calculations=calculations;this.previews=previews;this.current=current;}
    @GetMapping("/search") public FoodSearchResponse search(@RequestParam(required=false) String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return foods.searchActive(query,page,size);}
    @GetMapping("/by-id/{id}") public FoodReferenceResponse getById(@PathVariable Long id) {return foods.getImmutableReference(id);}
    @GetMapping("/{publicFoodKey}") public FoodReferenceResponse getByKey(@PathVariable String publicFoodKey) {return foods.getActiveByPublicKey(publicFoodKey);}
    @GetMapping("/by-id/{id}/nutrition") public FoodNutritionResponse previewByReferenceId(@PathVariable Long id,@RequestParam BigDecimal grams,@RequestParam(required=false)Long recordId,Authentication auth) {return recordId==null?calculations.preview(foods.requireImmutable(id),grams):previews.food(current.requireGeneralUser(auth).getId(),id,grams,recordId);}
    @GetMapping("/{publicFoodKey}/nutrition") public FoodNutritionResponse previewByPublicKey(@PathVariable String publicFoodKey,@RequestParam BigDecimal grams) {return calculations.preview(foods.requireActiveByPublicKey(publicFoodKey),grams);}
}
