package com.fitnutri.backend.speech;

import com.fitnutri.backend.ai.AiFeature;
import com.fitnutri.backend.capabilities.CapabilitiesService;
import com.fitnutri.backend.common.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/speech")
public class SpeechController {
    private final TranscriptionService transcription;private final EntryExtractionService extraction;private final CapabilitiesService capabilities;private final CurrentUserService current;
    public SpeechController(TranscriptionService transcription,EntryExtractionService extraction,CapabilitiesService capabilities,CurrentUserService current){this.transcription=transcription;this.extraction=extraction;this.capabilities=capabilities;this.current=current;}
    @PostMapping(value="/transcribe",consumes="multipart/form-data")
    public ResponseEntity<TranscriptionResponse> transcribe(@RequestParam("audio")MultipartFile audio,Authentication auth,HttpServletRequest request){
        capabilities.requireAvailable(AiFeature.TRANSCRIPTION,current.requireGeneralUser(auth));
        try{var parts=request.getParts();if(parts.size()!=1||parts.stream().anyMatch(part->!part.getName().equals("audio")))throw BusinessException.invalid("audio","Send exactly one audio file without extra fields.");}
        catch(java.io.IOException|jakarta.servlet.ServletException ignored){throw BusinessException.invalid("audio","Send one valid multipart audio file.");}
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new TranscriptionResponse(transcription.transcribe(audio),List.of("Edit and review the transcript before extracting food or exercise details. Nothing has been saved."),RequestCorrelation.id(request)));
    }
    @PostMapping(value="/parse",consumes="application/json")
    public ResponseEntity<ParsedEntryResponse> parse(@jakarta.validation.Valid @RequestBody ParseSpeechRequest input,Authentication auth,HttpServletRequest request){
        var owner=current.requireGeneralUser(auth);capabilities.requireAvailable(AiFeature.SPEECH_PARSING,owner);
        var catalogues=capabilities.getCapabilities(owner).catalogues();var directory=input.kind()==ParseSpeechRequest.Kind.FOOD?catalogues.food():catalogues.exercise();
        if(!directory.ready()||!directory.verified())throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","A verified catalogue is required for this entry kind.");
        return ResponseEntity.ok().header("Cache-Control","no-store").body(extraction.extract(input).withRequestId(RequestCorrelation.id(request)));
    }

}
