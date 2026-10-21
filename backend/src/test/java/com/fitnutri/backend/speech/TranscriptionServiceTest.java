package com.fitnutri.backend.speech;

import com.fitnutri.backend.ai.ControlledAiProvider;
import com.fitnutri.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class TranscriptionServiceTest {
    MockMultipartFile audio() throws Exception {try(var input=getClass().getResourceAsStream("/fixtures/ai/synthetic-tone.webm")){assertNotNull(input);return new MockMultipartFile("audio","private-file.webm","audio/webm",input.readAllBytes());}}
    @Test void realMultipartProviderRequestContainsOnlyDecodedAudioAndReturnsEditableText() throws Exception {
        try(var provider=new ControlledAiProvider()) {provider.body="{\"text\":\"Synthetic controlled-provider transcript only.\"}";var service=new TranscriptionService(provider.transport,new AudioValidator(System.getenv().getOrDefault("FFMPEG_BIN","ffmpeg"),10000));assertEquals("Synthetic controlled-provider transcript only.",service.transcribe(audio()));assertEquals(1,provider.calls.get());assertTrue(provider.lastRequest.contains("filename=\"audio.wav\""));assertTrue(provider.lastRequest.contains("Content-Type: audio/wav"));assertFalse(provider.lastRequest.contains("private-file"));assertFalse(provider.lastRequest.contains("SYNTHETIC_PRIVATE_METADATA"));assertFalse(provider.lastRequest.contains("userId"));}
    }
    @Test void invalidProviderTextAndProviderFailuresNeverUseAnExampleTranscript() throws Exception {
        try(var provider=new ControlledAiProvider()){var service=new TranscriptionService(provider.transport,new AudioValidator(System.getenv().getOrDefault("FFMPEG_BIN","ffmpeg"),10000));for(String body:new String[]{"{}","{\"text\":5}","{\"text\":\"\"}","{\"text\":\"   \"}","{\"text\":\"a\\u0000b\"}","{\"text\":\""+"a".repeat(10001)+"\"}","not private JSON"}){provider.body=body;assertEquals("PROVIDER_BAD_RESPONSE",assertThrows(BusinessException.class,()->service.transcribe(audio())).code());}provider.status=401;assertEquals("FEATURE_NOT_CONFIGURED",assertThrows(BusinessException.class,()->service.transcribe(audio())).code());}
    }
    @Test void malformedAudioNeverReachesProvider() throws Exception {try(var provider=new ControlledAiProvider()){var service=new TranscriptionService(provider.transport,new AudioValidator(System.getenv().getOrDefault("FFMPEG_BIN","ffmpeg"),10000));assertThrows(BusinessException.class,()->service.transcribe(new MockMultipartFile("audio","bad.webm","audio/webm",new byte[]{1,2})));assertEquals(0,provider.calls.get());}}
}
