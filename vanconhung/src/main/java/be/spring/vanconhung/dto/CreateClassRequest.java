package be.spring.vanconhung.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
        @NotBlank String name,
        String grade,
        List<ScheduleSlotInput> schedules,
        String note) {
}
