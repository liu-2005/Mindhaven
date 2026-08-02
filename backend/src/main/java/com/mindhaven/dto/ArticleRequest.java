package com.mindhaven.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRequest(
        @NotBlank String title,
        String summary,
        @NotBlank String contentHtml,
        @NotNull Long categoryId,
        List<String> tags,
        String coverUrl) {
}
