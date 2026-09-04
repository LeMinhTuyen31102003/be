package be.spring.vanconhung.dto;

import be.spring.vanconhung.entity.TuitionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTuitionRequest(
        @NotNull @Min(0) Long amount,
        @NotNull TuitionStatus status,
        String note) {
}
