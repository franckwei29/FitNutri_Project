package com.fitnutri.backend.activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ActivityDatasetRepository extends JpaRepository<ActivityDataset,Long> {
    Optional<ActivityDataset> findByNameAndReleaseAndRevision(String name,String release,String revision);
}
