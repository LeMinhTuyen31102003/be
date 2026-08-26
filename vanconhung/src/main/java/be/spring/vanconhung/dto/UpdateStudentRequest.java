package be.spring.vanconhung.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStudentRequest(
        @NotBlank String fullName,
        String grade,
        String schoolName,
        String parentName,
        String parentPhone) {
}
