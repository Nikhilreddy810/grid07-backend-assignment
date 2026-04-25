package com.grid07.controller;

import com.grid07.dto.CommentRequest;
import com.grid07.dto.LikeRequest;
import com.grid07.dto.PostRequest;
import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
    // POST /api/posts
    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(req));
    }

    // POST /api/posts/{postId}/comments
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.addComment(postId, req));
    }

    // POST /api/posts/{postId}/like
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest req) {
        return ResponseEntity.ok(postService.likePost(postId, req));
    }
}
