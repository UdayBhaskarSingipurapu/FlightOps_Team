package com.project.flightOps.util;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private final List<T> data;
    private final long totalCount;
    private final int totalPages;
    private final int currentPage;

    public static <T> PageResponse<T> of(List<T> data, long totalCount, int totalPages, int currentPage) {
        return PageResponse.<T>builder()
                .data(data)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .build();
    }
}
