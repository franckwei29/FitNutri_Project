package com.fitnutri.backend.records.food;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
/** One owned historical occurrence, never a shared mutable recipe template or a second nutrient ledger. */
@Entity @Table(name="food_dish_record") @Getter @Setter
public class FoodDishRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="client_entry_id",nullable=false) private UUID clientEntryId;
 @Column(nullable=false,length=100) private String name;
 @Column(name="recorded_date",nullable=false) private LocalDate recordDate;
 @Enumerated(EnumType.STRING) @Column(name="meal_type",nullable=false,length=16) private MealType mealType;
 @Column(name="input_mode",nullable=false,length=16) private String inputMode;
 @Column(name="portion_input_kind",nullable=false,length=16) private String portionInputKind;
 @Column(name="portion_numerator",nullable=false) private long portionNumerator;
 @Column(name="portion_denominator",nullable=false) private long portionDenominator;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="portion_input_json",columnDefinition="jsonb",nullable=false) private Map<String,Object> portionInput=Map.of();
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private RecordSource source;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="origin_dish_id") private FoodDishRecord origin;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=createdAt;
 @Version @Column(nullable=false) private long version;
 @Column(name="deleted_at") private Instant deletedAt;
}
