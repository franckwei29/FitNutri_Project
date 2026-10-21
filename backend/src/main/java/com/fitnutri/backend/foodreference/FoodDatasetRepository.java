package com.fitnutri.backend.foodreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FoodDatasetRepository extends JpaRepository<FoodDataset,Long> {
    Optional<FoodDataset> findByNameAndReleaseAndRevision(String name,String release,String revision);
}
