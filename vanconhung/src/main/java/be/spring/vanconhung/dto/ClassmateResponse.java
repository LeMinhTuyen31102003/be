package be.spring.vanconhung.dto;

import be.spring.vanconhung.entity.User;

public record ClassmateResponse(Long id, String fullName, String grade) {

    public static ClassmateResponse from(User user) {
        return new ClassmateResponse(user.getId(), user.getFullName(), user.getGrade());
    }
}
