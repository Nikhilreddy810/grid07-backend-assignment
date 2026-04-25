package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViralityService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int BOT_REPLY_POINTS    = 1;
    private static final int HUMAN_LIKE_POINTS   = 20;
    private static final int HUMAN_COMMENT_POINTS = 50;

    public void onBotReply(Long postId) {
        increment(postId, BOT_REPLY_POINTS);
    }

    public void onHumanLike(Long postId) {
        increment(postId, HUMAN_LIKE_POINTS);
    }

    public void onHumanComment(Long postId) {
        increment(postId, HUMAN_COMMENT_POINTS);
    }

    private void increment(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
        log.info("Virality score updated for post {} | +{} pts | new score: {}",
                postId, points, redisTemplate.opsForValue().get(key));
    }

    public String getScore(Long postId) {
        return redisTemplate.opsForValue().get("post:" + postId + ":virality_score");
    }
}
