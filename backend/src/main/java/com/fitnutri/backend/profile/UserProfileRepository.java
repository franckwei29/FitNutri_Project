package com.fitnutri.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    @org.springframework.data.jpa.repository.Query("select p.dailyTargetKj from UserProfile p where p.userId=:owner")
    java.util.Optional<java.math.BigDecimal> findDailyTargetKjByUserId(@org.springframework.data.repository.query.Param("owner") Long owner);
}
