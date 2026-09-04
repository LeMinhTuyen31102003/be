package be.spring.vanconhung.dto;

import be.spring.vanconhung.entity.User;

public record TeacherContactResponse(String fullName, String email, String phone) {

    public static TeacherContactResponse from(User teacher) {
        return new TeacherContactResponse(teacher.getFullName(), teacher.getEmail(), teacher.getPhone());
    }
}
