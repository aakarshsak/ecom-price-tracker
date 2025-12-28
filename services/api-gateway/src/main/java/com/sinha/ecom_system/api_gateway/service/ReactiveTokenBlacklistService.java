package com.sinha.ecom_system.api_gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ReactiveTokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public ReactiveTokenBlacklistService(
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isTokenBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        return redisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnNext(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.debug("Token {} is blacklisted", tokenId);
                    }
                });
    }
}