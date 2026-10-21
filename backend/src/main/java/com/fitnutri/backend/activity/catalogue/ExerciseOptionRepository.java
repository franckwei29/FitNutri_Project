package com.fitnutri.backend.activity.catalogue;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ExerciseOptionRepository extends JpaRepository<ExerciseOption,Long>{
 @Query("select o from ExerciseOption o left join fetch o.reference r left join fetch r.dataset where o.id in :ids order by o.id") List<ExerciseOption> findAllWithReferenceById(@Param("ids") Collection<Long> ids);
}
