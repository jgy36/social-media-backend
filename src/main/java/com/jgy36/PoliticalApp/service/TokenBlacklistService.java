package com.jgy36.PoliticalApp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationMillis) {
        try {
            // Ensure meaningful expiration time
            long safeExpirationMillis = Math.max(expirationMillis, 60000); // Minimum 1 minute
            String blacklistKey = BLACKLIST_PREFIX + token;

            logger.info("🚫 Blacklisting Token: {}", token);
            logger.info("🕒 Token Expiration Time: {} ms", safeExpirationMillis);

            // Store blacklisted token with expiration
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "blacklisted",
                    safeExpirationMillis,
                    TimeUnit.MILLISECONDS
            );

            // Verify token was added to blacklist
            Boolean exists = redisTemplate.hasKey(blacklistKey);
            logger.info("✅ Token Blacklist Confirmation: {}", exists);
        } catch (Exception e) {
            logger.error("❌ Error blacklisting token", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));

            logger.info("🔍 Token Blacklist Check:");
            logger.info("🔑 Blacklist Key: {}", blacklistKey);
            logger.info("🚫 Is Blacklisted: {}", isBlacklisted);

            return isBlacklisted;
        } catch (Exception e) {
            logger.error("❌ Error checking token blacklist", e);
            return false;
        }
    }

    public void removeFromBlacklist(String token) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            Boolean deleted = redisTemplate.delete(blacklistKey);
            logger.info("🗑️ Removed Token from Blacklist: {}, Deleted: {}", token, deleted);
        } catch (Exception e) {
            logger.error("❌ Error removing token from blacklist", e);
        }
    }

    public void clearOldBlacklistedTokens() {
        try {
            // Implementation depends on your Redis configuration
            // This is a basic example and might need adjustment based on your specific Redis setup
            Set<String> blacklistedKeys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (blacklistedKeys != null && !blacklistedKeys.isEmpty()) {
                redisTemplate.delete(blacklistedKeys);
                logger.info("🧹 Cleared {} old blacklisted tokens", blacklistedKeys.size());
            }
        } catch (Exception e) {
            logger.error("❌ Error clearing old blacklisted tokens", e);
        }
    }
}
