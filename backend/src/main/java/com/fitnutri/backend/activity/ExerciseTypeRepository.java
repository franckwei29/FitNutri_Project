package com.fitnutri.backend.activity;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;
public interface ExerciseTypeRepository extends JpaRepository<ExerciseType,Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select t from ExerciseType t join fetch t.dataset where t.id=:id")
    Optional<ExerciseType> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select t from ExerciseType t join fetch t.dataset where t.id in :ids order by t.id")
    List<ExerciseType> lockAllById(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
    @Override @EntityGraph(attributePaths="dataset") Optional<ExerciseType> findById(Long id);
    @Modifying @Query("update ExerciseType t set t.active=false where t.active=true and t.dataset.id in (select d.id from ActivityDataset d where d.verified=true)") int deactivateActiveVerified();
    @EntityGraph(attributePaths="dataset")
    @Query("select t from ExerciseType t where t.active=true and t.dataset.verified=true and t.sourceActivityCode is not null and trim(t.sourceActivityCode)<>'' and t.sourceDescription is not null and trim(t.sourceDescription)<>'' and t.dataset.minAge is not null and t.dataset.maxAge is not null")
    List<ExerciseType> findByActiveTrueAndDatasetVerifiedTrue();
    boolean existsByActiveTrueAndDatasetVerifiedTrue();
}
