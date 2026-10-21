package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.foodreference.FoodReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
/** UX labels and reviewed attributes never replace immutable nutrient references. */
@Entity @Table(name="food_search_profile") @Getter @Setter
public class FoodSearchProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reference_id",nullable=false) private FoodReference reference;
    @Column(name="metadata_revision",length=100,nullable=false) private String metadataRevision;
    @Column(name="is_active",nullable=false) private boolean active;
    @Column(name="review_status",length=16,nullable=false) private String reviewStatus;
    @Column(name="short_name_en",columnDefinition="text",nullable=false) private String shortNameEn;
    @Column(name="short_name_zh",columnDefinition="text") private String shortNameZh;
    @Column(name="concept_key",length=100) private String conceptKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private Map<String,String> facets=Map.of();
    @Column(name="common_rank",nullable=false) private int commonRank;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private Map<String,String> evidence=Map.of();
}
