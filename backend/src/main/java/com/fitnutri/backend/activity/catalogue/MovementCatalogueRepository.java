package com.fitnutri.backend.activity.catalogue;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface MovementCatalogueRepository extends JpaRepository<MovementCatalogue,Long>{
 @Query("select m from MovementCatalogue m join fetch m.defaultPeriodOption where m.id in :ids order by m.id") List<MovementCatalogue> findAllWithDefaultById(@Param("ids") Collection<Long> ids);
}
