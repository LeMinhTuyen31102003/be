package be.spring.vanconhung.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record UpdateClassRequest(
        @NotBlank String name,
        String grade,
        List<ScheduleSlotInput> schedules,
        String note,
        Long feePerSession) {
}
