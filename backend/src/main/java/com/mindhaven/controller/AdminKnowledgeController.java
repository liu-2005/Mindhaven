package com.mindhaven.controller;

import java.util.List;
import java.util.Map;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.ArticleDto;
import com.mindhaven.dto.ArticleRequest;
import com.mindhaven.entity.Article;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/knowledge")
public class AdminKnowledgeController {

    private final KnowledgeService knowledgeService;

    public AdminKnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/articles")
    @RequirePermission("article:manage")
    public ApiResponse<PageResult<ArticleDto>> page(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String title) {
        var result = knowledgeService.adminPage(categoryId, status, title, page, size);
        var items = toDtos(result.getContent());
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @GetMapping("/articles/{articleId}")
    @RequirePermission("article:manage")
    public ApiResponse<ArticleDto> get(@PathVariable Long articleId) {
        return ApiResponse.success(toDto(knowledgeService.adminGet(articleId)));
    }

    @PostMapping("/articles")
    @RequirePermission("article:manage")
    public ApiResponse<ArticleDto> create(@Valid @RequestBody ArticleRequest request) {
        return ApiResponse.success(toDto(knowledgeService.create(AuthContextHolder.userId(), request)));
    }

    @PutMapping("/articles/{articleId}")
    @RequirePermission("article:manage")
    public ApiResponse<ArticleDto> update(@PathVariable Long articleId, @Valid @RequestBody ArticleRequest request) {
        return ApiResponse.success(toDto(knowledgeService.update(AuthContextHolder.userId(), articleId, request)));
    }

    @PutMapping("/articles/{articleId}/status")
    @RequirePermission("article:publish")
    public ApiResponse<ArticleDto> updateStatus(@PathVariable Long articleId, @RequestBody Map<String, String> body) {
        return ApiResponse.success(toDto(knowledgeService.updateStatus(AuthContextHolder.userId(), articleId, body.get("status"))));
    }

    @DeleteMapping("/articles/{articleId}")
    @RequirePermission("article:manage")
    public ApiResponse<Void> delete(@PathVariable Long articleId) {
        knowledgeService.delete(AuthContextHolder.userId(), articleId);
        return ApiResponse.success(null);
    }

    private List<ArticleDto> toDtos(List<Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }
        var categoryNames = knowledgeService.categoryNameByIds(articles.stream().map(Article::getCategoryId).toList());
        var tagNames = knowledgeService.tagNamesByArticleIds(articles.stream().map(Article::getId).toList());
        return articles.stream().map(a -> ArticleDto.of(a,
                categoryNames.getOrDefault(a.getCategoryId(), ""),
                tagNames.getOrDefault(a.getId(), List.of()),
                knowledgeService.authorName(a.getAuthorUserId()),
                knowledgeService.fileUrl(a.getCoverFileId()))).toList();
    }

    private ArticleDto toDto(Article article) {
        var categoryNames = knowledgeService.categoryNameByIds(List.of(article.getCategoryId()));
        var tagNames = knowledgeService.tagNamesByArticleIds(List.of(article.getId()));
        return ArticleDto.of(article,
                categoryNames.getOrDefault(article.getCategoryId(), ""),
                tagNames.getOrDefault(article.getId(), List.of()),
                knowledgeService.authorName(article.getAuthorUserId()),
                knowledgeService.fileUrl(article.getCoverFileId()));
    }
}
