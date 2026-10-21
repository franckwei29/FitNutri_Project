package com.fitnutri.backend.records.food;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface FoodDishRepository extends JpaRepository<FoodDishRecord,Long>{
 @Query("select d from FoodDishRecord d where d.user.id=:owner and d.id=:id") Optional<FoodDishRecord> findOwnedIncludingDeleted(@Param("owner") Long owner,@Param("id") Long id);
}
