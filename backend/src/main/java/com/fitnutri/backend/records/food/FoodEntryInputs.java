package com.fitnutri.backend.records.food;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.annotation.JsonDeserialize;

/** V2 food inputs are typed, strictly bounded later by one shared business validator, and never accept nutrients. */
public final class FoodEntryInputs {
 private FoodEntryInputs(){}
 @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,property="kind")
 @JsonSubTypes({@JsonSubTypes.Type(value=Single.class,name="SINGLE"),@JsonSubTypes.Type(value=Dish.class,name="DISH")})
 public sealed interface Entry permits Single,Dish {UUID clientEntryId();MealType mealType();}
 public record Single(UUID clientEntryId,@JsonDeserialize(using=StrictIdDeserializer.class) String referenceId,@JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal quantityGrams,@JsonDeserialize(using=MealDeserializer.class) MealType mealType,@JsonDeserialize(using=TextDeserializer.class) String selectionKind,@JsonDeserialize(using=TextDeserializer.class) String searchMetadataRevision) implements Entry{
  @JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown single food input");}
 }
 public record Ingredient(UUID clientEntryId,@JsonDeserialize(using=StrictIdDeserializer.class) String referenceId,@JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal inputGrams,@JsonDeserialize(using=TextDeserializer.class) String selectionKind,@JsonDeserialize(using=TextDeserializer.class) String searchMetadataRevision){@JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown ingredient input");}}
 public record Dish(UUID clientEntryId,@JsonDeserialize(using=TextDeserializer.class) String name,@JsonDeserialize(using=MealDeserializer.class) MealType mealType,@JsonDeserialize(using=TextDeserializer.class) String inputMode,Map<String,Object> portion,List<Ingredient> ingredients,@JsonDeserialize(using=StrictIdDeserializer.class) String originDishId) implements Entry{
  public Dish{if(portion!=null)portion=Collections.unmodifiableMap(new LinkedHashMap<>(portion));if(ingredients!=null)ingredients=Collections.unmodifiableList(new ArrayList<>(ingredients));}
  @JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown dish input");}
 }
 public record PreviewRequest(LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,@JsonDeserialize(using=SourceDeserializer.class) RecordSource source,List<Entry> entries){public PreviewRequest{if(entries!=null)entries=Collections.unmodifiableList(new ArrayList<>(entries));}@JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown preview input");}}
 public record BatchRequest(UUID submissionId,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,@JsonDeserialize(using=SourceDeserializer.class) RecordSource source,List<Entry> entries,@JsonDeserialize(using=TextDeserializer.class) String previewFingerprint){public BatchRequest{if(entries!=null)entries=Collections.unmodifiableList(new ArrayList<>(entries));}public PreviewRequest preview(){return new PreviewRequest(recordDate,zone,source,entries);}@JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown food submission input");}}
 public record CorrectionRequest(@JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,Entry entry){@JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown correction preview input");}}
 public record UpdateRequest(@JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,Entry entry,@JsonDeserialize(using=TextDeserializer.class) String previewFingerprint){@JsonAnySetter public void reject(String name,Object value){throw new IllegalArgumentException("Unknown food update input");}}
 public static final class TextDeserializer extends tools.jackson.databind.ValueDeserializer<String>{public String deserialize(tools.jackson.core.JsonParser p,tools.jackson.databind.DeserializationContext c){if(!p.hasToken(tools.jackson.core.JsonToken.VALUE_STRING))return c.reportInputMismatch(String.class,"Use a JSON string.");return p.getString();}}
 public static final class MealDeserializer extends tools.jackson.databind.ValueDeserializer<MealType>{public MealType deserialize(tools.jackson.core.JsonParser p,tools.jackson.databind.DeserializationContext c){if(p.hasToken(tools.jackson.core.JsonToken.VALUE_STRING))try{return MealType.valueOf(p.getString());}catch(IllegalArgumentException ignored){}return c.reportInputMismatch(MealType.class,"Choose a supported meal name.");}}
 public static final class SourceDeserializer extends tools.jackson.databind.ValueDeserializer<RecordSource>{public RecordSource deserialize(tools.jackson.core.JsonParser p,tools.jackson.databind.DeserializationContext c){if(p.hasToken(tools.jackson.core.JsonToken.VALUE_STRING))try{var source=RecordSource.valueOf(p.getString());if(source!=RecordSource.PHOTO)return source;}catch(IllegalArgumentException ignored){}return c.reportInputMismatch(RecordSource.class,"Choose MANUAL, IMAGE or VOICE.");}}
}
