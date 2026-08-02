package com.mindhaven.controller;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.ArticleDto;
import com.mindhaven.dto.CategoryDto;
import com.mindhaven.entity.Article;
import com.mindhaven.service.KnowledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/categories")
    public ApiResponse<java.util.List<CategoryDto>> categories() {
        return ApiResponse.success(knowledgeService.categoryTree());
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<ArticleDto>> articles(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) String keyword) {
        var result = knowledgeService.publicPage(categoryId, keyword, page, size);
        var items = toDtos(result.getContent());
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @GetMapping("/articles/{articleId}")
    public ApiResponse<ArticleDto> detail(@PathVariable Long articleId) {
        Article article = knowledgeService.publicDetail(articleId);
        return ApiResponse.success(toDto(article));
    }

    private java.util.List<ArticleDto> toDtos(java.util.List<Article> articles) {
        if (articles.isEmpty()) {
            return java.util.List.of();
        }
        var categoryNames = knowledgeService.categoryNameByIds(articles.stream().map(Article::getCategoryId).toList());
        var tagNames = knowledgeService.tagNamesByArticleIds(articles.stream().map(Article::getId).toList());
        return articles.stream().map(a -> ArticleDto.of(a,
                categoryNames.getOrDefault(a.getCategoryId(), ""),
                tagNames.getOrDefault(a.getId(), java.util.List.of()),
                knowledgeService.authorName(a.getAuthorUserId()),
                knowledgeService.fileUrl(a.getCoverFileId()))).toList();
    }

    private ArticleDto toDto(Article article) {
        var categoryNames = knowledgeService.categoryNameByIds(java.util.List.of(article.getCategoryId()));
        var tagNames = knowledgeService.tagNamesByArticleIds(java.util.List.of(article.getId()));
        return ArticleDto.of(article,
                categoryNames.getOrDefault(article.getCategoryId(), ""),
                tagNames.getOrDefault(article.getId(), java.util.List.of()),
                knowledgeService.authorName(article.getAuthorUserId()),
                knowledgeService.fileUrl(article.getCoverFileId()));
    }
}
