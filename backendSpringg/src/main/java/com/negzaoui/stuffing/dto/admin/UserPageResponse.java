package com.negzaoui.stuffing.dto.admin;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPageResponse {

    private List<UserAdminDto> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}

