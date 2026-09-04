package be.spring.vanconhung.dto;

import java.util.List;

import be.spring.vanconhung.entity.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarkAttendanceRequest(
        @NotEmpty List<@Valid StudentAttendanceInput> records) {

    public record StudentAttendanceInput(
            @NotNull Long studentId,
            AttendanceStatus status,
            String note) {
    }
}
