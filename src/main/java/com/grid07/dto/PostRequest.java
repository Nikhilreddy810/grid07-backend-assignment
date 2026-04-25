package com.grid07.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostRequest {

    @NotNull
    private Long authorId;

    // "USER" or "BOT"
    @NotBlank
    private String authorType;

    @NotBlank
    private String content;
}
