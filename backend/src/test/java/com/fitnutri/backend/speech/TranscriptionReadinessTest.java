package com.fitnutri.backend.speech;

import com.fitnutri.backend.ai.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TranscriptionReadinessTest {
    @Test void configuredAdapterWithMissingLocalDecoderRemainsUnavailableWithoutProviderProbe(){var provider=mock(AiTransport.class);var service=new TranscriptionService(provider,new AudioValidator("/nonexistent/fitnutri-owned-test-no-ffmpeg",100));var beans=new StaticListableBeanFactory();beans.addBean("transcription",service);assertFalse(new AiFeatureRegistry(beans.getBeanProvider(AiFeatureModule.class)).registered(AiFeature.TRANSCRIPTION));verifyNoInteractions(provider);}
}
