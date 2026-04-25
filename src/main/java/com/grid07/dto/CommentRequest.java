package com.grid07.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull
    private Long authorId;

    // "USER" or "BOT"
    @NotBlank
    private String authorType;

    @NotBlank
    private String content;

    // depth level of THIS comment (0 for direct reply to post, 1+ for nested)
    @NotNull
    private Integer depthLevel;
}
