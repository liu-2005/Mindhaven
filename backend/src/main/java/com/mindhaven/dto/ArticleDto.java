package com.mindhaven.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.mindhaven.entity.Article;

public record ArticleDto(Long id, String title, String summary, String contentHtml, Long categoryId,
                         String categoryName, List<String> tags, String authorName, String coverUrl,
                         String status, Long viewCount, LocalDateTime publishedAt, LocalDateTime createdAt,
                         LocalDateTime updatedAt) {

    public static ArticleDto of(Article a, String categoryName, List<String> tags, String authorName, String coverUrl) {
        return new ArticleDto(a.getId(), a.getTitle(), a.getSummary(), a.getContentHtml(), a.getCategoryId(),
                categoryName, tags, authorName, coverUrl, a.getStatus(), a.getViewCount(), a.getPublishedAt(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
