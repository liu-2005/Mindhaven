package com.mindhaven.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "mh_article_tag")
@IdClass(ArticleTag.ArticleTagId.class)
public class ArticleTag {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ArticleTag() {
    }

    public ArticleTag(Long articleId, Long tagId) {
        this.articleId = articleId;
        this.tagId = tagId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getTagId() {
        return tagId;
    }

    public static class ArticleTagId implements Serializable {
        private Long articleId;
        private Long tagId;

        public ArticleTagId() {
        }

        public ArticleTagId(Long articleId, Long tagId) {
            this.articleId = articleId;
            this.tagId = tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArticleTagId that = (ArticleTagId) o;
            return Objects.equals(articleId, that.articleId) && Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(articleId, tagId);
        }
    }
}
