package com.fitnutri.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = com.fitnutri.backend.user.User.Role.ADMIN order by u.id")
    java.util.List<User> lockAdministrators();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("select u from User u where lower(trim(u.email)) = :email")
    Optional<User> findByCanonicalEmail(@Param("email") String canonicalEmail);
}
