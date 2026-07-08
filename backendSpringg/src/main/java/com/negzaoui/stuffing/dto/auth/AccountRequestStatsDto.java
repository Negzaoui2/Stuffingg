package com.negzaoui.stuffing.dto.auth;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequestStatsDto {
    private long total;
    private long pending;
    private long approved;
    private long rejected;
}
