package com.fitnutri.backend.capabilities;

import com.fitnutri.backend.ai.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapabilitiesServiceTest {
    private final CapabilitiesRepository catalogues=mock(CapabilitiesRepository.class);private final UserProfileRepository profiles=mock(UserProfileRepository.class);private final AiFeatureRegistry modules=mock(AiFeatureRegistry.class);
    private final User user=user();
    private User user(){var u=new User();u.setId(41L);return u;}
    private AiFeatureConfiguration config(String key,boolean enabled){return new AiFeatureConfiguration(key,"synthetic-vision","synthetic-transcription","synthetic-parse","synthetic-qa",enabled,enabled,enabled,enabled);}
    private CapabilitiesService service(AiFeatureConfiguration c){return new CapabilitiesService(catalogues,profiles,c,modules,new ZoneResolver("Australia/Sydney"));}
    private void ready(){when(catalogues.food()).thenReturn(new CapabilitiesRepository.Snapshot(3,3,"1,2,3","1:synthetic-food-hash"));when(catalogues.exercise()).thenReturn(new CapabilitiesRepository.Snapshot(2,2,"1,2","1:synthetic-exercise-hash"));}
    private void implemented(){when(modules.registered(any())).thenReturn(true);var p=new UserProfile();p.setOnboardingCompleted(true);p.setAdultConfirmed(true);p.setHealthSafetyAnswer(HealthSafetyAnswer.NO);p.setHealthSafetyConsent(false);when(profiles.findById(41L)).thenReturn(Optional.of(p));}
    @Test void noCredentialsIsExplicitlyUnconfiguredAndNoSensitiveProfileReadOccurs(){ready();var result=service(config("",true)).getCapabilities(user);assertEquals("mvp-1",result.schemaVersion());assertEquals("Australia/Sydney",result.defaultZone());assertTrue(result.catalogues().food().ready());assertFalse(result.features().image().configured());assertFalse(result.features().image().available());assertNotNull(result.features().image().reason());verifyNoInteractions(profiles);}
    @Test void configurationDoesNotPretendAnUnintegratedAdapterOrRealSmokePassed(){ready();var result=service(config("synthetic-secret",true)).getCapabilities(user);assertTrue(result.features().image().configured());assertFalse(result.features().image().available());assertNotNull(result.features().image().reason());verifyNoInteractions(profiles);}
    @Test void configuredDisabledFeatureStaysUnavailable(){ready();implemented();var result=service(config("synthetic-secret",false)).getCapabilities(user);assertTrue(result.features().image().configured());assertFalse(result.features().image().available());verify(profiles,never()).findById(any());}
    @Test void helperAvailabilityUsesDirectoryAndSetupButQaRequiresFreshExplicitConsent(){ready();implemented();var policy=service(config("synthetic-secret",true));var result=policy.getCapabilities(user);assertTrue(result.features().image().available());assertTrue(result.features().transcription().available());assertTrue(result.features().speechParsing().available());assertFalse(result.features().qa().available());var p=profiles.findById(41L).orElseThrow();p.setHealthSafetyConsent(true);assertTrue(policy.getCapabilities(user).features().qa().available());p.setHealthSafetyAnswer(HealthSafetyAnswer.YES_OR_UNSURE);assertFalse(policy.getCapabilities(user).features().qa().available());}
    @Test void unverifiedIncompleteDirectoryDoesNotBecomeReadyOrEnableImage(){ready();implemented();when(catalogues.food()).thenReturn(new CapabilitiesRepository.Snapshot(0,0,"",""));var result=service(config("synthetic-secret",true)).getCapabilities(user);assertFalse(result.catalogues().food().ready());assertFalse(result.catalogues().food().verified());assertFalse(result.features().image().available());assertTrue(result.features().transcription().available());}
    @Test void selectedMembershipChangesRevisionEvenWithinTheSameImmutableDataset(){ready();var service=service(config("",false));String first=service.getCapabilities(user).catalogueRevision();when(catalogues.food()).thenReturn(new CapabilitiesRepository.Snapshot(2,2,"1,3","1:synthetic-food-hash"));assertNotEquals(first,service.getCapabilities(user).catalogueRevision());assertTrue(first.matches("[a-f0-9]{64}"));}
    @Test void administratorHasAuthenticatedMetadataButNeverUserAiScope(){ready();implemented();user.setRole(User.Role.ADMIN);var result=service(config("synthetic-secret",true)).getCapabilities(user);assertFalse(result.features().image().available());assertFalse(result.features().qa().available());verify(profiles,never()).findById(any());}
    @Test void publicDtoAndConfigurationToStringNeverExposeSecretOrModelNames() throws Exception {ready();var configuration=config("synthetic-secret-never-expose",true);String response=JsonMapper.builder().build().writeValueAsString(service(configuration).getCapabilities(user));assertFalse(response.contains("synthetic-secret-never-expose"));assertFalse(response.contains("synthetic-vision"));assertFalse(configuration.toString().contains("synthetic-secret-never-expose"));assertTrue(response.contains("configured"));}
}
