package com.syn.usermanagement.repository;

import com.syn.usermanagement.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    /**
     * Check if token is blacklisted
     */
    boolean existsByToken(String token);

    /**
     * Delete expired blacklisted tokens (cleanup job)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}