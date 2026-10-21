package com.fitnutri.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {

    Optional<WeightEntry> findFirstByUserIdOrderByEntryDateDesc(Long userId);

    Optional<WeightEntry> findByUserIdAndEntryDate(Long userId,LocalDate date);
    Optional<WeightEntry> findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(Long userId,LocalDate date);
    List<WeightEntry> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(Long userId,LocalDate from,LocalDate to);

    /** Newest entry date first; for the same date, the most recently saved first. */
    List<WeightEntry> findByUserIdOrderByEntryDateDescCreatedAtDesc(Long userId);
}