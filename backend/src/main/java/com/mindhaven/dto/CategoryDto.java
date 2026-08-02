package com.mindhaven.dto;

import java.util.List;

import com.mindhaven.entity.KnowledgeCategory;

public record CategoryDto(Long id, Long parentId, String name, String slug, Integer sortOrder, String status,
                          List<CategoryDto> children) {

    public static CategoryDto of(KnowledgeCategory c, List<CategoryDto> children) {
        return new CategoryDto(c.getId(), c.getParentId(), c.getName(), c.getSlug(), c.getSortOrder(), c.getStatus(), children);
    }
}
