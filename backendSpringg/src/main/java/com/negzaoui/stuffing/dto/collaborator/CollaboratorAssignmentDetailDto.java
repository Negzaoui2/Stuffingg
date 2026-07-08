package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CollaboratorAssignmentDetailDto {
    private Long id;
    private String projectName;
    private String clientName;
    private String roleName;
    private String startDate;
    private String endDate;
    private String status;
    private List<String> technologies;
}

