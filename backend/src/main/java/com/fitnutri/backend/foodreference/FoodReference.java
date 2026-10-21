package com.fitnutri.backend.foodreference;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "food_reference")
@Getter
@Setter
@NoArgsConstructor
public class FoodReference {

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="dataset_id",nullable=false)
    private FoodDataset dataset;
    @Column(name="public_food_key",nullable=false)
    private String publicFoodKey;
    @Column(name="survey_id") private String surveyId;
    @Column(name="official_name",columnDefinition="text") private String officialName;
    @Column(name="display_name",columnDefinition="text",nullable=false) private String displayName;
    @Column(columnDefinition="text") private String description;
    @Column(name="energy_kj_100g",precision=12,scale=3) private BigDecimal energyKj100g;
    @Column(name="protein_g_100g",precision=12,scale=3) private BigDecimal proteinG100g;
    @Column(name="fat_g_100g",precision=12,scale=3) private BigDecimal fatG100g;
    @Column(name="carbs_g_100g",precision=12,scale=3) private BigDecimal carbsG100g;
    @Column(name="is_active",nullable=false) private boolean active;


    private String foodName;

    // All nutrient fields below are per 100 g of food, matching AFCD's reporting convention.
    private Double energyKcal;
    private Double proteinG;
    private Double fatG;
    private Double carbsG;
    private Double fiberG;
    private Double sugarG;
    private Double sodiumMg;
}
