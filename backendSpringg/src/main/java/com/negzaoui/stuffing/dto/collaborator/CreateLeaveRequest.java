package com.negzaoui.stuffing.dto.collaborator;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequest {

    @NotNull(message = "Le type de conge est obligatoire")
    private String type;

    @NotNull(message = "La date de debut est obligatoire")
    @FutureOrPresent(message = "La date de debut doit etre aujourd'hui ou dans le futur")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    private String reason;
}

