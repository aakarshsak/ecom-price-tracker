package com.sinha.ecom_system.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist Service using Redis
 * 
 * Manages invalidated JWT access tokens to prevent reuse after logout
 * Tokens are stored in Redis with TTL matching their expiry time
 * Redis automatically removes tokens when they naturally expire
 * 
 * Key format: "token:blacklist:{tokenId}"
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a token to the blacklist
     * Token will be automatically removed from blacklist when it expires
     * 
     * @param tokenId The JWT ID (jti claim)
     * @param expiryDate The expiration date of the token
     */
    public void blacklistToken(String tokenId, Date expiryDate) {
        String key = BLACKLIST_PREFIX + tokenId;
        long ttl = expiryDate.getTime() - System.currentTimeMillis();
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            log.debug("Token {} added to blacklist with TTL: {} ms", tokenId, ttl);
        } else {
            log.warn("Token {} is already expired, not adding to blacklist", tokenId);
        }
    }

    /**
     * Check if a token is blacklisted
     * 
     * @param tokenId The JWT ID (jti claim)
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Token {} is blacklisted", tokenId);
            return true;
        }
        
        return false;
    }

    /**
     * Remove a token from the blacklist (manual cleanup, rarely needed)
     * 
     * @param tokenId The JWT ID (jti claim)
     */
    public void removeFromBlacklist(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.delete(key);
        log.debug("Token {} removed from blacklist", tokenId);
    }

    /**
     * Get remaining TTL for a blacklisted token
     * 
     * @param tokenId The JWT ID (jti claim)
     * @return TTL in seconds, or -1 if not blacklisted
     */
    public long getBlacklistTTL(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}

