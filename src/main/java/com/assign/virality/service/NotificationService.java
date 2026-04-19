package com.assign.virality.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final StringRedisTemplate redisTemplate;

    public NotificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Throttles notifications for a user.
     * If the user received a message recently, push it to a pending list.
     * If not, log it and set a cooldown.
     */
    public void notifyUser(Long userId, String message) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";
        String pendingListKey = "user:" + userId + ":pending_notifs";

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        
        if (Boolean.TRUE.equals(hasCooldown)) {
            // User received a notification within 15 minutes, add to list
            redisTemplate.opsForList().rightPush(pendingListKey, message);
        } else {
            // No recent notification, send it immediately
            log.info("Push Notification Sent to User {}: {}", userId, message);
            // Set 15 minute cooldown
            redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
        }
    }
}
