package com.fitnutri.backend.capabilities;
import com.fitnutri.backend.ai.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.User;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.fitnutri.backend.capabilities.CapabilitiesResponse.*;
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class CapabilitiesService {
    private final CapabilitiesRepository catalogues;private final UserProfileRepository profiles;private final AiFeatureConfiguration config;private final AiFeatureRegistry modules;private final ZoneResolver zones;
    public CapabilitiesService(CapabilitiesRepository catalogues,UserProfileRepository profiles,AiFeatureConfiguration config,AiFeatureRegistry modules,ZoneResolver zones){this.catalogues=catalogues;this.profiles=profiles;this.config=config;this.modules=modules;this.zones=zones;}
    public CapabilitiesResponse getCapabilities(User owner){
        var food=catalogues.food();var exercise=catalogues.exercise();boolean foodReady=food.usableCount()>0,exerciseReady=exercise.usableCount()>0;
        boolean user=owner.getRole()==User.Role.USER;boolean needsProfile=user&&Arrays.stream(AiFeature.values()).anyMatch(f->config.configured(f)&&config.enabled(f)&&modules.registered(f));var profile=needsProfile?profiles.findById(owner.getId()).orElse(null):null;
        return new CapabilitiesResponse("mvp-1",zones.resolve(null).getId(),revision(food,exercise),new Catalogues(catalogue(food,"food"),catalogue(exercise,"exercise")),new Features(capability(AiFeature.IMAGE,user,profile,foodReady),capability(AiFeature.TRANSCRIPTION,user,profile,true),capability(AiFeature.SPEECH_PARSING,user,profile,foodReady||exerciseReady),capability(AiFeature.QA,user,profile,true)));
    }
    public void requireAvailable(AiFeature feature,User owner){var all=getCapabilities(owner).features();var selected=switch(feature){case IMAGE->all.image();case TRANSCRIPTION->all.transcription();case SPEECH_PARSING->all.speechParsing();case QA->all.qa();};if(!selected.available())throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FEATURE_NOT_CONFIGURED",selected.reason());}
    private Capability capability(AiFeature feature,boolean user,UserProfile profile,boolean directory){
        boolean configured=config.configured(feature);String reason;
        if(!configured)reason="This AI feature is not configured.";
        else if(!config.enabled(feature))reason="This AI feature is currently disabled.";
        else if(!modules.registered(feature))reason="This AI feature is currently unavailable.";
        else if(!user)reason="This feature requires a user account.";
        else if(profile==null||!profile.isOnboardingCompleted()||!profile.isAdultConfirmed())reason="Complete your profile before using this feature.";
        else if(!directory)reason="A verified reference catalogue is not ready for this feature.";
        else if(feature==AiFeature.QA&&!QaEligibility.isEligible(profile))reason="Personalised Q&A is restricted for this profile. Recording and factual summaries remain available.";
        else reason=null;
        return new Capability(configured,reason==null,reason);
    }
    private static Catalogue catalogue(CapabilitiesRepository.Snapshot snapshot,String name){boolean ready=snapshot.usableCount()>0,verified=snapshot.verifiedCount()>0;return new Catalogue(ready,verified,ready?null:verified?"No usable "+name+" references are currently available.":"A verified "+name+" catalogue is not configured.");}
    private static String revision(CapabilitiesRepository.Snapshot food,CapabilitiesRepository.Snapshot exercise){String source="food:"+Objects.toString(food.selectedIds(),"")+":"+Objects.toString(food.datasetVersions(),"")+"|exercise:"+Objects.toString(exercise.selectedIds(),"")+":"+Objects.toString(exercise.datasetVersions(),"");try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}}
}
