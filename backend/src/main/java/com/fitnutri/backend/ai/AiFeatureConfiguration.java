package com.fitnutri.backend.ai;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
/** Server-only configuration. No generated toString/getter bean that could serialize secrets. */
@Component
public final class AiFeatureConfiguration {
    private final String key;private final Map<AiFeature,String> models;private final Set<AiFeature> enabled;
    public AiFeatureConfiguration(@Value("${app.ai.key:}")String key,@Value("${app.ai.image-model:}")String image,@Value("${app.ai.transcription-model:}")String transcription,@Value("${app.ai.parsing-model:}")String parsing,@Value("${app.ai.qa-model:}")String qa,@Value("${app.ai.image-enabled:false}")boolean imageEnabled,@Value("${app.ai.transcription-enabled:false}")boolean transcriptionEnabled,@Value("${app.ai.parsing-enabled:false}")boolean parsingEnabled,@Value("${app.ai.qa-enabled:false}")boolean qaEnabled){
        this.key=clean(key);models=Map.of(AiFeature.IMAGE,clean(image),AiFeature.TRANSCRIPTION,clean(transcription),AiFeature.SPEECH_PARSING,clean(parsing),AiFeature.QA,clean(qa));var flags=EnumSet.noneOf(AiFeature.class);if(imageEnabled)flags.add(AiFeature.IMAGE);if(transcriptionEnabled)flags.add(AiFeature.TRANSCRIPTION);if(parsingEnabled)flags.add(AiFeature.SPEECH_PARSING);if(qaEnabled)flags.add(AiFeature.QA);enabled=Set.copyOf(flags);
    }
    public boolean configured(AiFeature feature){return key.matches("[!-~]{1,1024}")&&models.get(feature).matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}");}
    public boolean enabled(AiFeature feature){return enabled.contains(feature);}
    public String apiKey(){return key;}
    public String model(AiFeature feature){return models.get(feature);}
    private static String clean(String value){return value==null?"":value.trim();}
}
