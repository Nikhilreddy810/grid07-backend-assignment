# Grid07 Backend Assignment — Core API & Guardrails

## Tech Stack
- Java 17, Spring Boot 3.2.5
- PostgreSQL 15 (via Docker)
- Redis 7 (via Docker)

---

## How to Run

### 1. Start PostgreSQL + Redis
```bash
docker-compose up -d
```

### 2. Run the Spring Boot App
```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`

---

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/posts` | Create a new post |
| POST | `/api/posts/{postId}/comments` | Add a comment to a post |
| POST | `/api/posts/{postId}/like` | Like a post (human only) |

### Sample Request — Create Post
```json
POST /api/posts
{
  "authorId": 1,
  "authorType": "USER",
  "content": "Hello this is my first post!"
}
```

### Sample Request — Bot Comment
```json
POST /api/posts/1/comments
{
  "authorId": 1,
  "authorType": "BOT",
  "content": "Bot reply to post.",
  "depthLevel": 0
}
```

### Sample Request — Like Post
```json
POST /api/posts/1/like
{
  "userId": 1
}
```

---

## Phase 2: Thread Safety — Atomic Locks

### The Problem
With 200 concurrent bot requests hitting a single post simultaneously, a naive approach fails:

```
count = INCR key       // Thread A gets 100
if count > 100 reject  // Thread B also gets 100 before A checks
                       // Both pass → 101 comments in DB
```

This is a race condition.

### The Solution — Lua Script in Redis

```lua
local count = redis.call('INCR', KEYS[1])
if count > tonumber(ARGV[1]) then
  redis.call('DECR', KEYS[1])
  return 0
end
return 1
```

Redis executes Lua scripts **atomically** — no other command can run between the INCR and the check. This guarantees exactly 100 bot replies, never 101, even under 200 concurrent requests.

### All 3 Guardrails

| Guardrail | Redis Key | Mechanism |
|-----------|-----------|-----------|
| Horizontal Cap (max 100 bot replies per post) | `post:{id}:bot_count` | Lua atomic INCR + rollback |
| Vertical Cap (max comment depth 20) | — | Checked in Java before Redis ops |
| Cooldown (bot cannot hit same human twice in 10 min) | `cooldown:bot_{id}:human_{id}` | Redis SET with 10-min TTL |

---

## Phase 3: Notification Engine

When a bot interacts with a user's post:
- If user has `notif:cooldown:user_{id}` key in Redis → push message to `user:{id}:pending_notifs` Redis List
- If no cooldown key → log "Push Notification Sent to User", set 15-min TTL cooldown key

`@Scheduled` CRON runs every 5 minutes:
- Scans all `user:*:pending_notifs` keys
- Pops all pending messages for each user
- Logs summarized message: "Bot X and [N] others interacted with your posts."
- Clears the Redis list

---

## Phase 4: Statelessness

All counters, cooldowns, and pending notifications are stored **only in Redis**.
No `HashMap`, no `static` variables, no Java memory state anywhere.

PostgreSQL is the source of truth for actual content.
Redis is the gatekeeper — the DB only commits if Redis guardrails pass.

---

## Redis Key Reference

| Key | Type | Purpose |
|-----|------|---------|
| `post:{id}:virality_score` | String | Virality score (Bot=+1, Like=+20, Comment=+50) |
| `post:{id}:bot_count` | String | Number of bot replies on a post |
| `cooldown:bot_{id}:human_{id}` | String (TTL 10min) | Bot-human interaction cooldown |
| `notif:cooldown:user_{id}` | String (TTL 15min) | Notification cooldown per user |
| `user:{id}:pending_notifs` | List | Pending notification messages |
