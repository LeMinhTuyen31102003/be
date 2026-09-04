package be.spring.vanconhung.dto;

import java.time.Instant;

import be.spring.vanconhung.entity.User;

public record MyProfileResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String address,
        String role,
        boolean active,
        String grade,
        String schoolName,
        String parentName,
        String parentPhone,
        Instant createdAt) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().name(),
                user.isEnabled(),
                user.getGrade(),
                user.getSchoolName(),
                user.getParentName(),
                user.getParentPhone(),
                user.getCreatedAt());
    }
}
