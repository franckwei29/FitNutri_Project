package com.fitnutri.backend.speech;
import java.util.List;
public record TranscriptionResponse(String transcript,List<String> notices,String requestId){}
