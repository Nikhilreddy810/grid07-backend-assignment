package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long NOTIF_COOLDOWN_MINUTES = 15;

    /**
     * Called when a bot interacts with a user's post.
     * If user is in cooldown → push to pending list.
     * If not → send immediately and set cooldown.
     */
    public void handleBotInteraction(Long userId, String botName, Long postId) {
        String cooldownKey  = "notif:cooldown:user_" + userId;
        String pendingKey   = "user:" + userId + ":pending_notifs";
        String notifMessage = "Bot " + botName + " replied to your post (postId=" + postId + ")";

        Boolean inCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(inCooldown)) {
            // Push to pending list
            redisTemplate.opsForList().rightPush(pendingKey, notifMessage);
            log.info("Notification queued for user {}: {}", userId, notifMessage);
        } else {
            // Send immediately and set 15-minute cooldown
            log.info("Push Notification Sent to User {}: {}", userId, notifMessage);
            redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(NOTIF_COOLDOWN_MINUTES));
        }
    }
}
