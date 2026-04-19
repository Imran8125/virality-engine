package com.assign.virality.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class RedisGuardrailService {

    private final StringRedisTemplate redisTemplate;

    public RedisGuardrailService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Increments the bot count for a post. If it exceeds 100, throws an exception.
     */
    public void enforceHorizontalCap(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count > 100) {
            // Count can exceed 100 in Redis, but we reject the actual request
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Post has reached the maximum number of bot replies.");
        }
    }

    /**
     * Checks vertical cap for comments.
     */
    public void enforceVerticalCap(int depthLevel) {
        if (depthLevel > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment thread depth cannot exceed 20 levels.");
        }
    }

    /**
     * Checks and sets a cooldown for a bot interacting with a human.
     */
    public void enforceCooldownCap(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(isSet)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot is on cooldown for interacting with this human.");
        }
    }

    /**
     * Increments the virality score of a post based on interaction type.
     */
    public void updateViralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }

    /**
     * Gets the current virality score of a post.
     */
    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
