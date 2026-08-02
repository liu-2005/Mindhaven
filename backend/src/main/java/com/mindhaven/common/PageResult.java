package com.mindhaven.common;

import java.util.List;

/**
 * 统一分页响应。数据在 items 中，配合 page/size/total/totalPages 使用。
 */
public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        int pages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(items, page, size, total, pages);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0, 0);
    }
}
