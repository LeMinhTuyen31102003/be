package be.spring.vanconhung.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateMyProfileRequest(
        @NotBlank String fullName,
        @Email String email,
        String phone,
        String address,
        String grade,
        String schoolName,
        String parentName,
        String parentPhone) {
}
