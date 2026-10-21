package com.fitnutri.backend.records.workout;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkoutMovementRepository extends JpaRepository<WorkoutMovementLog,Long>{}
