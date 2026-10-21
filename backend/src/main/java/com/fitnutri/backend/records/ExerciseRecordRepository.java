package com.fitnutri.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from ExerciseRecord r where r.user.id=:owner and r.deletedAt is null and r.recordDate between :from and :to and (:typeId is null or r.reference.id=:typeId) order by r.recordDate desc,r.id desc")
    List<ExerciseRecord> findOwnedRange(@Param("owner") Long owner,@Param("from") java.time.LocalDate from,@Param("to") java.time.LocalDate to,@Param("typeId") Long typeId);
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from ExerciseRecord r where r.user.id=:owner and r.id=:id and r.deletedAt is null")
    java.util.Optional<ExerciseRecord> findOwnedActive(@Param("owner") Long owner,@Param("id") Long id);
    @EntityGraph(attributePaths={"reference","reference.dataset"})
    @Query("select r from ExerciseRecord r where r.user.id=:owner and r.id=:id")
    java.util.Optional<ExerciseRecord> findOwnedIncludingDeleted(@Param("owner") Long owner,@Param("id") Long id);
    boolean existsByUserIdAndClientEntryIdIn(Long userId,java.util.Collection<java.util.UUID> clientEntryIds);
    @Query("select r from ExerciseRecord r where r.user.id=:userId and r.deletedAt is null order by r.recordedAt desc")
    List<ExerciseRecord> findByUserIdOrderByRecordedAtDesc(@Param("userId") Long userId);
}
