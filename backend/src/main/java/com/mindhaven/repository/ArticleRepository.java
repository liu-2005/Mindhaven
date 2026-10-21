package com.mindhaven.repository;

import java.util.Optional;

import com.mindhaven.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByIdAndDeletedAtIsNull(Long id);

    Optional<Article> findByTitleAndDeletedAtIsNull(String title);

    boolean existsByTitle(String title);

    @Query("select a from Article a where a.deletedAt is null " +
            "and (:categoryId is null or a.categoryId = :categoryId) " +
            "and (:status is null or a.status = :status) " +
            "and (:title is null or a.title like %:title%) " +
            "order by a.publishedAt desc, a.id desc")
    Page<Article> search(@Param("categoryId") Long categoryId,
                         @Param("status") String status,
                         @Param("title") String title,
                         Pageable pageable);

    @Query("select a from Article a where a.deletedAt is null and a.status = 'PUBLISHED' " +
            "and (:categoryId is null or a.categoryId = :categoryId) " +
            "and (:keyword is null or a.title like %:keyword% or a.summary like %:keyword%) " +
            "order by a.publishedAt desc, a.id desc")
    Page<Article> publicPage(@Param("categoryId") Long categoryId,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    long countByStatus(String status);
}
