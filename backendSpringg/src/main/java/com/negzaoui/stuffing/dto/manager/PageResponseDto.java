package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}

