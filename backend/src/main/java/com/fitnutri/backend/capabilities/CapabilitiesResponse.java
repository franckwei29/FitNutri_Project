package com.fitnutri.backend.capabilities;
public record CapabilitiesResponse(String schemaVersion,String defaultZone,String catalogueRevision,Catalogues catalogues,Features features){
    public record Catalogue(boolean ready,boolean verified,String reason){}
    public record Catalogues(Catalogue food,Catalogue exercise){}
    public record Capability(boolean configured,boolean available,String reason){}
    public record Features(Capability image,Capability transcription,Capability speechParsing,Capability qa){}
}
