package com.fitnutri.backend.activity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
final class ActivityFixture {
    record Input(Resource manifest,Resource records,Map<String,Resource> sources) {}
    static String sha(byte[] b) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    static Input input(String revision,String key,String met) throws Exception {
        var json=JsonMapper.builder().build();byte[] source=("SYNTHETIC SOURCE "+key).getBytes(StandardCharsets.UTF_8);
        var row=Map.of("activityKey",key,"displayName","Synthetic activity","sourceActivityCode","99901","sourceDescription","Synthetic activity, general","metValue",new java.math.BigDecimal(met));
        byte[] records=json.writeValueAsBytes(Map.of("format","fitnutri.activity.v1","activities",List.of(row)));
        var m=new LinkedHashMap<String,Object>();m.put("format","fitnutri.activity-manifest.v1");m.put("datasetName","SYNTHETIC_ACTIVITY_TEST_ONLY");m.put("release","test");m.put("revision",revision);m.put("sourceUrl","https://example.test/activity");m.put("minAge",19);m.put("maxAge",59);m.put("recordsFile","activities.json");m.put("recordsSha256",sha(records));m.put("expectedCount",1);m.put("mapping",Map.of("fixture","synthetic source/age evidence"));m.put("sourceNote","Synthetic fixture only, not official activity facts.");m.put("sourceFiles",List.of(Map.of("filename","source.txt","url","https://example.test/activity","sha256",sha(source))));
        return new Input(new ByteArrayResource(json.writeValueAsBytes(m)),new ByteArrayResource(records),Map.of("source.txt",new ByteArrayResource(source)));
    }
    static Input transform(Input i,java.util.function.Consumer<ObjectNode> change) throws Exception {
        var json=JsonMapper.builder().build();var data=(ObjectNode)json.readTree(i.records().getInputStream());change.accept(data);byte[] bytes=json.writeValueAsBytes(data);var m=(ObjectNode)json.readTree(i.manifest().getInputStream());m.put("recordsSha256",sha(bytes));m.put("expectedCount",data.get("activities").size());return new Input(new ByteArrayResource(json.writeValueAsBytes(m)),new ByteArrayResource(bytes),i.sources());
    }
}
