package com.fitnutri.backend.foodreference;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FoodReferenceRepository extends JpaRepository<FoodReference,Long>, JpaSpecificationExecutor<FoodReference> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select f from FoodReference f join fetch f.dataset where f.id in :ids order by f.id")
    List<FoodReference> lockAllById(@Param("ids") java.util.Collection<Long> ids);
    @EntityGraph(attributePaths="dataset")
    @Query("select f from FoodReference f where f.id in :ids")
    List<FoodReference> findAllWithDatasetById(@Param("ids") java.util.Collection<Long> ids);
    @Override @EntityGraph(attributePaths="dataset")
    Optional<FoodReference> findById(Long id);
    @Modifying @Query("update FoodReference f set f.active=false where f.active=true and f.dataset.id in (select d.id from FoodDataset d where d.verified=true)")
    int deactivateActiveVerified();
    @Query("select count(f) from FoodReference f where f.active=true and f.dataset.verified=true and f.energyKj100g is not null and f.proteinG100g is not null and f.fatG100g is not null and f.carbsG100g is not null")
    long countSelectable();
    @Query("select f from FoodReference f join fetch f.dataset d where f.publicFoodKey=:key and f.active=true and d.verified=true")
    Optional<FoodReference> findActiveByPublicFoodKey(@Param("key") String publicFoodKey);
    @EntityGraph(attributePaths="dataset")
    List<FoodReference> findByActiveTrueAndDatasetVerifiedTrue();
    boolean existsByActiveTrueAndDatasetVerifiedTrue();
}
