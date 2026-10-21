package com.fitnutri.backend.qa;
public record QaResponse(String answer,String disclaimer,QaScopePolicy.Scope scope,QaRequest.RecordRange recordRangeUsed,String requestId){public QaResponse withRequestId(String id){return new QaResponse(answer,disclaimer,scope,recordRangeUsed,id);}}
