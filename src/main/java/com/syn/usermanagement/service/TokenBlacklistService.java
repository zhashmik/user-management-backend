package com.syn.usermanagement.service;

import com.syn.usermanagement.entity.BlacklistedToken;
import com.syn.usermanagement.repository.BlacklistedTokenRepository;
import com.syn.usermanagement.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtUtils jwtUtils;

    /**
     * Add token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            // Get token expiration
            Date expiration = jwtUtils.extractExpiration(token);
            LocalDateTime expiresAt = expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Save to blacklist
            BlacklistedToken blacklistedToken = new BlacklistedToken(token, expiresAt);
            blacklistedTokenRepository.save(blacklistedToken);

            logger.info("Token blacklisted successfully");
        } catch (Exception e) {
            logger.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    /**
     * Cleanup expired tokens from blacklist (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        logger.info("Running blacklisted token cleanup...");
        blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("Blacklisted token cleanup completed");
    }
}