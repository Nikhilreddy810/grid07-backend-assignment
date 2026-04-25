# Grid07 Backend Assignment

## Tech Stack
Java 17+, Spring Boot 3.2.5, PostgreSQL, Redis

---

## How to Run

### 1. Start Postgres and Redis
```bash
docker-compose up -d
```

### 2. Run the Spring Boot app
```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/posts` | Create a post |
| POST | `/api/posts/{postId}/comments` | Add a comment |
| POST | `/api/posts/{postId}/like` | Like a post |

---

## Phase 2: Thread Safety — Atomic Locks

### How I guaranteed thread safety for the Horizontal Cap (100 bot replies):

The critical section is incrementing the bot reply counter in Redis. With 200 concurrent requests, a naive `GET → check → SET` sequence would have race conditions where multiple threads read the same count, all pass the check, and the counter overshoots 100.

**Solution: Lua Script executed atomically on Redis**

```lua
local count = redis.call('INCR', KEYS[1])
if count > tonumber(ARGV[1]) then
  redis.call('DECR', KEYS[1])
  return 0
end
return 1
```

Redis executes Lua scripts atomically — no other command can run between the INCR and the check. This means:
- Exactly 100 bot replies are allowed, never 101
- The counter is rolled back immediately if the cap is exceeded
- No Java-level locking or synchronization needed
- Spring Boot remains completely stateless

### Other Guardrails:
- **Vertical Cap (depth > 20):** Checked before Redis ops. Pure value check, no concurrency issue.
- **Cooldown (10 min per bot-human pair):** Uses Redis `SET key value EX ttl NX` semantics via `hasKey` + `set with TTL`. TTL auto-expires the key.

---

## Phase 3: Notification Engine

- When a bot interacts with a user's post:
  - If user has a 15-min cooldown key → push message to `user:{id}:pending_notifs` (Redis List)
  - If no cooldown → log "Push Notification Sent" + set 15-min cooldown key
- `@Scheduled` CRON runs every 5 minutes, scans all `user:*:pending_notifs` keys, pops all messages, logs a summarized notification, clears the list.

---

## Phase 4: Statelessness

- No `HashMap`, no `static` variables
- All counters, cooldowns, pending notifications stored in Redis only
- PostgreSQL is source of truth for content
- Redis is gatekeeper — DB write only happens if Redis guardrails pass
