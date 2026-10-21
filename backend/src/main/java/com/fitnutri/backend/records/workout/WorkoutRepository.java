package com.fitnutri.backend.records.workout;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface WorkoutRepository extends JpaRepository<WorkoutRecord,Long>{
 @Query("select r from WorkoutRecord r where r.user.id=:owner and r.id=:id") Optional<WorkoutRecord> findOwnedIncludingDeleted(@Param("owner")Long owner,@Param("id")Long id);
}
