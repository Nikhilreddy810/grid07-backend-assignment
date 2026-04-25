package com.grid07.service;

import com.grid07.exception.BotCapExceededException;
import com.grid07.exception.BotCooldownException;
import com.grid07.exception.VerticalCapExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardrailService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> botCountAtomicScript;

    private static final int HORIZONTAL_CAP   = 100;
    private static final int VERTICAL_CAP     = 20;
    private static final long COOLDOWN_MINUTES = 10;

    /**
     * Checks all 3 guardrails before allowing a bot to comment.
     * Throws specific exception if any guardrail is violated.
     *
     * @param postId    target post ID
     * @param botId     bot attempting to comment
     * @param humanId   post owner (for cooldown check)
     * @param depthLevel depth of the comment being added
     */
    public void checkBotGuardrails(Long postId, Long botId, Long humanId, int depthLevel) {
        checkVerticalCap(depthLevel);
        checkCooldown(botId, humanId);
        checkHorizontalCap(postId); // This also atomically increments the counter
    }

    // ---------- Vertical Cap ----------

    private void checkVerticalCap(int depthLevel) {
        if (depthLevel > VERTICAL_CAP) {
            throw new VerticalCapExceededException(
                "Comment depth " + depthLevel + " exceeds max allowed depth of " + VERTICAL_CAP
            );
        }
    }

    // ---------- Cooldown Cap ----------

    private void checkCooldown(Long botId, Long humanId) {
        String cooldownKey = "cooldown:bot_" + botId + ":human_" + humanId;
        Boolean exists = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BotCooldownException(
                "Bot " + botId + " is in cooldown for human " + humanId + ". Try after 10 minutes."
            );
        }
        // Set cooldown key with 10-minute TTL
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(COOLDOWN_MINUTES));
        log.info("Cooldown set: bot {} -> human {} for {} minutes", botId, humanId, COOLDOWN_MINUTES);
    }

    // ---------- Horizontal Cap (Lua atomic script) ----------

    private void checkHorizontalCap(Long postId) {
        String botCountKey = "post:" + postId + ":bot_count";
        Long result = redisTemplate.execute(
            botCountAtomicScript,
            List.of(botCountKey),
            String.valueOf(HORIZONTAL_CAP)
        );
        if (result == null || result == 0L) {
            throw new BotCapExceededException(
                "Post " + postId + " has reached the maximum of " + HORIZONTAL_CAP + " bot replies."
            );
        }
        log.info("Bot count incremented for post {}. Result: {}", postId, result);
    }

    /**
     * Called when a bot interaction is rejected AFTER the horizontal cap was already checked.
     * Rolls back the bot_count increment so the counter stays accurate.
     */
    public void rollbackBotCount(Long postId) {
        String botCountKey = "post:" + postId + ":bot_count";
        redisTemplate.opsForValue().decrement(botCountKey);
        log.warn("Rolled back bot_count for post {}", postId);
    }
}
