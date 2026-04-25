package com.grid07.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Lua script for atomic check-and-increment.
     * Returns 1 if increment succeeded (count <= 100).
     * Returns 0 if cap exceeded (count > 100) — also decrements back.
     *
     * This guarantees the race condition test: exactly 100 bot replies, never 101.
     */
    @Bean
    public DefaultRedisScript<Long> botCountAtomicScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count > tonumber(ARGV[1]) then " +
            "  redis.call('DECR', KEYS[1]) " +
            "  return 0 " +
            "end " +
            "return 1"
        );
        script.setResultType(Long.class);
        return script;
    }
}
