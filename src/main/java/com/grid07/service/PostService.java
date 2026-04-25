package com.grid07.service;

import com.grid07.dto.CommentRequest;
import com.grid07.dto.LikeRequest;
import com.grid07.dto.PostRequest;
import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.repository.BotRepository;
import com.grid07.repository.CommentRepository;
import com.grid07.repository.PostRepository;
import com.grid07.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository       postRepository;
    private final CommentRepository    commentRepository;
    private final UserRepository       userRepository;
    private final BotRepository        botRepository;
    private final ViralityService      viralityService;
    private final GuardrailService     guardrailService;
    private final NotificationService  notificationService;

    // ─── Create Post ────────────────────────────────────────────────────────

    @Transactional
    public Post createPost(PostRequest req) {
        // Validate author exists
        validateAuthor(req.getAuthorId(), req.getAuthorType());

        Post post = new Post();
        post.setAuthorId(req.getAuthorId());
        post.setAuthorType(req.getAuthorType().toUpperCase());
        post.setContent(req.getContent());

        return postRepository.save(post);
    }

    // ─── Add Comment ─────────────────────────────────────────────────────────

    @Transactional
    public Comment addComment(Long postId, CommentRequest req) {
        // Post must exist
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        String authorType = req.getAuthorType().toUpperCase();
        validateAuthor(req.getAuthorId(), authorType);

        if ("BOT".equals(authorType)) {
            // Get post owner (must be a USER for cooldown check)
            Long humanId = getHumanOwner(post);

            // All 3 Redis guardrails checked atomically
            guardrailService.checkBotGuardrails(postId, req.getAuthorId(), humanId, req.getDepthLevel());

            // Save comment to DB (guardrails passed)
            Comment comment = saveComment(postId, req, authorType);

            // Update virality score
            viralityService.onBotReply(postId);

            // Trigger notification
            String botName = botRepository.findById(req.getAuthorId())
                .map(b -> b.getName())
                .orElse("Bot " + req.getAuthorId());
            notificationService.handleBotInteraction(humanId, botName, postId);

            return comment;

        } else {
            // Human comment
            Comment comment = saveComment(postId, req, authorType);
            viralityService.onHumanComment(postId);
            return comment;
        }
    }

    // ─── Like Post ────────────────────────────────────────────────────────────

    @Transactional
    public String likePost(Long postId, LikeRequest req) {
        postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        userRepository.findById(req.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        viralityService.onHumanLike(postId);

        return "Post " + postId + " liked by user " + req.getUserId()
             + " | Virality: " + viralityService.getScore(postId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Comment saveComment(Long postId, CommentRequest req, String authorType) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(req.getAuthorId());
        comment.setAuthorType(authorType);
        comment.setContent(req.getContent());
        comment.setDepthLevel(req.getDepthLevel());
        return commentRepository.save(comment);
    }

    private void validateAuthor(Long authorId, String authorType) {
        if ("BOT".equalsIgnoreCase(authorType)) {
            botRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + authorId));
        } else {
            userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found: " + authorId));
        }
    }

    /**
     * Returns the human owner of a post.
     * If post was made by a bot, defaults to authorId=1 for cooldown purposes.
     * In production you'd store post owner separately.
     */
    private Long getHumanOwner(Post post) {
        if ("USER".equals(post.getAuthorType())) {
            return post.getAuthorId();
        }
        // Bot-authored post — no human owner, use postId as a proxy key
        return post.getId();
    }
}
