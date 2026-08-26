package be.spring.vanconhung.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự") String password,
        @NotBlank String fullName,
        String grade,
        String schoolName,
        String parentName,
        String parentPhone,
        Boolean active) {
}
