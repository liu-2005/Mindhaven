package com.mindhaven.repository;

import java.util.List;

import com.mindhaven.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {

    List<KnowledgeCategory> findByStatusOrderBySortOrderAscIdAsc(String status);
}
