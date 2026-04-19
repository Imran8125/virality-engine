package com.assign.virality.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationSweeper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSweeper.class);
    private final StringRedisTemplate redisTemplate;

    public NotificationSweeper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void sweepNotifications() {
        log.info("Starting scheduled notification sweeper...");
        Set<String> keys = scanKeys("user:*:pending_notifs");
        for (String key : keys) {
            String userIdStr = key.split(":")[1];
            
            // Pop all pending messages atomically or by fetching then deleting
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
            if (messages != null && !messages.isEmpty()) {
                long size = messages.size();
                redisTemplate.delete(key);
                
                String firstMessage = messages.get(0);
                if (size == 1) {
                    log.info("Summarized Push Notification: {} (and 0 others interacted with your posts.)", firstMessage);
                } else {
                    log.info("Summarized Push Notification: {} and {} others interacted with your posts.", firstMessage, size - 1);
                }
            }
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .keyCommands()
                .scan(ScanOptions.scanOptions().match(pattern).build())) {
            
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.error("Error scanning redis keys", e);
        }
        return keys;
    }
}
