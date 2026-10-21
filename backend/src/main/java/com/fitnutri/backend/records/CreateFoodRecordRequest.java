package com.fitnutri.backend.records;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public record CreateFoodRecordRequest(
    @JsonProperty(required=true) @NotNull UUID submissionId,
    @JsonProperty(required=true) @NotNull LocalDate recordDate,
    @JsonProperty(required=true) @NotBlank String zone,
    @JsonProperty(required=true) @NotNull RecordSource source,
    @JsonProperty(required=true) @NotNull UUID clientEntryId,
    @JsonProperty(required=true) @NotBlank @Pattern(regexp="[1-9][0-9]*") @Size(max=19) @JsonDeserialize(using=StrictIdDeserializer.class) String referenceId,
    @JsonProperty(required=true) @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("10000") @JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal quantityGrams,
    @JsonProperty(required=true) @NotNull MealType mealType
) {
    public FoodBatchRequest toBatch(){return new FoodBatchRequest(submissionId,recordDate,zone,source,List.of(new FoodBatchRequest.Item(clientEntryId,referenceId,quantityGrams,mealType)));}
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown food field");}
}
