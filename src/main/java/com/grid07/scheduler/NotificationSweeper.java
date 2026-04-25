package com.grid07.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSweeper {

	private final RedisTemplate<String, String> redisTemplate;
    /**
     * Runs every 5 minutes (300,000 ms).
     * Scans all pending notification lists.
     * Pops messages, counts them, logs a summary, clears the list.
     */
    @Scheduled(fixedRate = 300_000)
    public void sweepPendingNotifications() {
        log.info("=== CRON Sweeper running ===");

        // Scan for all pending notif keys
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) {
            log.info("No pending notifications found.");
            return;
        }

        for (String key : keys) {
            // Extract userId from key pattern "user:{id}:pending_notifs"
            String userId = key.split(":")[1];

            // Pop ALL pending messages
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
            if (messages == null || messages.isEmpty()) continue;

            int count = messages.size();

            // Build summarized message
            String firstMessage = messages.get(0); // e.g. "Bot X replied to your post"
            String botName = extractBotName(firstMessage);
            String summary = count == 1
                ? "Summarized Push Notification: " + botName + " interacted with your posts."
                : "Summarized Push Notification: " + botName + " and [" + (count - 1) + "] others interacted with your posts.";

            log.info("User {}: {}", userId, summary);

            // Clear the list
            redisTemplate.delete(key);
        }

        log.info("=== CRON Sweeper done ===");
    }

    private String extractBotName(String message) {
        // Message format: "Bot X replied to your post (postId=N)"
        try {
            return message.split(" replied")[0]; // returns "Bot X"
        } catch (Exception e) {
            return "Bot";
        }
    }
}
