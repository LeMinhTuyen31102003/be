package be.spring.vanconhung.dto;

import java.time.Instant;
import java.util.List;

import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.User;

public record StudentResponse(
        Long id,
        String username,
        String fullName,
        String grade,
        String schoolName,
        String parentName,
        String parentPhone,
        boolean active,
        Instant createdAt,
        List<ClassRef> classes) {

    public record ClassRef(Long id, String name) {
    }

    public static StudentResponse from(User user) {
        return from(user, List.of());
    }

    public static StudentResponse from(User user, List<ClassRoom> classRooms) {
        List<ClassRef> classes = classRooms.stream()
                .map(c -> new ClassRef(c.getId(), c.getName()))
                .toList();

        return new StudentResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getGrade(),
                user.getSchoolName(),
                user.getParentName(),
                user.getParentPhone(),
                user.isEnabled(),
                user.getCreatedAt(),
                classes);
    }
}
