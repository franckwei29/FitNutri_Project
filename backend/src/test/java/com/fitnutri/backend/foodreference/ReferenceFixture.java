package com.fitnutri.backend.foodreference;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/** Synthetic fixture data only; no official identity/values are invented by tests. */
final class ReferenceFixture {
    record Input(Resource manifest,Resource records,Map<String,Resource> sources) {}
    static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static Input input(String revision,String key,BigDecimal energy) throws Exception {
        var json=JsonMapper.builder().build();byte[] upstream=("SYNTHETIC TEST SOURCE "+key).getBytes(StandardCharsets.UTF_8);
        var row=new LinkedHashMap<String,Object>();row.put("publicFoodKey",key);row.put("surveyId","synthetic-"+key);row.put("officialName","Same synthetic name");row.put("displayName","Same synthetic name");row.put("description","Synthetic fixture only");row.put("energyKj100g",energy);row.put("proteinG100g",BigDecimal.ZERO);row.put("fatG100g",new BigDecimal("2.5"));row.put("carbsG100g",new BigDecimal("10.125"));
        row.put("fiberG100g",null);row.put("sugarG100g",null);row.put("sodiumMg100g",null);
        byte[] records=json.writeValueAsBytes(Map.of("format","fitnutri.food.v1","foods",List.of(row)));
        var manifest=new LinkedHashMap<String,Object>();manifest.put("format","fitnutri.food-manifest.v1");manifest.put("datasetName","SYNTHETIC_TEST_ONLY");manifest.put("release","test");manifest.put("revision",revision);manifest.put("sourceUrl","https://example.test/synthetic");manifest.put("nutrientPolicy","CORE4_REQUIRED");manifest.put("recordsFile","foods.json");manifest.put("recordsSha256",sha(records));manifest.put("expectedCount",1);manifest.put("mapping",Map.of("fixture","synthetic-only"));manifest.put("sourceNote","Synthetic source validation fixture; never official data.");manifest.put("sourceFiles",List.of(Map.of("filename","source.txt","url","https://example.test/synthetic","sha256",sha(upstream))));
        return new Input(new ByteArrayResource(json.writeValueAsBytes(manifest)),new ByteArrayResource(records),Map.of("source.txt",new ByteArrayResource(upstream)));
    }
    static Input transform(Input input,java.util.function.Consumer<tools.jackson.databind.node.ObjectNode> change) throws Exception {
        var json=JsonMapper.builder().build();var data=(tools.jackson.databind.node.ObjectNode)json.readTree(input.records().getInputStream());change.accept(data);byte[] bytes=json.writeValueAsBytes(data);
        var manifest=(tools.jackson.databind.node.ObjectNode)json.readTree(input.manifest().getInputStream());manifest.put("recordsSha256",sha(bytes));manifest.put("expectedCount",data.get("foods").size());
        return new Input(new ByteArrayResource(json.writeValueAsBytes(manifest)),new ByteArrayResource(bytes),input.sources());
    }

}
