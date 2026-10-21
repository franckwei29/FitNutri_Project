package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profiles;
    private final CurrentUserService current;
    public ProfileController(ProfileService profiles,CurrentUserService current) {this.profiles=profiles;this.current=current;}
    @GetMapping public ProfileResponse get(Authentication auth) {return profiles.getCurrent(current.requireGeneralUser(auth).getId());}
    @PostMapping("/onboarding") public ProfileResponse onboarding(@Valid @RequestBody CompleteOnboardingRequest request,Authentication auth) {return profiles.completeOnboarding(current.requireGeneralUser(auth).getId(),request);}
    @PutMapping public ProfileResponse update(@Valid @RequestBody ProfileSettingsRequest request,Authentication auth) {return profiles.updateSettings(current.requireGeneralUser(auth).getId(),request);}
    @DeleteMapping("/goal") public ProfileResponse removeGoal(@RequestParam long expectedUserVersion,@RequestParam long expectedProfileVersion,Authentication auth) {return profiles.clearGoal(current.requireGeneralUser(auth).getId(),expectedUserVersion,expectedProfileVersion,false);}
    @DeleteMapping("/energy-goal") public ProfileResponse removeEnergyGoal(@RequestParam long expectedUserVersion,@RequestParam long expectedProfileVersion,Authentication auth) {return profiles.clearGoal(current.requireGeneralUser(auth).getId(),expectedUserVersion,expectedProfileVersion,true);}
}
