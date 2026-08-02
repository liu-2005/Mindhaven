package com.mindhaven.repository;

import java.util.List;

import com.mindhaven.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, ArticleTag.ArticleTagId> {

    List<ArticleTag> findByArticleId(Long articleId);

    List<ArticleTag> findByArticleIdIn(List<Long> articleIds);
}
