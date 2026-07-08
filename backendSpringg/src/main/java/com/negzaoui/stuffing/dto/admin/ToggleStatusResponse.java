package com.negzaoui.stuffing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusResponse {

    private Long id;

    @JsonProperty("isActive")
    private boolean isActive;
}

