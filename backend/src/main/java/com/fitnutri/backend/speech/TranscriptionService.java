package com.fitnutri.backend.speech;

import com.fitnutri.backend.ai.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stateless transcription only: no record repository, audio persistence or example fallback. */
@Service
public final class TranscriptionService implements AiFeatureModule {
    private final AiTransport transport;private final AudioValidator audio;
    public TranscriptionService(AiTransport transport,AudioValidator audio){this.transport=transport;this.audio=audio;}
    @Override public AiFeature feature(){return AiFeature.TRANSCRIPTION;}
    @Override public boolean ready(){return audio.ready();}
    public String transcribe(MultipartFile input){
        transport.validateProviderConfiguration(feature());
        var prepared=audio.read(input);var response=transport.sendMultipart(feature(),prepared.bytes(),prepared.mimeType()).json();var text=response.get("text");
        if(text==null||!text.isTextual())throw AiExceptionMapper.badResponse();String value=text.textValue().strip();
        if(value.isEmpty()||value.length()>10000||value.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw AiExceptionMapper.badResponse();
        return value;
    }
}
