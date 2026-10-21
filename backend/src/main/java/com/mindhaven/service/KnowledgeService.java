package com.mindhaven.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.ArticleRequest;
import com.mindhaven.dto.CategoryDto;
import com.mindhaven.entity.Article;
import com.mindhaven.entity.ArticleTag;
import com.mindhaven.entity.ArticleViewEvent;
import com.mindhaven.entity.FileObject;
import com.mindhaven.entity.KnowledgeCategory;
import com.mindhaven.entity.Tag;
import com.mindhaven.repository.ArticleRepository;
import com.mindhaven.repository.ArticleTagRepository;
import com.mindhaven.repository.ArticleViewEventRepository;
import com.mindhaven.repository.FileObjectRepository;
import com.mindhaven.repository.KnowledgeCategoryRepository;
import com.mindhaven.repository.TagRepository;
import com.mindhaven.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final ArticleRepository articleRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final FileObjectRepository fileObjectRepository;
    private final UserRepository userRepository;
    private final ArticleViewEventRepository viewEventRepository;
    private final AuditService auditService;

    public KnowledgeService(ArticleRepository articleRepository,
                            KnowledgeCategoryRepository categoryRepository,
                            TagRepository tagRepository,
                            ArticleTagRepository articleTagRepository,
                            FileObjectRepository fileObjectRepository,
                            UserRepository userRepository,
                            ArticleViewEventRepository viewEventRepository,
                            AuditService auditService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.articleTagRepository = articleTagRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.userRepository = userRepository;
        this.viewEventRepository = viewEventRepository;
        this.auditService = auditService;
    }

    public List<CategoryDto> categoryTree() {
        List<KnowledgeCategory> all = categoryRepository.findByStatusOrderBySortOrderAscIdAsc("ACTIVE");
        Map<Long, List<CategoryDto>> childrenByParent = new LinkedHashMap<>();
        for (KnowledgeCategory c : all) {
            Long pid = c.getParentId();
            if (pid != null) {
                childrenByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(CategoryDto.of(c, List.of()));
            }
        }
        List<CategoryDto> roots = new ArrayList<>();
        for (KnowledgeCategory c : all) {
            if (c.getParentId() == null) {
                roots.add(CategoryDto.of(c, childrenByParent.getOrDefault(c.getId(), List.of())));
            }
        }
        return roots;
    }

    public Page<Article> publicPage(Long categoryId, String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return articleRepository.publicPage(categoryId, keyword, pageable);
    }

    @Transactional
    public Article publicDetail(Long articleId) {
        Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        Article saved = articleRepository.save(article);

        ArticleViewEvent event = new ArticleViewEvent();
        event.setArticleId(article.getId());
        event.setViewDate(LocalDate.now());
        viewEventRepository.save(event);
        return saved;
    }

    public Page<Article> adminPage(Long categoryId, String status, String title, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return articleRepository.search(categoryId, status, title, pageable);
    }

    public Article adminGet(Long articleId) {
        return articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Article create(Long operatorId, ArticleRequest request) {
        Article article = new Article();
        apply(article, request, operatorId);
        article.setStatus(STATUS_DRAFT);
        article.setVersion(0);
        article.setViewCount(0L);
        article = articleRepository.save(article);
        bindTags(article.getId(), request.tags());
        auditService.record(operatorId, "article:create", "ARTICLE", String.valueOf(article.getId()), "SUCCESS", null, null);
        return article;
    }

    @Transactional
    public Article update(Long operatorId, Long articleId, ArticleRequest request) {
        Article article = adminGet(articleId);
        apply(article, request, operatorId);
        article.setVersion((article.getVersion() == null ? 0 : article.getVersion()) + 1);
        article = articleRepository.save(article);
        articleTagRepository.deleteAll(articleTagRepository.findByArticleId(articleId));
        bindTags(articleId, request.tags());
        auditService.record(operatorId, "article:update", "ARTICLE", String.valueOf(articleId), "SUCCESS", null, null);
        return article;
    }

    @Transactional
    public Article updateStatus(Long operatorId, Long articleId, String status) {
        Article article = adminGet(articleId);
        if (!List.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE, STATUS_ARCHIVED).contains(status)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "非法的文章状态");
        }
        String from = article.getStatus();
        if (from.equals(status)) {
            return article;
        }
        boolean allowed = (from.equals(STATUS_DRAFT) || from.equals(STATUS_OFFLINE)) && status.equals(STATUS_PUBLISHED)
                || from.equals(STATUS_PUBLISHED) && status.equals(STATUS_OFFLINE)
                || from.equals(STATUS_DRAFT) && status.equals(STATUS_ARCHIVED)
                || from.equals(STATUS_OFFLINE) && status.equals(STATUS_ARCHIVED)
                || status.equals(STATUS_PUBLISHED) && from.equals(STATUS_OFFLINE);
        if (!allowed) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "不允许的状态流转: " + from + " -> " + status);
        }
        article.setStatus(status);
        if (status.equals(STATUS_PUBLISHED)) {
            article.setPublishedAt(article.getPublishedAt() == null ? java.time.LocalDateTime.now() : article.getPublishedAt());
        }
        article = articleRepository.save(article);
        auditService.record(operatorId, "article:status", "ARTICLE", String.valueOf(articleId), "SUCCESS",
                from + " -> " + status, null);
        return article;
    }

    @Transactional
    public void delete(Long operatorId, Long articleId) {
        Article article = adminGet(articleId);
        article.setDeletedAt(java.time.LocalDateTime.now());
        article.setStatus(STATUS_ARCHIVED);
        articleRepository.save(article);
        auditService.record(operatorId, "article:delete", "ARTICLE", String.valueOf(articleId), "SUCCESS", null, null);
    }

    private void apply(Article article, ArticleRequest request, Long operatorId) {
        article.setTitle(request.title().trim());
        article.setSummary(request.summary());
        article.setContentHtml(sanitize(request.contentHtml()));
        article.setCategoryId(request.categoryId());
        article.setAuthorUserId(operatorId);
        article.setCoverFileId(resolveCoverFileId(request.coverUrl()));
    }

    private Long resolveCoverFileId(String coverUrl) {
        if (!StringUtils.hasText(coverUrl)) {
            return null;
        }
        String trimmed = coverUrl.trim();
        int lastSlash = trimmed.lastIndexOf('/');
        String idStr = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        try {
            Long id = Long.parseLong(idStr);
            FileObject file = fileObjectRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
            return file == null ? null : file.getId();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void bindTags(Long articleId, List<String> tags) {
        if (tags == null) {
            return;
        }
        for (String raw : tags) {
            String name = raw.trim();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Tag tag = tagRepository.findByName(name).orElseGet(() -> {
                Tag t = new Tag();
                t.setName(name);
                t.setSlug(slugify(name));
                return tagRepository.save(t);
            });
            articleTagRepository.save(new ArticleTag(articleId, tag.getId()));
        }
    }

    private String sanitize(String html) {
        if (html == null) {
            return "";
        }
        Safelist safelist = Safelist.relaxed()
                .addTags("h2", "h3", "h4", "p", "span", "div", "blockquote")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addAttributes("a", "href", "title", "target", "rel")
                .addAttributes("span", "style");
        Document doc = Jsoup.parseBodyFragment(html);
        return Jsoup.clean(doc.body().html(), safelist);
    }

    private String slugify(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-+|-+$", "").toLowerCase();
        return base.isEmpty() ? "tag-" + Integer.toUnsignedString(name.hashCode()) : base;
    }

    public Map<Long, String> categoryNameByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
    }

    public Map<Long, List<String>> tagNamesByArticleIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<ArticleTag> rels = articleTagRepository.findByArticleIdIn(new ArrayList<>(ids));
        List<Long> tagIds = rels.stream().map(ArticleTag::getTagId).distinct().toList();
        Map<Long, String> tagNames = tagIds.isEmpty() ? Map.of()
                : tagRepository.findAllById(tagIds).stream().collect(Collectors.toMap(Tag::getId, Tag::getName));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (ArticleTag rel : rels) {
            String name = tagNames.get(rel.getTagId());
            if (name != null) {
                result.computeIfAbsent(rel.getArticleId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    public String authorName(Long authorUserId) {
        if (authorUserId == null) {
            return "心晴";
        }
        return userRepository.findById(authorUserId).map(u -> {
            String name = u.getNickname();
            return StringUtils.hasText(name) ? name : u.getUsername();
        }).orElse("心晴");
    }

    public String fileUrl(Long coverFileId) {
        return coverFileId == null ? null : "/api/v1/files/" + coverFileId;
    }
}
