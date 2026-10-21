package com.fitnutri.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRecordRepository extends JpaRepository<FoodRecord, Long> {
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from FoodRecord r where r.user.id=:owner and r.deletedAt is null and r.recordDate between :from and :to and (:mealType is null or r.mealType=:mealType) order by r.recordDate desc,r.id desc")
    List<FoodRecord> findOwnedRange(@Param("owner") Long owner,@Param("from") java.time.LocalDate from,@Param("to") java.time.LocalDate to,@Param("mealType") MealType mealType);
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from FoodRecord r where r.user.id=:owner and r.id=:id and r.deletedAt is null")
    java.util.Optional<FoodRecord> findOwnedActive(@Param("owner") Long owner,@Param("id") Long id);
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from FoodRecord r where r.user.id=:owner and r.id=:id")
    java.util.Optional<FoodRecord> findOwnedIncludingDeleted(@Param("owner") Long owner,@Param("id") Long id);
    boolean existsByUserIdAndClientEntryIdIn(Long userId,java.util.Collection<java.util.UUID> clientEntryIds);
    @Query("select r from FoodRecord r where r.user.id=:userId and r.deletedAt is null order by r.recordedAt desc")
    List<FoodRecord> findByUserIdOrderByRecordedAtDesc(@Param("userId") Long userId);
}
