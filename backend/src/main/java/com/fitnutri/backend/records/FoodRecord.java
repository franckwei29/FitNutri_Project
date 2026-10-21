package com.fitnutri.backend.records;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.fitnutri.backend.foodreference.FoodReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "food_record")
@Getter
@Setter
@NoArgsConstructor
public class FoodRecord extends BaseRecord {

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(name = "quantity_grams_legacy")
    private Double quantityGrams;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    private Double confidence;

    private Double calories;

    @Column(name = "protein_grams_legacy")
    private Double proteinGrams;

    @Column(name = "carbs_grams_legacy")
    private Double carbsGrams;

    @Column(name = "fat_grams_legacy")
    private Double fatGrams;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="food_reference_id") private FoodReference reference;
    @Column(name="quantity_grams",precision=24,scale=12) private BigDecimal quantityGramsSnapshot;
    @Column(name="energy_kj",precision=12,scale=3) private BigDecimal energyKj;
    @Column(name="protein_grams",precision=12,scale=3) private BigDecimal proteinGramsSnapshot;
    @Column(name="carbs_grams",precision=12,scale=3) private BigDecimal carbsGramsSnapshot;
    @Column(name="fat_grams",precision=12,scale=3) private BigDecimal fatGramsSnapshot;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="dish_id") private com.fitnutri.backend.records.food.FoodDishRecord dish;
    @Column(name="dish_position") private Integer dishPosition;
    @Column(name="ingredient_input_grams",precision=12,scale=3) private BigDecimal ingredientInputGrams;
    @Column(name="nutrition_policy",nullable=false,length=48) private String nutritionPolicy="REFERENCE_GRAMS_V1";
    @Column(name="selection_kind",nullable=false,length=16) private String selectionKind="USER_SELECTED";
    @Column(name="search_metadata_revision",length=100) private String searchMetadataRevision;
    // Temporary legacy read-only DTO/summary adapters. Canonical services use decimal snapshots directly.
    public Double getQuantityGrams() {return quantityGramsSnapshot==null?quantityGrams:quantityGramsSnapshot.doubleValue();}
    public Double getProteinGrams() {return proteinGramsSnapshot==null?proteinGrams:proteinGramsSnapshot.doubleValue();}
    public Double getCarbsGrams() {return carbsGramsSnapshot==null?carbsGrams:carbsGramsSnapshot.doubleValue();}
    public Double getFatGrams() {return fatGramsSnapshot==null?fatGrams:fatGramsSnapshot.doubleValue();}

}
