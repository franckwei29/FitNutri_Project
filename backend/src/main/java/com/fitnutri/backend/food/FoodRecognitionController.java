package com.fitnutri.backend.food;

import com.fitnutri.backend.ai.AiFeature;
import com.fitnutri.backend.capabilities.CapabilitiesService;
import com.fitnutri.backend.common.CurrentUserService;
import com.fitnutri.backend.common.RequestCorrelation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/food")
public class FoodRecognitionController {
    private final OpenAiVisionService vision;private final CapabilitiesService capabilities;private final CurrentUserService current;private final ImageValidator images;
    public FoodRecognitionController(OpenAiVisionService vision,CapabilitiesService capabilities,CurrentUserService current,ImageValidator images){this.vision=vision;this.capabilities=capabilities;this.current=current;this.images=images;}
    @PostMapping(value="/recognize",consumes="multipart/form-data")
    public ResponseEntity<FoodRecognitionResponse> recognize(@RequestParam("image")MultipartFile image,Authentication auth,HttpServletRequest request){
        capabilities.requireAvailable(AiFeature.IMAGE,current.requireGeneralUser(auth));
        var candidates=vision.recognizeFood(images.read(image));
        var notices=candidates.isEmpty()?List.of("No food candidates were returned. Try another image or enter the foods manually."):List.of("These are unconfirmed image suggestions. Review every reference, portion and meal before saving.");
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new FoodRecognitionResponse(candidates,notices,RequestCorrelation.id(request)));
    }
}
