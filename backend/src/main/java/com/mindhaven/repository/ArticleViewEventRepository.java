package com.mindhaven.repository;

import com.mindhaven.entity.ArticleViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewEventRepository extends JpaRepository<ArticleViewEvent, Long> {
}
