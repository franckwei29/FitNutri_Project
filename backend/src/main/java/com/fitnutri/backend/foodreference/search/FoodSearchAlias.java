package com.fitnutri.backend.foodreference.search;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/** A spelling may point to multiple related references, but each row has one target. */
@Entity @Table(name="food_search_alias") @Getter @Setter
public class FoodSearchAlias {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="profile_id") private FoodSearchProfile profile;
    @Column(name="concept_key",length=100) private String conceptKey;
    @Column(length=8,nullable=false) private String locale;
    @Column(name="normalised_text",length=80,nullable=false) private String normalisedText;
    @Column(name="metadata_revision",length=100,nullable=false) private String metadataRevision;
    @Column(name="review_status",length=16,nullable=false) private String reviewStatus;
}
